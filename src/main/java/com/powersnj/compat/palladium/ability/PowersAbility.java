package com.powersnj.compat.palladium.ability;

import com.powersnj.ability.AbilityExecutor;
import com.powersnj.compat.palladium.PalladiumBridge;
import com.powersnj.core.ability.AbilityCost;
import com.powersnj.player.PowersPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.StringProperty;

/**
 * Base of every Powers NJ Palladium ability type.
 * <p>
 * Palladium owns input (key binds, ability bar, ability wheel), unlocking/enabling conditions and
 * the power screen. This layer adds what Palladium does not cover: server-validated activation
 * ({@link AbilityExecutor}), energy costs from Powers NJ resources, config-scaled cooldowns,
 * skill/level gates and suit XP. All behaviour runs on the logical server only.
 * <ul>
 *     <li>One-shot abilities (enabling condition {@code palladium:action}) implement
 *     {@link #onActivated}.</li>
 *     <li>Toggle/held abilities additionally implement {@link #whileEnabled} / {@link #onDisabled}.</li>
 * </ul>
 */
public abstract class PowersAbility extends Ability {

    public static final PalladiumProperty<Float> ENERGY_COST = new FloatProperty("energy_cost").configurable("Energy (suit resource) consumed when the ability activates. Server validated.");
    public static final PalladiumProperty<Float> ENERGY_PER_TICK = new FloatProperty("energy_per_tick").configurable("Energy drained every tick while a toggle/held ability is enabled.");
    public static final PalladiumProperty<Integer> COOLDOWN = new IntegerProperty("cooldown_ticks").configurable("Cooldown in ticks, scaled by the server cooldownMultiplier. Use this instead of the condition cooldown.");
    public static final PalladiumProperty<String> REQUIRED_SKILL = new StringProperty("required_skill").configurable("Skill of the suit skill tree that must be unlocked (empty = none).");
    public static final PalladiumProperty<Integer> MIN_LEVEL = new IntegerProperty("min_level").configurable("Minimum suit level.");
    public static final PalladiumProperty<Float> XP_REWARD = new FloatProperty("xp_reward").configurable("Suit XP per successful activation. -1 uses the suit default.");

    protected PowersAbility() {
        this.withProperty(ENERGY_COST, 0F);
        this.withProperty(ENERGY_PER_TICK, 0F);
        this.withProperty(COOLDOWN, 0);
        this.withProperty(REQUIRED_SKILL, "");
        this.withProperty(MIN_LEVEL, 1);
        this.withProperty(XP_REWARD, -1F);
    }

    public AbilityCost cost(AbilityInstance instance, String key) {
        return new AbilityCost(key, instance.getProperty(REQUIRED_SKILL), instance.getProperty(MIN_LEVEL),
                instance.getProperty(ENERGY_COST), instance.getProperty(COOLDOWN));
    }

    /**
     * @return true when a server config switch disables this ability type
     */
    public boolean isDisabledByConfig() {
        return false;
    }

    /**
     * Passive abilities (always-on modifiers/traits) never produce activation feedback or become the
     * HUD's selected ability.
     */
    public boolean isPassive() {
        return false;
    }

    /**
     * @return whether a "no target" result should be reported to the player
     */
    public boolean reportsNoTarget() {
        return false;
    }

    /**
     * Whether an enabled-but-inactive ability retries its activation (toggles and passives).
     */
    protected boolean retriesWhileEnabled() {
        return true;
    }

    /**
     * Runs once when the ability becomes enabled and passed the gate.
     *
     * @return false when nothing happened (no target...): no energy or cooldown is consumed
     */
    public abstract boolean onActivated(ServerPlayer player, AbilityInstance instance, PowersPlayerData data);

    /**
     * Runs every tick while a toggle/held ability stays enabled and sustainable.
     */
    protected void whileEnabled(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
    }

    /**
     * Runs when the ability gets disabled (key released / toggled off / not sustainable).
     */
    protected void onDisabled(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance instance, IPowerHolder holder, boolean enabled) {
        if (enabled && !entity.level().isClientSide && entity instanceof ServerPlayer player) {
            AbilityExecutor.activate(player, instance, this);
        }
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance instance, IPowerHolder holder, boolean enabled) {
        if (!enabled || entity.level().isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        PowersPlayerData data = PowersPlayerData.get(player).orElse(null);
        if (data == null) {
            return;
        }
        String key = PalladiumBridge.cooldownKey(instance);
        boolean wasActive = data.activeAbilities().contains(key);
        if (!wasActive && this.retriesWhileEnabled() && player.tickCount % 20 == 0) {
            // A toggle/passive that could not start (suit not detected yet, skill locked, no energy)
            // keeps trying quietly while Palladium keeps it enabled.
            AbilityExecutor.activateQuietly(player, instance, this);
            return;
        }
        if (AbilityExecutor.sustain(player, instance, this, data)) {
            this.whileEnabled(player, instance, data);
        } else if (wasActive) {
            this.onDisabled(player, instance, data);
        }
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance instance, IPowerHolder holder, boolean enabled) {
        if (entity.level().isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        PowersPlayerData.get(player).ifPresent(data -> {
            if (data.activeAbilities().remove(PalladiumBridge.cooldownKey(instance))) {
                this.onDisabled(player, instance, data);
            }
        });
    }
}
