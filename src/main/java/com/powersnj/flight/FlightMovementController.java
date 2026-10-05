package com.powersnj.flight;

import com.powersnj.combat.CombatService;
import com.powersnj.compat.palladium.PalladiumBridge;
import com.powersnj.core.config.Settings;
import com.powersnj.core.energy.EnergyPool;
import com.powersnj.core.flight.FlightController;
import com.powersnj.core.flight.FlightProfile;
import com.powersnj.core.net.MovementSnapshot;
import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.movement.AttributeHelper;
import com.powersnj.movement.MotionTracker;
import com.powersnj.movement.MovementController;
import com.powersnj.movement.MovementControllers;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.progression.ProgressionEvents;
import com.powersnj.registry.ModDamageTypes;
import com.powersnj.registry.ModParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.entity.PalladiumAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Generic flight movement controller (used by Thragg, reusable by any flyer). Wraps the pure
 * {@link FlightController} and drives Palladium's flight through the
 * {@code palladium:flight_speed} attribute, so the client movement stays Palladium's well tested
 * heroic flight while speed tiers, boosts, energy and ramming damage are decided by the server.
 */
public final class FlightMovementController implements MovementController {

    private static final UUID FLIGHT_SPEED_ID = UUID.fromString("5f0e0b8e-6b77-4d0f-9a4b-3f2f51f0a001");
    private static final UUID HEROIC_TYPE_ID = UUID.fromString("5f0e0b8e-6b77-4d0f-9a4b-3f2f51f0a002");
    private static final int IMPACT_COOLDOWN = 10;

    private final FlightController controller;
    private final MotionTracker motion = new MotionTracker();
    private final Map<Integer, Integer> impactCooldowns = new HashMap<>();
    private boolean flying;

    public FlightMovementController(SuitDefinition definition) {
        this.controller = new FlightController(FlightProfile.from(definition));
    }

    public FlightController controller() {
        return this.controller;
    }

    public boolean isFlying() {
        return this.flying;
    }

    @Override
    public String id() {
        return MovementControllers.FLIGHT;
    }

    @Override
    public void activate(ServerPlayer player, PowersPlayerData data) {
        this.motion.reset();
        AttributeHelper.set(player, PalladiumAttributes.HEROIC_FLIGHT_TYPE.get(), HEROIC_TYPE_ID, "powersnj heroic flight", 1D, AttributeModifier.Operation.ADDITION, 0.01D);
    }

    @Override
    public void tick(ServerPlayer player, PowersPlayerData data, SuitDefinition definition) {
        this.flying = PalladiumBridge.isFlying(player);
        double moved = this.motion.update(player.position());
        double speed = this.motion.lastTotal();
        EnergyPool energy = data.activeEnergy(false).orElse(null);

        FlightController.FlightTick tick = this.controller.tick(this.flying, energy, Settings.get().maxSpeedMultiplier());
        AttributeHelper.set(player, PalladiumAttributes.FLIGHT_SPEED.get(), FLIGHT_SPEED_ID, "powersnj flight", tick.attributeValue(),
                AttributeModifier.Operation.ADDITION, 0.02D);
        if (tick.highSpeedCancelled()) {
            player.displayClientMessage(Component.translatable("message.powersnj.flight.exhausted").withStyle(ChatFormatting.RED), true);
        }

        if (this.flying) {
            player.fallDistance = 0F;
            ProgressionEvents.onTravel(player, data, moved);
            this.aerialImpacts(player, speed);
        }
        this.impactCooldowns.replaceAll((id, ticks) -> ticks - 1);
        this.impactCooldowns.values().removeIf(ticks -> ticks <= 0);
    }

    /**
     * Aerial combat: flying through entities at high speed rams them.
     */
    private void aerialImpacts(ServerPlayer player, double speed) {
        float damage = this.controller.impactDamage(speed);
        if (damage <= 0F) {
            return;
        }
        Vec3 direction = player.getLookAngle();
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(0.6D),
                e -> e != player && e.isAlive() && !this.impactCooldowns.containsKey(e.getId()))) {
            if (CombatService.dealAbilityDamage(player, target, damage, ModDamageTypes.IMPACT, false)) {
                CombatService.launch(target, target.getDeltaMovement().add(direction.scale(Math.min(3D, speed))).add(0, 0.3D, 0));
                this.impactCooldowns.put(target.getId(), IMPACT_COOLDOWN);
                if (player.level() instanceof ServerLevel level) {
                    level.sendParticles(ModParticles.VILTRUMITE_IMPACT.get(), target.getX(), target.getY(0.5D), target.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
                }
            }
        }
    }

    @Override
    public void deactivate(ServerPlayer player, PowersPlayerData data) {
        AttributeHelper.remove(player, PalladiumAttributes.FLIGHT_SPEED.get(), FLIGHT_SPEED_ID);
        AttributeHelper.remove(player, PalladiumAttributes.HEROIC_FLIGHT_TYPE.get(), HEROIC_TYPE_ID);
        this.controller.setHighSpeed(false);
    }

    @Override
    public MovementSnapshot snapshot(ServerPlayer player) {
        return new MovementSnapshot(player.getId(), MovementControllers.FLIGHT, this.controller.mode().name(), (float) this.controller.currentSpeed(),
                0, this.flying ? MovementSnapshot.FLAG_ACTIVE : 0);
    }
}
