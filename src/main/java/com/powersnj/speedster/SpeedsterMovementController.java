package com.powersnj.speedster;

import com.powersnj.PowersNJ;
import com.powersnj.combat.CombatService;
import com.powersnj.core.config.PowersSettings;
import com.powersnj.core.config.Settings;
import com.powersnj.core.energy.EnergyPool;
import com.powersnj.core.net.MovementSnapshot;
import com.powersnj.core.speedster.SpeedsterEngine;
import com.powersnj.core.speedster.SpeedsterEvent;
import com.powersnj.core.speedster.SpeedsterInput;
import com.powersnj.core.speedster.SpeedsterProfile;
import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.movement.AttributeHelper;
import com.powersnj.movement.MotionTracker;
import com.powersnj.movement.MovementController;
import com.powersnj.movement.MovementControllers;
import com.powersnj.network.NbtDataNode;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.progression.ProgressionEvents;
import com.powersnj.registry.ModDamageTypes;
import com.powersnj.registry.ModParticles;
import com.powersnj.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Applies the {@link SpeedsterEngine} to a player. The engine's multiplier becomes a transient
 * movement-speed modifier decided by the server (vanilla syncs it to the client), never a
 * client-trusted value and never a plain Speed effect.
 * <p>
 * Server responsibilities: energy drain, collisions (walls and entities), wall/water running
 * state, step height, movement validation, travel XP and throttled multiplayer sync. Client
 * responsibilities (prediction only): turning control, wall/water running motion, FOV, trails and
 * particles ({@code com.powersnj.client.ClientMovementHandler} and {@code SpeedsterVisuals}).
 */
public final class SpeedsterMovementController implements MovementController {

    public static final String TRAIT_WALL_RUNNING = "wall_running";
    public static final String TRAIT_WATER_RUNNING = "water_running";
    /** Normal sprinting distance per tick of a player, used by movement validation. */
    public static final double BASE_SPRINT_PER_TICK = 0.28D;

    private static final UUID SPEED_ID = UUID.fromString("5f0e0b8e-6b77-4d0f-9a4b-3f2f51f0b001");
    private static final UUID STEP_ID = UUID.fromString("5f0e0b8e-6b77-4d0f-9a4b-3f2f51f0b002");
    private static final int HIT_COOLDOWN = 12;

    private final SpeedsterEngine engine;
    private final MotionTracker motion = new MotionTracker();
    private final Map<Integer, Integer> hitCooldowns = new HashMap<>();

    public SpeedsterMovementController(SuitDefinition definition) {
        this.engine = new SpeedsterEngine(SpeedsterProfile.from(definition));
    }

    public SpeedsterEngine engine() {
        return this.engine;
    }

    @Override
    public String id() {
        return MovementControllers.SPEEDSTER;
    }

    /**
     * Toggles super speed (called by the {@code powersnj:super_speed} ability).
     */
    public void setActive(ServerPlayer player, boolean active) {
        if (this.engine.isActive() == active) {
            return;
        }
        this.engine.setActive(active);
        player.level().playSound(null, player.blockPosition(), active ? ModSounds.SPEED_START.get() : ModSounds.SPEED_STOP.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    @Override
    public void activate(ServerPlayer player, PowersPlayerData data) {
        this.motion.reset();
    }

    @Override
    public void tick(ServerPlayer player, PowersPlayerData data, SuitDefinition definition) {
        PowersSettings settings = Settings.get();
        double moved = this.motion.update(player.position());
        BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.1D, player.getZ());
        boolean onWaterSurface = player.level().getFluidState(below).is(FluidTags.WATER) && !player.isUnderWater();

        SpeedsterInput input = new SpeedsterInput(moved > 0.05D, player.onGround(), player.horizontalCollision, onWaterSurface,
                data.hasTrait(TRAIT_WALL_RUNNING), data.hasTrait(TRAIT_WATER_RUNNING));
        EnergyPool energy = data.activeEnergy(false).orElse(null);
        SpeedsterEngine.SpeedsterTick tick = this.engine.tick(input, energy, settings);

        AttributeHelper.set(player, Attributes.MOVEMENT_SPEED, SPEED_ID, "powersnj speedster", tick.multiplier() - 1D,
                AttributeModifier.Operation.MULTIPLY_TOTAL, 0.05D);
        AttributeHelper.set(player, ForgeMod.STEP_HEIGHT_ADDITION.get(), STEP_ID, "powersnj speedster step",
                this.engine.isActive() ? this.engine.profile().stepHeightBonus() : 0D, AttributeModifier.Operation.ADDITION, 0.01D);

        this.handleEvents(player, tick);

        if (tick.wallRunning() || tick.waterRunning() || this.engine.isActive()) {
            player.fallDistance = 0F;
        }
        if (this.engine.isActive()) {
            ProgressionEvents.onTravel(player, data, moved);
            this.collideWithEntities(player, moved);
            this.validate(player, moved);
        }
        this.hitCooldowns.replaceAll((id, ticks) -> ticks - 1);
        this.hitCooldowns.values().removeIf(ticks -> ticks <= 0);
    }

    private void handleEvents(ServerPlayer player, SpeedsterEngine.SpeedsterTick tick) {
        ServerLevel level = player.serverLevel();
        for (SpeedsterEvent event : tick.events()) {
            switch (event) {
                case OUT_OF_ENERGY -> player.displayClientMessage(Component.translatable("message.powersnj.speedster.exhausted").withStyle(ChatFormatting.RED), true);
                case WALL_IMPACT -> {
                    level.playSound(null, player.blockPosition(), ModSounds.SPEED_STOP.get(), SoundSource.PLAYERS, 1.0F, 0.6F);
                    level.sendParticles(ModParticles.SPEED_SPARK.get(), player.getX(), player.getY(1.0D), player.getZ(), 12, 0.4, 0.4, 0.4, 0.2);
                }
                case TOP_SPEED_REACHED -> level.sendParticles(ModParticles.NEGATIVE_LIGHTNING.get(), player.getX(), player.getY(0.6D), player.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
                case STOPPED -> level.playSound(null, player.blockPosition(), ModSounds.SPEED_STOP.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
                default -> {
                }
            }
        }
    }

    /**
     * Running into entities at high speed hits them (multiplayer-safe: server decides).
     */
    private void collideWithEntities(ServerPlayer player, double moved) {
        float damage = this.engine.collisionDamage();
        if (damage <= 0F || moved < 0.5D) {
            return;
        }
        Vec3 direction = new Vec3(player.getX() - player.xo, 0, player.getZ() - player.zo);
        direction = direction.lengthSqr() < 1.0E-6 ? player.getLookAngle().multiply(1, 0, 1).normalize() : direction.normalize();
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(0.4D),
                e -> e != player && e.isAlive() && !this.hitCooldowns.containsKey(e.getId()))) {
            if (CombatService.dealAbilityDamage(player, target, damage, ModDamageTypes.SPEED_STRIKE, false)) {
                CombatService.launch(target, target.getDeltaMovement().add(direction.scale(1.2D)).add(0, 0.35D, 0));
                this.hitCooldowns.put(target.getId(), HIT_COOLDOWN);
            }
        }
    }

    /**
     * Server validation: the client may not move faster than the engine allows.
     */
    private void validate(ServerPlayer player, double moved) {
        if (player.isPassenger() || player.isFallFlying() || player.getAbilities().flying) {
            return;
        }
        this.engine.validateMovement(moved, BASE_SPRINT_PER_TICK);
        if (this.engine.isFlagged()) {
            PowersNJ.LOGGER.warn("Speedster movement of {} exceeded the server speed engine, disabling super speed", player.getGameProfile().getName());
            this.engine.resetViolations();
            this.setActive(player, false);
            player.displayClientMessage(Component.translatable("message.powersnj.speedster.desync").withStyle(ChatFormatting.RED), true);
        }
    }

    @Override
    public void deactivate(ServerPlayer player, PowersPlayerData data) {
        AttributeHelper.remove(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
        AttributeHelper.remove(player, ForgeMod.STEP_HEIGHT_ADDITION.get(), STEP_ID);
    }

    @Override
    public MovementSnapshot snapshot(ServerPlayer player) {
        int flags = 0;
        if (this.engine.isActive()) {
            flags |= MovementSnapshot.FLAG_ACTIVE;
        }
        if (this.engine.isWallRunning()) {
            flags |= MovementSnapshot.FLAG_WALL_RUNNING;
        }
        if (this.engine.isWaterRunning()) {
            flags |= MovementSnapshot.FLAG_WATER_RUNNING;
        }
        return new MovementSnapshot(player.getId(), MovementControllers.SPEEDSTER, this.engine.isActive() ? "RUNNING" : "IDLE",
                (float) this.engine.currentMultiplier(), this.engine.speedLevel(), flags);
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        this.engine.write(new NbtDataNode(tag));
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        this.engine.read(new NbtDataNode(tag));
    }
}
