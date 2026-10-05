package com.powersnj.compat.palladium.ability;

import com.powersnj.player.PowersPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

/**
 * {@code powersnj:camouflage} - toggle: the symbiote mimics the surroundings. Invisibility while
 * enabled and nearby mobs lose track of the wearer. Drains energy per tick (energy_per_tick).
 */
public class CamouflageAbility extends PowersAbility {

    public static final PalladiumProperty<Float> RADIUS = new FloatProperty("radius").configurable("Radius in which mobs lose their target.");

    public CamouflageAbility() {
        this.withProperty(RADIUS, 16F);
        this.withProperty(ENERGY_PER_TICK, 0.25F);
    }

    @Override
    public boolean onActivated(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        this.whileEnabled(player, instance, data);
        return true;
    }

    @Override
    protected void whileEnabled(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false, false));
        if (player.tickCount % 10 == 0) {
            float radius = instance.getProperty(RADIUS);
            for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius), m -> m.getTarget() == player)) {
                mob.setTarget(null);
            }
        }
    }

    @Override
    protected void onDisabled(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        player.removeEffect(MobEffects.INVISIBILITY);
    }

    @Override
    public String getDocumentationDescription() {
        return "Powers NJ: symbiote camouflage.";
    }
}
