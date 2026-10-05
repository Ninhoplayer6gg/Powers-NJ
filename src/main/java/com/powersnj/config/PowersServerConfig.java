package com.powersnj.config;

import com.powersnj.PowersNJ;
import com.powersnj.core.config.PowersSettings;
import com.powersnj.core.config.Settings;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Server config ({@code <world>/serverconfig/powersnj-server.toml}). Every load/reload publishes an
 * immutable {@link PowersSettings} snapshot used by all gameplay code.
 */
@Mod.EventBusSubscriber(modid = PowersNJ.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PowersServerConfig {

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue ENABLE_PVP;
    private static final ForgeConfigSpec.DoubleValue PVP_DAMAGE_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue PVE_DAMAGE_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue BOSS_DAMAGE_MULTIPLIER;
    private static final ForgeConfigSpec.BooleanValue ENABLE_DESTRUCTION;
    private static final ForgeConfigSpec.IntValue MAX_DESTROYED_BLOCKS_PER_ATTACK;
    private static final ForgeConfigSpec.IntValue MAX_DESTROYED_BLOCKS_PER_TICK;
    private static final ForgeConfigSpec.BooleanValue ALLOW_OBSIDIAN_DESTRUCTION;
    private static final ForgeConfigSpec.DoubleValue XP_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue MAX_SPEED_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue ENERGY_REGENERATION_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue COOLDOWN_MULTIPLIER;
    private static final ForgeConfigSpec.BooleanValue ENABLE_PHASING;
    private static final ForgeConfigSpec.IntValue MAX_PHASE_DISTANCE;
    private static final ForgeConfigSpec.BooleanValue ENABLE_VENOM_WEAKNESSES;
    private static final ForgeConfigSpec.BooleanValue WORLDGEN_ENABLED;
    private static final ForgeConfigSpec.IntValue VILTRUMITE_VEINS_PER_CHUNK;
    private static final ForgeConfigSpec.IntValue SPEED_CRYSTAL_VEINS_PER_CHUNK;

    static {
        PowersSettings d = PowersSettings.DEFAULTS;
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Combat balancing. Ability damage is authored for PvE; players and bosses get separate multipliers.").push("combat");
        ENABLE_PVP = b.comment("Allow powers to damage other players.").define("enablePvP", d.enablePvP());
        PVP_DAMAGE_MULTIPLIER = b.comment("Multiplier for power damage dealt by players to players.").defineInRange("pvpDamageMultiplier", d.pvpDamageMultiplier(), 0D, 10D);
        PVE_DAMAGE_MULTIPLIER = b.comment("Multiplier for power damage dealt by players to mobs.").defineInRange("pveDamageMultiplier", d.pveDamageMultiplier(), 0D, 10D);
        BOSS_DAMAGE_MULTIPLIER = b.comment("Extra multiplier (on top of PvE) against bosses (tag forge:bosses).").defineInRange("bossDamageMultiplier", d.bossDamageMultiplier(), 0D, 10D);
        b.pop();

        b.comment("Terrain destruction by powers (ground slam, charge, heavy punch...).").push("destruction");
        ENABLE_DESTRUCTION = b.define("enableDestruction", d.enableDestruction());
        MAX_DESTROYED_BLOCKS_PER_ATTACK = b.comment("Upper bound of blocks a single attack may destroy.").defineInRange("maxDestroyedBlocksPerAttack", d.maxDestroyedBlocksPerAttack(), 0, 512);
        MAX_DESTROYED_BLOCKS_PER_TICK = b.comment("Server-wide cap of blocks destroyed per tick (the rest is queued).").defineInRange("maxDestroyedBlocksPerTick", d.maxDestroyedBlocksPerTick(), 1, 1024);
        ALLOW_OBSIDIAN_DESTRUCTION = b.comment("Allow EXTREME-tier blocks (obsidian class) to be destroyed by the strongest attacks. Bedrock is never breakable.").define("allowObsidianDestruction", d.allowObsidianDestruction());
        b.pop();

        b.push("progression");
        XP_MULTIPLIER = b.comment("Multiplier for all suit XP gains.").defineInRange("xpMultiplier", d.xpMultiplier(), 0D, 100D);
        b.pop();

        b.push("powers");
        MAX_SPEED_MULTIPLIER = b.comment("Scales the top speed of speedsters and flight.").defineInRange("maxSpeedMultiplier", d.maxSpeedMultiplier(), 0.1D, 4D);
        ENERGY_REGENERATION_MULTIPLIER = b.comment("Scales regeneration of every energy resource.").defineInRange("energyRegenerationMultiplier", d.energyRegenerationMultiplier(), 0D, 20D);
        COOLDOWN_MULTIPLIER = b.comment("Scales every ability cooldown (0 = no cooldowns).").defineInRange("cooldownMultiplier", d.cooldownMultiplier(), 0D, 20D);
        ENABLE_PHASING = b.comment("Allow speedsters to phase through blocks.").define("enablePhasing", d.enablePhasing());
        MAX_PHASE_DISTANCE = b.comment("Maximum distance (blocks) of a single phase.").defineInRange("maxPhaseDistance", d.maxPhaseDistance(), 1, 32);
        ENABLE_VENOM_WEAKNESSES = b.comment("Symbiotes take extra damage and destabilise from fire, heat and loud sounds.").define("enableVenomWeaknesses", d.enableVenomWeaknesses());
        b.pop();

        b.push("worldgen");
        WORLDGEN_ENABLED = b.comment("Generate Viltrumite and Speed Crystal ores in new chunks.").define("worldgenEnabled", d.worldgenEnabled());
        VILTRUMITE_VEINS_PER_CHUNK = b.defineInRange("viltrumiteVeinsPerChunk", d.viltrumiteVeinsPerChunk(), 0, 32);
        SPEED_CRYSTAL_VEINS_PER_CHUNK = b.defineInRange("speedCrystalVeinsPerChunk", d.speedCrystalVeinsPerChunk(), 0, 32);
        b.pop();

        SPEC = b.build();
    }

    private PowersServerConfig() {
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        publish(event);
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        publish(event);
    }

    @SubscribeEvent
    public static void onUnload(ModConfigEvent.Unloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            Settings.reset();
        }
    }

    private static void publish(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        Settings.update(new PowersSettings(
                ENABLE_PVP.get(),
                PVP_DAMAGE_MULTIPLIER.get(),
                PVE_DAMAGE_MULTIPLIER.get(),
                BOSS_DAMAGE_MULTIPLIER.get(),
                ENABLE_DESTRUCTION.get(),
                MAX_DESTROYED_BLOCKS_PER_ATTACK.get(),
                MAX_DESTROYED_BLOCKS_PER_TICK.get(),
                ALLOW_OBSIDIAN_DESTRUCTION.get(),
                XP_MULTIPLIER.get(),
                MAX_SPEED_MULTIPLIER.get(),
                ENERGY_REGENERATION_MULTIPLIER.get(),
                COOLDOWN_MULTIPLIER.get(),
                ENABLE_PHASING.get(),
                MAX_PHASE_DISTANCE.get(),
                ENABLE_VENOM_WEAKNESSES.get(),
                WORLDGEN_ENABLED.get(),
                VILTRUMITE_VEINS_PER_CHUNK.get(),
                SPEED_CRYSTAL_VEINS_PER_CHUNK.get()));
        PowersNJ.LOGGER.info("Powers NJ server config {}", event instanceof ModConfigEvent.Reloading ? "reloaded" : "loaded");
    }
}
