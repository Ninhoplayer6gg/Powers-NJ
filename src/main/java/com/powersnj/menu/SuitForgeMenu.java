package com.powersnj.menu;

import com.powersnj.block.SuitForgeBlockEntity;
import com.powersnj.recipe.SuitForgeLayout;
import com.powersnj.registry.ModBlocks;
import com.powersnj.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Suit Forge container. Slot positions match {@code textures/gui/suit_forge.png} (176x222).
 * The "Fabricate" button uses vanilla's server-validated menu button packet
 * ({@link #clickMenuButton}).
 */
public class SuitForgeMenu extends AbstractContainerMenu {

    public static final int BUTTON_FABRICATE = 0;

    public static final int BLUEPRINT_X = 8;
    public static final int BLUEPRINT_Y = 20;
    public static final int CORE_X = 8;
    public static final int CORE_Y = 56;
    public static final int MATERIALS_X = 30;
    public static final int MATERIALS_Y = 20;
    public static final int OUTPUT_X = 116;
    public static final int OUTPUT_Y = 20;
    public static final int INVENTORY_Y = 140;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    @Nullable
    private final SuitForgeBlockEntity blockEntity;
    private final IItemHandler handler;

    public SuitForgeMenu(int containerId, Inventory playerInventory, @Nullable SuitForgeBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.SUIT_FORGE.get(), containerId);
        checkContainerDataCount(data, SuitForgeBlockEntity.DATA_COUNT);
        this.blockEntity = blockEntity;
        this.data = data;
        this.handler = blockEntity != null ? blockEntity.getInventory() : new ItemStackHandler(SuitForgeLayout.SIZE);
        this.access = blockEntity != null && blockEntity.getLevel() != null
                ? ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()) : ContainerLevelAccess.NULL;

        this.addSlot(new FilteredSlot(this.handler, SuitForgeLayout.BLUEPRINT, BLUEPRINT_X, BLUEPRINT_Y));
        this.addSlot(new FilteredSlot(this.handler, SuitForgeLayout.POWER_CORE, CORE_X, CORE_Y));
        for (int i = 0; i < SuitForgeLayout.MATERIAL_COUNT; i++) {
            this.addSlot(new FilteredSlot(this.handler, SuitForgeLayout.MATERIAL_START + i, MATERIALS_X + (i % 3) * 18, MATERIALS_Y + (i / 3) * 18));
        }
        for (int i = 0; i < SuitForgeLayout.OUTPUT_COUNT; i++) {
            this.addSlot(new FilteredSlot(this.handler, SuitForgeLayout.OUTPUT_START + i, OUTPUT_X + (i % 2) * 18, OUTPUT_Y + (i / 2) * 18));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, INVENTORY_Y + 58));
        }

        this.addDataSlots(data);
    }

    /**
     * Client constructor (IForgeMenuType): the server sends the block position.
     */
    public static SuitForgeMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        SuitForgeBlockEntity be = playerInventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof SuitForgeBlockEntity forge ? forge : null;
        return new SuitForgeMenu(containerId, playerInventory, be, new SimpleContainerData(SuitForgeBlockEntity.DATA_COUNT));
    }

    public @Nullable SuitForgeBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public IItemHandler getHandler() {
        return this.handler;
    }

    public int getProgress() {
        return this.data.get(SuitForgeBlockEntity.DATA_PROGRESS);
    }

    public int getMaxProgress() {
        return this.data.get(SuitForgeBlockEntity.DATA_MAX_PROGRESS);
    }

    public SuitForgeBlockEntity.Status getStatus() {
        return SuitForgeBlockEntity.Status.byId(this.data.get(SuitForgeBlockEntity.DATA_STATUS));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_FABRICATE && this.blockEntity != null && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(this.blockEntity.tryStartFabrication(serverPlayer), true);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.SUIT_FORGE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int machineSlots = SuitForgeLayout.SIZE;
        int total = this.slots.size();

        if (index < machineSlots) {
            if (!this.moveItemStackTo(stack, machineSlots, total, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            if (SuitForgeBlockEntity.isValidForSlot(SuitForgeLayout.BLUEPRINT, stack)) {
                moved = this.moveItemStackTo(stack, SuitForgeLayout.BLUEPRINT, SuitForgeLayout.BLUEPRINT + 1, false);
            } else if (SuitForgeBlockEntity.isValidForSlot(SuitForgeLayout.POWER_CORE, stack)) {
                moved = this.moveItemStackTo(stack, SuitForgeLayout.POWER_CORE, SuitForgeLayout.POWER_CORE + 1, false);
            }
            if (!moved && SuitForgeBlockEntity.isValidForSlot(SuitForgeLayout.MATERIAL_START, stack)) {
                moved = this.moveItemStackTo(stack, SuitForgeLayout.MATERIAL_START, SuitForgeLayout.OUTPUT_START, false);
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    /**
     * Slot enforcing the forge rules on the client too (so ghost placements never happen).
     */
    private static final class FilteredSlot extends SlotItemHandler {

        private final int index;

        FilteredSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
            this.index = index;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return SuitForgeBlockEntity.isValidForSlot(this.index, stack);
        }

        @Override
        public int getMaxStackSize() {
            return this.index == SuitForgeLayout.BLUEPRINT ? 1 : super.getMaxStackSize();
        }
    }
}
