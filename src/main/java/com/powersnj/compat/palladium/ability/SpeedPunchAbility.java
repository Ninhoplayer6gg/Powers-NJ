package com.powersnj.compat.palladium.ability;

import com.powersnj.combat.CombatService;
import com.powersnj.combat.Targeting;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.registry.ModDamageTypes;
import com.powersnj.registry.ModParticles;
import com.powersnj.speedster.SpeedsterMovementController;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

/**
 * {@code powersnj:speed_punch} - punch whose damage scales with the current speed multiplier.
 */
public class SpeedPunchAbility extends PowersActionAbility {

    public static final PalladiumProperty<Float> DAMAGE = new FloatProperty("damage").configurable("Base damage (PvE value).");
    public static final PalladiumProperty<Float> DAMAGE_PER_SPEED = new FloatProperty("damage_per_speed").configurable("Extra damage per speed multiplier point.");
    public static final PalladiumProperty<Float> REACH = new FloatProperty("reach").configurable("Reach in blocks.");

    public SpeedPunchAbility() {
        this.withProperty(DAMAGE, 6F);
        this.withProperty(DAMAGE_PER_SPEED, 1.5F);
        this.withProperty(REACH, 4.5F);
    }

    @Override
    public boolean onActivated(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        LivingEntity target = Targeting.livingInSight(player, instance.getProperty(REACH));
        if (target == null) {
            return false;
        }
        double speed = data.movement() instanceof SpeedsterMovementController s ? s.engine().currentMultiplier() : 1D;
        float damage = (float) (instance.getProperty(DAMAGE) + instance.getProperty(DAMAGE_PER_SPEED) * Math.max(0D, speed - 1D));
        if (!CombatService.dealAbilityDamage(player, target, damage, ModDamageTypes.SPEED_STRIKE, true)) {
            return false;
        }
        Vec3 push = Targeting.horizontalLook(player).scale(0.6D + speed * 0.15D);
        CombatService.launch(target, target.getDeltaMovement().add(push).add(0, 0.3D, 0));
        player.serverLevel().sendParticles(ModParticles.NEGATIVE_LIGHTNING.get(), target.getX(), target.getY(0.6D), target.getZ(), 8, 0.2, 0.3, 0.2, 0.1);
        return true;
    }

    @Override
    public String getDocumentationDescription() {
        return "Powers NJ: speed-scaled punch.";
    }
}
