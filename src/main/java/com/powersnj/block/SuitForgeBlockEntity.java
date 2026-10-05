package com.powersnj.block;

import com.powersnj.PowersNJ;
import com.powersnj.core.fabrication.FabricationCheck;
import com.powersnj.core.fabrication.FabricationMatcher;
import com.powersnj.item.BlueprintItem;
import com.powersnj.menu.SuitForgeMenu;
import com.powersnj.recipe.SuitFabricationRecipe;
import com.powersnj.recipe.SuitForgeLayout;
import com.powersnj.registry.ModBlockEntities;
import com.powersnj.registry.ModItems;
import com.powersnj.registry.ModRecipes;
import com.powersnj.registry.ModSounds;
import com.powersnj.suit.SuitArmorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Suit Forge logic. The server is the only authority: the client screen merely asks to fabricate
 * (menu button), the block entity re-validates blueprint, power core, materials and free outputs
 * when starting, on every tick of the process and once more before consuming anything.
 */
public class SuitForgeBlockEntity extends BlockEntity implements MenuProvider {

    public enum Status {
        NO_BLUEPRINT, UNKNOWN_BLUEPRINT, MISSING_CORE, MISSING_MATERIALS, OUTPUT_BLOCKED, READY, FABRICATING;

        public String translationKey() {
            return "gui.powersnj.suit_forge.status." + this.name().toLowerCase(Locale.ROOT);
        }

        public static Status byId(int id) {
            Status[] values = values();
            return id >= 0 && id < values.length ? values[id] : NO_BLUEPRINT;
        }
    }

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX_PROGRESS = 1;
    public static final int DATA_STATUS = 2;
    public static final int DATA_COUNT = 3;

    private final ItemStackHandler inventory = new ItemStackHandler(SuitForgeLayout.SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            SuitForgeBlockEntity.this.statusDirty = true;
            SuitForgeBlockEntity.this.setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return SuitForgeBlockEntity.isValidForSlot(slot, stack);
        }
    };
    private final LazyOptional<IItemHandler> automation = LazyOptional.of(() -> new AutomationHandler(this.inventory));

    private int progress;
    private int maxProgress;
    private boolean fabricating;
    @Nullable
    private ResourceLocation activeRecipe;
    private Status status = Status.NO_BLUEPRINT;
    private boolean statusDirty = true;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> SuitForgeBlockEntity.this.progress;
                case DATA_MAX_PROGRESS -> SuitForgeBlockEntity.this.maxProgress;
                case DATA_STATUS -> SuitForgeBlockEntity.this.status.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_PROGRESS -> SuitForgeBlockEntity.this.progress = value;
                case DATA_MAX_PROGRESS -> SuitForgeBlockEntity.this.maxProgress = value;
                case DATA_STATUS -> SuitForgeBlockEntity.this.status = Status.byId(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public SuitForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUIT_FORGE.get(), pos, state);
    }

    public static boolean isValidForSlot(int slot, ItemStack stack) {
        if (slot == SuitForgeLayout.BLUEPRINT) {
            return stack.getItem() instanceof BlueprintItem;
        }
        if (slot == SuitForgeLayout.POWER_CORE) {
            return stack.is(ModItems.POWER_CORE.get());
        }
        if (SuitForgeLayout.isMaterialSlot(slot)) {
            return !(stack.getItem() instanceof BlueprintItem) && !(stack.getItem() instanceof SuitArmorItem);
        }
        return false;
    }

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    public ContainerData getData() {
        return this.data;
    }

    public Status getStatus() {
        return this.status;
    }

    public boolean isFabricating() {
        return this.fabricating;
    }

    public int getProgress() {
        return this.progress;
    }

    /**
     * Recipe selected by the blueprint currently inserted (works on both sides: recipes are synced).
     */
    public static Optional<SuitFabricationRecipe> findRecipe(Level level, ItemStack blueprint) {
        if (level == null || blueprint.isEmpty()) {
            return Optional.empty();
        }
        List<SuitFabricationRecipe> recipes = level.getRecipeManager().getAllRecipesFor(ModRecipes.SUIT_FABRICATION.get());
        for (SuitFabricationRecipe recipe : recipes) {
            if (blueprint.is(recipe.blueprint())) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    public Optional<SuitFabricationRecipe> currentRecipe() {
        return findRecipe(this.level, this.inventory.getStackInSlot(SuitForgeLayout.BLUEPRINT));
    }

    private boolean outputsEmpty() {
        for (int i = 0; i < SuitForgeLayout.OUTPUT_COUNT; i++) {
            if (!this.inventory.getStackInSlot(SuitForgeLayout.OUTPUT_START + i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private Status computeStatus() {
        if (this.fabricating) {
            return Status.FABRICATING;
        }
        ItemStack blueprint = this.inventory.getStackInSlot(SuitForgeLayout.BLUEPRINT);
        if (blueprint.isEmpty()) {
            return Status.NO_BLUEPRINT;
        }
        Optional<SuitFabricationRecipe> recipe = this.currentRecipe();
        if (recipe.isEmpty()) {
            return Status.UNKNOWN_BLUEPRINT;
        }
        FabricationCheck check = recipe.get().check(new RecipeWrapper(this.inventory));
        if (!check.powerCorePresent()) {
            return Status.MISSING_CORE;
        }
        if (!check.materialsSatisfied()) {
            return Status.MISSING_MATERIALS;
        }
        if (!this.outputsEmpty()) {
            return Status.OUTPUT_BLOCKED;
        }
        return Status.READY;
    }

    /**
     * Server-side entry point of the "Fabricate" button.
     *
     * @return feedback for the player
     */
    public Component tryStartFabrication(@Nullable ServerPlayer player) {
        if (this.level == null || this.level.isClientSide) {
            return Component.empty();
        }
        this.status = this.computeStatus();
        if (this.status != Status.READY) {
            return Component.translatable(this.status.translationKey());
        }
        SuitFabricationRecipe recipe = this.currentRecipe().orElseThrow();
        this.fabricating = true;
        this.progress = 0;
        this.maxProgress = Math.max(1, recipe.processingTime());
        this.activeRecipe = recipe.getId();
        this.status = Status.FABRICATING;
        this.setWorking(true);
        this.setChanged();
        PowersNJ.LOGGER.debug("{} started fabricating {} at {}", player == null ? "automation" : player.getGameProfile().getName(), recipe.getId(), this.worldPosition);
        return Component.translatable("message.powersnj.suit_forge.started", Component.translatable("suit.powersnj." + recipe.suit().name()));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SuitForgeBlockEntity forge) {
        if (forge.fabricating) {
            forge.tickFabrication((ServerLevel) level, pos);
        } else if (forge.statusDirty || level.getGameTime() % 20 == 0) {
            forge.status = forge.computeStatus();
            forge.statusDirty = false;
        }
    }

    private void tickFabrication(ServerLevel level, BlockPos pos) {
        Optional<SuitFabricationRecipe> recipe = this.currentRecipe();
        boolean valid = recipe.isPresent() && recipe.get().getId().equals(this.activeRecipe)
                && recipe.get().check(new RecipeWrapper(this.inventory)).canFabricate() && this.outputsEmpty();
        if (!valid) {
            this.abort();
            return;
        }
        this.progress++;
        if (this.progress % 10 == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.05);
        }
        if (this.progress >= this.maxProgress) {
            this.complete(level, pos, recipe.get());
        }
    }

    private void complete(ServerLevel level, BlockPos pos, SuitFabricationRecipe recipe) {
        var plan = FabricationMatcher.planConsumption(recipe.spec(), SuitForgeLayout.materialContents(this.inventory));
        if (plan.isEmpty() || this.inventory.getStackInSlot(SuitForgeLayout.POWER_CORE).isEmpty() || !this.outputsEmpty()) {
            this.abort();
            return;
        }
        for (FabricationMatcher.SlotTake take : plan.get()) {
            int slot = SuitForgeLayout.MATERIAL_START + take.slot();
            ItemStack stack = this.inventory.getStackInSlot(slot).copy();
            stack.shrink(take.amount());
            this.inventory.setStackInSlot(slot, stack);
        }
        ItemStack core = this.inventory.getStackInSlot(SuitForgeLayout.POWER_CORE).copy();
        core.shrink(1);
        this.inventory.setStackInSlot(SuitForgeLayout.POWER_CORE, core);
        if (recipe.consumeBlueprint()) {
            this.inventory.setStackInSlot(SuitForgeLayout.BLUEPRINT, ItemStack.EMPTY);
        }
        List<ItemStack> results = recipe.results();
        for (int i = 0; i < results.size(); i++) {
            this.inventory.setStackInSlot(SuitForgeLayout.OUTPUT_START + i, results.get(i));
        }
        this.fabricating = false;
        this.progress = 0;
        this.activeRecipe = null;
        this.status = this.computeStatus();
        this.setWorking(false);
        this.setChanged();
        level.playSound(null, pos, ModSounds.SUIT_FABRICATE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.1);
    }

    private void abort() {
        this.fabricating = false;
        this.progress = 0;
        this.activeRecipe = null;
        this.status = this.computeStatus();
        this.setWorking(false);
        this.setChanged();
    }

    private void setWorking(boolean working) {
        if (this.level != null) {
            BlockState state = this.getBlockState();
            if (state.hasProperty(SuitForgeBlock.WORKING) && state.getValue(SuitForgeBlock.WORKING) != working) {
                this.level.setBlock(this.worldPosition, state.setValue(SuitForgeBlock.WORKING, working), Block.UPDATE_ALL);
            }
        }
    }

    public int comparatorSignal() {
        if (this.fabricating && this.maxProgress > 0) {
            return 1 + Math.min(14, this.progress * 14 / this.maxProgress);
        }
        return this.status == Status.READY ? 15 : 0;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < this.inventory.getSlots(); i++) {
            ItemStack stack = this.inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", this.inventory.serializeNBT());
        tag.putInt("progress", this.progress);
        tag.putInt("max_progress", this.maxProgress);
        tag.putBoolean("fabricating", this.fabricating);
        if (this.activeRecipe != null) {
            tag.putString("recipe", this.activeRecipe.toString());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.inventory.deserializeNBT(tag.getCompound("inventory"));
        this.progress = tag.getInt("progress");
        this.maxProgress = tag.getInt("max_progress");
        this.fabricating = tag.getBoolean("fabricating");
        this.activeRecipe = tag.contains("recipe") ? ResourceLocation.tryParse(tag.getString("recipe")) : null;
        this.statusDirty = true;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.powersnj.suit_forge");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SuitForgeMenu(containerId, inventory, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return this.automation.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.automation.invalidate();
    }

    /**
     * Hopper/pipe access: inputs can be inserted (validated), only outputs can be extracted.
     */
    private record AutomationHandler(ItemStackHandler inner) implements IItemHandler {

        @Override
        public int getSlots() {
            return this.inner.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return this.inner.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return SuitForgeLayout.isOutputSlot(slot) ? stack : this.inner.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return SuitForgeLayout.isOutputSlot(slot) ? this.inner.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return this.inner.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return !SuitForgeLayout.isOutputSlot(slot) && this.inner.isItemValid(slot, stack);
        }
    }
}
