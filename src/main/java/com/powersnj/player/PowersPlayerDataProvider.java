package com.powersnj.player;

import com.powersnj.PowersNJ;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Attaches {@link PowersPlayerData} to players and persists it in the player NBT.
 */
public final class PowersPlayerDataProvider implements ICapabilitySerializable<CompoundTag> {

    public static final ResourceLocation ID = PowersNJ.id("player_data");

    private final PowersPlayerData data = new PowersPlayerData();
    private final LazyOptional<PowersPlayerData> optional = LazyOptional.of(() -> this.data);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return PowersPlayerData.CAPABILITY.orEmpty(cap, this.optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return this.data.save();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.data.load(tag);
    }

    public void invalidate() {
        this.optional.invalidate();
    }
}
