package com.powersnj.compat.palladium.ability;

import com.powersnj.combat.CombatService;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.power.ServerTasks;
import com.powersnj.registry.ModDamageTypes;
import com.powersnj.registry.ModParticles;
import com.powersnj.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * {@code powersnj:shockwave} - a ring that expands along the ground over several ticks, hitting each
 * entity once (multi-tick server task, no client trust).
 */
public class ShockwaveAbility extends PowersActionAbility {

    public static final PalladiumProperty<Float> RADIUS = new FloatProperty("radius").configurable("Final ring radius.");
    public static final PalladiumProperty<Float> SPEED = new FloatProperty("speed").configurable("Ring growth in blocks per tick.");
    public static final PalladiumProperty<Float> DAMAGE = new FloatProperty("damage").configurable("Damage (PvE value).");
    public static final PalladiumProperty<Float> KNOCKBACK = new FloatProperty("knockback").configurable("Knockback strength.");

    public ShockwaveAbility() {
        this.withProperty(RADIUS, 12F);
        this.withProperty(SPEED, 0.8F);
        this.withProperty(DAMAGE, 8F);
        this.withProperty(KNOCKBACK, 1.2F);
    }

    @Override
    public boolean onActivated(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        ServerLevel level = player.serverLevel();
        Vec3 origin = player.position();
        float maxRadius = instance.getProperty(RADIUS);
        float speed = Math.max(0.1F, instance.getProperty(SPEED));
        float damage = instance.getProperty(DAMAGE);
        float knockback = instance.getProperty(KNOCKBACK);
        UUID ownerId = player.getUUID();
        Set<Integer> hit = new HashSet<>();
        level.playSound(null, player.blockPosition(), ModSounds.SHOCKWAVE.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
        ServerTasks.schedule(level, (lvl, age) -> {
            double radius = (age + 1) * speed;
            if (!(lvl.getPlayerByUUID(ownerId) instanceof ServerPlayer owner)) {
                return true;
            }
            for (int i = 0; i < 24; i++) {
                double angle = Math.PI * 2 * i / 24;
                lvl.sendParticles(ModParticles.SHOCKWAVE_DUST.get(), origin.x + Math.cos(angle) * radius, origin.y + 0.1D, origin.z + Math.sin(angle) * radius, 1, 0, 0.05, 0, 0);
            }
            AABB box = new AABB(origin, origin).inflate(radius, 2, radius);
            for (LivingEntity target : lvl.getEntitiesOfClass(LivingEntity.class, box, e -> e != owner && !hit.contains(e.getId()))) {
                double d = Math.sqrt(target.distanceToSqr(origin.x, target.getY(), origin.z));
                if (d <= radius && d >= radius - speed - 0.5D) {
                    hit.add(target.getId());
                    if (CombatService.dealAbilityDamage(owner, target, damage, ModDamageTypes.SHOCKWAVE, false)) {
                        CombatService.knockbackFrom(target, origin, knockback, 0.4D);
                    }
                }
            }
            return radius >= maxRadius;
        });
        return true;
    }

    @Override
    public String getDocumentationDescription() {
        return "Powers NJ: expanding ground shockwave.";
    }
}
