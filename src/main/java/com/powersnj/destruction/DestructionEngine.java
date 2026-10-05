package com.powersnj.destruction;

import com.powersnj.core.config.PowersSettings;
import com.powersnj.core.config.Settings;
import com.powersnj.core.destruction.BlockTraits;
import com.powersnj.core.destruction.DestructionClassifier;
import com.powersnj.core.destruction.DestructionPlanner;
import com.powersnj.core.destruction.DestructionTier;
import com.powersnj.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Central terrain destruction service. Every destructive power goes through here:
 * <ol>
 *     <li>blocks are classified FRAGILE / NORMAL / HARD / EXTREME / PROTECTED
 *     ({@link DestructionClassifier}, overridable with {@code powersnj:destruction/*} block tags);</li>
 *     <li>a bounded plan is built ({@code maxDestroyedBlocksPerAttack});</li>
 *     <li>the plan is executed over several ticks by {@link DestructionScheduler}
 *     ({@code maxDestroyedBlocksPerTick}) with protection / claim checks per block.</li>
 * </ol>
 * Bedrock and other unbreakable blocks are always PROTECTED.
 */
public final class DestructionEngine {

    private DestructionEngine() {
    }

    public static BlockTraits traits(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        Set<String> tags = new HashSet<>();
        for (TagKey<Block> tag : ModTags.DESTRUCTION_TAGS) {
            if (state.is(tag)) {
                tags.add(tag.location().toString());
            }
        }
        return new BlockTraits(id == null ? "minecraft:air" : id.toString(), state.getDestroySpeed(level, pos), block.getExplosionResistance(),
                state.isAir(), block instanceof LiquidBlock, state.hasBlockEntity(), tags);
    }

    public static DestructionTier classify(Level level, BlockPos pos) {
        return DestructionClassifier.classify(traits(level, pos));
    }

    /**
     * Whether destruction is allowed for this attacker at all (config, mob griefing for mobs).
     */
    public static boolean isAllowed(ServerLevel level, @Nullable Entity attacker) {
        PowersSettings settings = Settings.get();
        if (!settings.enableDestruction() || settings.maxDestroyedBlocksPerAttack() <= 0) {
            return false;
        }
        return attacker instanceof Player || level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    /**
     * Spherical crater (ground slam, heavy impacts).
     *
     * @return number of blocks scheduled
     */
    public static int crater(ServerLevel level, @Nullable Entity attacker, BlockPos center, double radius, DestructionTier power) {
        if (!isAllowed(level, attacker)) {
            return 0;
        }
        PowersSettings settings = Settings.get();
        List<DestructionPlanner.Target> plan = DestructionPlanner.sphere(center.getX(), center.getY(), center.getZ(), radius, power,
                settings.allowObsidianDestruction(), settings.maxDestroyedBlocksPerAttack(), (x, y, z) -> lookup(level, x, y, z));
        DestructionScheduler.enqueue(level, attacker, plan, power);
        return plan.size();
    }

    /**
     * Straight tunnel (charges, thrown entities).
     */
    public static int tunnel(ServerLevel level, @Nullable Entity attacker, BlockPos start, double dirX, double dirZ, int length, int width, int height, DestructionTier power) {
        if (!isAllowed(level, attacker)) {
            return 0;
        }
        PowersSettings settings = Settings.get();
        List<DestructionPlanner.Target> plan = DestructionPlanner.line(start.getX(), start.getY(), start.getZ(), dirX, dirZ, length, width, height, power,
                settings.allowObsidianDestruction(), settings.maxDestroyedBlocksPerAttack(), (x, y, z) -> lookup(level, x, y, z));
        DestructionScheduler.enqueue(level, attacker, plan, power);
        return plan.size();
    }

    private static @Nullable BlockTraits lookup(ServerLevel level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (level.isOutsideBuildHeight(pos) || !level.isLoaded(pos)) {
            return null;
        }
        return traits(level, pos);
    }
}
