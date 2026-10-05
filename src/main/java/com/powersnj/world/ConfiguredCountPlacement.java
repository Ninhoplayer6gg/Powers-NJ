package com.powersnj.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.powersnj.core.config.PowersSettings;
import com.powersnj.core.config.Settings;
import com.powersnj.registry.ModPlacementModifiers;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.placement.RepeatingPlacement;

/**
 * {@code powersnj:configured_count} placement modifier: number of ore veins per chunk read from the
 * server config at generation time ({@code worldgenEnabled}, {@code viltrumiteVeinsPerChunk},
 * {@code speedCrystalVeinsPerChunk}). Returns 0 when worldgen is disabled, so ores can be switched
 * off without editing data packs.
 */
public final class ConfiguredCountPlacement extends RepeatingPlacement {

    public static final Codec<ConfiguredCountPlacement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("ore").forGetter(p -> p.ore)
    ).apply(instance, ConfiguredCountPlacement::new));

    public static final String VILTRUMITE = "viltrumite";
    public static final String SPEED_CRYSTAL = "speed_crystal";

    private final String ore;

    public ConfiguredCountPlacement(String ore) {
        this.ore = ore;
    }

    public static int veinsFor(String ore, PowersSettings settings) {
        if (!settings.worldgenEnabled()) {
            return 0;
        }
        return switch (ore) {
            case VILTRUMITE -> settings.viltrumiteVeinsPerChunk();
            case SPEED_CRYSTAL -> settings.speedCrystalVeinsPerChunk();
            default -> 0;
        };
    }

    @Override
    protected int count(RandomSource random, BlockPos pos) {
        return veinsFor(this.ore, Settings.get());
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifiers.CONFIGURED_COUNT.get();
    }
}
