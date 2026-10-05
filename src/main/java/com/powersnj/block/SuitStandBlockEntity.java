package com.powersnj.block;

import com.powersnj.registry.ModBlockEntities;
import com.powersnj.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Stores one suit (head, chest, legs, feet). Contents are saved to NBT and synced to every client
 * tracking the chunk through the standard block entity update packet, so all players see the
 * displayed suit.
 */
public class SuitStandBlockEntity extends BlockEntity {

    /** Slot order inside the handler. */
    public static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private final ItemStackHandler items = new ItemStackHandler(SLOTS.size()) {
        @Override
        protected void onContentsChanged(int slot) {
            SuitStandBlockEntity.this.markUpdated();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == SLOTS.get(slot);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    public SuitStandBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUIT_STAND.get(), pos, state);
    }

    public ItemStack getItem(EquipmentSlot slot) {
        int index = SLOTS.indexOf(slot);
        return index < 0 ? ItemStack.EMPTY : this.items.getStackInSlot(index);
    }

    public void setItem(EquipmentSlot slot, ItemStack stack) {
        int index = SLOTS.indexOf(slot);
        if (index >= 0) {
            this.items.setStackInSlot(index, stack);
        }
    }

    public int pieceCount() {
        int count = 0;
        for (int i = 0; i < this.items.getSlots(); i++) {
            if (!this.items.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public boolean isEmpty() {
        return this.pieceCount() == 0;
    }

    /**
     * Server-side interaction logic (see {@link SuitStandBlock}).
     *
     * @return true when something changed
     */
    public boolean interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof ArmorItem armor && SLOTS.contains(armor.getEquipmentSlot())) {
            EquipmentSlot slot = armor.getEquipmentSlot();
            ItemStack previous = this.getItem(slot);
            ItemStack placed = held.split(1);
            this.setItem(slot, placed);
            if (!previous.isEmpty() && !player.getInventory().add(previous)) {
                player.drop(previous, false);
            }
            this.playSound();
            return true;
        }
        if (!held.isEmpty() || hand != InteractionHand.MAIN_HAND) {
            return false;
        }
        if (player.isShiftKeyDown()) {
            boolean changed = false;
            for (EquipmentSlot slot : SLOTS) {
                ItemStack stack = this.getItem(slot);
                if (!stack.isEmpty()) {
                    this.setItem(slot, ItemStack.EMPTY);
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                    changed = true;
                }
            }
            if (changed) {
                this.playSound();
            }
            return changed;
        }
        return this.swapWith(player);
    }

    /**
     * Swaps the worn armor of {@code entity} with the stored suit, piece by piece.
     */
    public boolean swapWith(LivingEntity entity) {
        boolean changed = false;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack worn = entity.getItemBySlot(slot);
            ItemStack stored = this.getItem(slot);
            if (worn.isEmpty() && stored.isEmpty()) {
                continue;
            }
            entity.setItemSlot(slot, stored.copy());
            this.setItem(slot, worn.copy());
            changed = true;
        }
        if (changed) {
            this.playSound();
        }
        return changed;
    }

    private void playSound() {
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, ModSounds.SUIT_EQUIP.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
        }
    }

    private void markUpdated() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < this.items.getSlots(); i++) {
            ItemStack stack = this.items.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("items", this.items.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.items.deserializeNBT(tag.getCompound("items"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.put("items", this.items.serializeNBT());
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
