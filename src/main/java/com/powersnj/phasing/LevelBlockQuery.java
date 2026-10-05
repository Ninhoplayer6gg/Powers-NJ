package com.powersnj.phasing;

import com.powersnj.core.phasing.BlockQuery;
import com.powersnj.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@link BlockQuery} over a live {@link Level}. Unloaded chunks, the world border and the build
 * height count as "out of world" so a phase can never end there.
 */
public final class LevelBlockQuery implements BlockQuery {

    private final Level level;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    public LevelBlockQuery(Level level) {
        this.level = level;
    }

    private BlockState state(int x, int y, int z) {
        return this.level.getBlockState(this.cursor.set(x, y, z));
    }

    @Override
    public boolean isSolid(int x, int y, int z) {
        BlockState state = this.state(x, y, z);
        return !state.getCollisionShape(this.level, this.cursor).isEmpty();
    }

    @Override
    public boolean isPhaseProof(int x, int y, int z) {
        BlockState state = this.state(x, y, z);
        return state.is(ModTags.PHASE_PROOF) || state.getDestroySpeed(this.level, this.cursor) < 0F;
    }

    @Override
    public boolean isHazard(int x, int y, int z) {
        BlockState state = this.state(x, y, z);
        return state.getFluidState().is(FluidTags.LAVA) || state.is(BlockTags.FIRE) || state.is(BlockTags.CAMPFIRES)
                || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.WITHER_ROSE);
    }

    @Override
    public boolean isOutOfWorld(int x, int y, int z) {
        this.cursor.set(x, y, z);
        return this.level.isOutsideBuildHeight(this.cursor) || !this.level.isLoaded(this.cursor) || !this.level.getWorldBorder().isWithinBounds(this.cursor);
    }
}
