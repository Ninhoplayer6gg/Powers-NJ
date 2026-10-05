package com.powersnj.compat.palladium.ability;

import com.powersnj.combat.CombatService;
import com.powersnj.combat.Targeting;
import com.powersnj.core.destruction.DestructionTier;
import com.powersnj.destruction.DestructionEngine;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.power.ServerTasks;
import com.powersnj.registry.ModDamageTypes;
import com.powersnj.registry.ModParticles;
import com.powersnj.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * {@code powersnj:charge} - rams forward for a few ticks (works on ground and in flight), hitting
 * everything on the way and tunnelling through weak blocks.
 */
public class ChargeAbility extends PowersActionAbility {

    public static final PalladiumProperty<Integer> DURATION = new IntegerProperty("duration").configurable("Charge duration in ticks.");
    public static final PalladiumProperty<Float> SPEED = new FloatProperty("speed").configurable("Speed in blocks per tick.");
    public static final PalladiumProperty<Float> DAMAGE = new FloatProperty("damage").configurable("Damage per hit (PvE value).");

    public ChargeAbility() {
        this.withProperty(DURATION, 12);
        this.withProperty(SPEED, 1.6F);
        this.withProperty(DAMAGE, 12F);
        this.withProperty(DESTRUCTION_POWER, "normal");
    }

    @Override
    public boolean onActivated(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        ServerLevel level = player.serverLevel();
        Vec3 direction = player.getLookAngle().normalize();
        int duration = instance.getProperty(DURATION);
        float speed = instance.getProperty(SPEED);
        float damage = instance.getProperty(DAMAGE);
        DestructionTier power = destructionPower(instance);
        UUID ownerId = player.getUUID();
        Set<Integer> hit = new HashSet<>();
        level.playSound(null, player.blockPosition(), ModSounds.FLIGHT_BOOST.get(), SoundSource.PLAYERS, 1.2F, 0.8F);
        ServerTasks.schedule(level, (lvl, age) -> {
            if (!(lvl.getPlayerByUUID(ownerId) instanceof ServerPlayer owner) || !owner.isAlive() || age >= duration) {
                return true;
            }
            CombatService.launch(owner, direction.scale(speed));
            owner.fallDistance = 0F;
            for (LivingEntity target : lvl.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(1.0D), e -> e != owner && !hit.contains(e.getId()))) {
                hit.add(target.getId());
                if (CombatService.dealAbilityDamage(owner, target, damage, ModDamageTypes.SUPER_STRENGTH, false)) {
                    CombatService.launch(target, target.getDeltaMovement().add(direction.scale(speed * 1.5D)).add(0, 0.4D, 0));
                }
            }
            if (power != null && age % 3 == 0) {
                Vec3 h = Targeting.horizontalLook(owner);
                DestructionEngine.tunnel(lvl, owner, BlockPos.containing(owner.position()), h.x, h.z, 2, 3, 3, power);
            }
            lvl.sendParticles(ModParticles.VILTRUMITE_IMPACT.get(), owner.getX(), owner.getY(0.5D), owner.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
            return false;
        });
        return true;
    }

    @Override
    public String getDocumentationDescription() {
        return "Powers NJ: forward charge that rams entities and tunnels through blocks.";
    }
}
