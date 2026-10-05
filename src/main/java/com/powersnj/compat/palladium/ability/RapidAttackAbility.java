package com.powersnj.compat.palladium.ability;

import com.powersnj.combat.CombatService;
import com.powersnj.combat.Targeting;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.registry.ModDamageTypes;
import com.powersnj.registry.ModParticles;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

/**
 * {@code powersnj:rapid_attack} - held: a flurry of hits on the target in sight every few ticks,
 * ignoring hurt invulnerability. Drains energy per tick.
 */
public class RapidAttackAbility extends PowersAbility {

    public static final PalladiumProperty<Float> DAMAGE = new FloatProperty("damage").configurable("Damage per hit (PvE value).");
    public static final PalladiumProperty<Integer> INTERVAL = new IntegerProperty("interval").configurable("Ticks between hits.");
    public static final PalladiumProperty<Float> REACH = new FloatProperty("reach").configurable("Reach in blocks.");

    public RapidAttackAbility() {
        this.withProperty(DAMAGE, 2F);
        this.withProperty(INTERVAL, 2);
        this.withProperty(REACH, 4F);
        this.withProperty(ENERGY_PER_TICK, 0.4F);
    }

    @Override
    protected boolean retriesWhileEnabled() {
        return false;
    }

    @Override
    public boolean onActivated(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        return true;
    }

    @Override
    protected void whileEnabled(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        if (player.tickCount % Math.max(1, instance.getProperty(INTERVAL)) != 0) {
            return;
        }
        LivingEntity target = Targeting.livingInSight(player, instance.getProperty(REACH));
        if (target != null && CombatService.dealAbilityDamage(player, target, instance.getProperty(DAMAGE), ModDamageTypes.SPEED_STRIKE, true)) {
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            player.serverLevel().sendParticles(ModParticles.SPEED_SPARK.get(), target.getX(), target.getY(0.6D), target.getZ(), 4, 0.3, 0.3, 0.3, 0.1);
        }
    }

    @Override
    public String getDocumentationDescription() {
        return "Powers NJ: rapid multi-hit attack (held).";
    }
}
