package com.powersnj.compat.palladium.ability;

import com.powersnj.combat.CombatService;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.registry.ModParticles;
import com.powersnj.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

/**
 * {@code powersnj:speed_dash} - instant burst in the look direction with a few invulnerable ticks.
 */
public class SpeedDashAbility extends PowersActionAbility {

    public static final PalladiumProperty<Float> STRENGTH = new FloatProperty("strength").configurable("Dash velocity in blocks per tick.");

    public SpeedDashAbility() {
        this.withProperty(STRENGTH, 2.5F);
    }

    @Override
    public boolean onActivated(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        Vec3 look = player.getLookAngle();
        Vec3 dash = new Vec3(look.x, Math.max(0.1D, look.y * 0.5D), look.z).normalize().scale(instance.getProperty(STRENGTH));
        player.serverLevel().sendParticles(ModParticles.SPEED_SPARK.get(), player.getX(), player.getY(1.0D), player.getZ(), 15, 0.3, 0.5, 0.3, 0.1);
        CombatService.launch(player, dash);
        player.invulnerableTime = Math.max(player.invulnerableTime, 10);
        player.fallDistance = 0F;
        player.level().playSound(null, player.blockPosition(), ModSounds.SPEED_DASH.get(), SoundSource.PLAYERS, 1.0F, 1.2F);
        return true;
    }

    @Override
    public String getDocumentationDescription() {
        return "Powers NJ: speedster dash.";
    }
}
