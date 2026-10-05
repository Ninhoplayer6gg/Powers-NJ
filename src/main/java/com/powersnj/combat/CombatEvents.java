package com.powersnj.combat;

import com.powersnj.PowersNJ;
import com.powersnj.core.combat.CombatRules;
import com.powersnj.core.combat.TargetKind;
import com.powersnj.core.config.PowersSettings;
import com.powersnj.core.config.Settings;
import com.powersnj.core.energy.EnergyPool;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Melee balancing and defensive powers.
 * <ul>
 *     <li>Melee hits from a player wearing an active suit are balanced like ability damage
 *     (PvP / PvE / boss multipliers; cancelled against players when PvP is disabled).
 *     Ability damage uses Powers NJ damage types and is balanced at the source.</li>
 *     <li>Symbiote shield: absorbs part of incoming damage by spending biomass.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = PowersNJ.MOD_ID)
public final class CombatEvents {

    /** Fraction of damage the symbiote shield can absorb. */
    public static final float SHIELD_ABSORPTION = 0.7F;
    /** Biomass spent per absorbed damage point. */
    public static final float SHIELD_COST_PER_DAMAGE = 2.0F;

    private CombatEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void balanceSuitedMelee(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer attacker) || source.getDirectEntity() != attacker || !source.is(DamageTypes.PLAYER_ATTACK)) {
            return;
        }
        PowersPlayerData data = PowersPlayerData.get(attacker).orElse(null);
        if (data == null || !data.hasActiveSuit()) {
            return;
        }
        PowersSettings settings = Settings.get();
        TargetKind kind = CombatService.kindOf(event.getEntity());
        if (!CombatRules.canHit(kind, true, settings)) {
            event.setCanceled(true);
            return;
        }
        event.setAmount(CombatRules.scaleDamage(event.getAmount(), kind, true, settings));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void symbioteShield(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0) {
            return;
        }
        PowersPlayerData data = PowersPlayerData.get(player).orElse(null);
        if (data == null || !data.isShieldActive()) {
            return;
        }
        EnergyPool biomass = data.activeEnergy(false).orElse(null);
        if (biomass == null) {
            return;
        }
        float absorbable = event.getAmount() * SHIELD_ABSORPTION;
        float affordable = biomass.current() / SHIELD_COST_PER_DAMAGE;
        float absorbed = Math.min(absorbable, affordable);
        if (absorbed <= 0) {
            return;
        }
        biomass.drain(absorbed * SHIELD_COST_PER_DAMAGE);
        event.setAmount(event.getAmount() - absorbed);
        player.level().playSound(null, player.blockPosition(), ModSounds.SYMBIOTE_SHIELD.get(), SoundSource.PLAYERS, 0.6F, 1.2F);
    }
}
