package com.powersnj.symbiote;

import com.powersnj.PowersNJ;
import com.powersnj.core.config.Settings;
import com.powersnj.core.energy.EnergyPool;
import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.registry.ModEffects;
import com.powersnj.registry.ModParticles;
import com.powersnj.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.NoteBlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Symbiote special systems (enabled per suit through the definition's {@code "systems"} list):
 * <ul>
 *     <li>{@code powersnj:venom_weaknesses} - heat (fire, lava, explosions) and sonic (sonic booms,
 *     bells, note blocks, explosions nearby) deal extra damage, drain biomass and destabilise the
 *     symbiote ({@code powersnj:symbiote_destabilized} blocks symbiote abilities). Server config
 *     {@code enableVenomWeaknesses}.</li>
 *     <li>{@code powersnj:biomass_feeding} - kills and food restore biomass.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = PowersNJ.MOD_ID)
public final class SymbioteEvents {

    public static final String SYSTEM_WEAKNESSES = "powersnj:venom_weaknesses";
    public static final String SYSTEM_FEEDING = "powersnj:biomass_feeding";

    private SymbioteEvents() {
    }

    private static SuitDefinition definitionWith(ServerPlayer player, PowersPlayerData data, String system) {
        SuitDefinition definition = data.activeDefinition(false).orElse(null);
        return definition != null && definition.hasSystem(system) ? definition : null;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void weaknessDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !Settings.get().enableVenomWeaknesses()) {
            return;
        }
        PowersPlayerData data = PowersPlayerData.get(player).orElse(null);
        SuitDefinition definition = data == null ? null : definitionWith(player, data, SYSTEM_WEAKNESSES);
        if (definition == null) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(DamageTypes.SONIC_BOOM)) {
            event.setAmount(event.getAmount() * (float) definition.setting("sonic_damage_multiplier", 2.0D));
            destabilize(player, data, definition, (int) definition.setting("sonic_destabilize_ticks", 100));
        } else if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_LIGHTNING)) {
            event.setAmount(event.getAmount() * (float) definition.setting("heat_damage_multiplier", 1.75D));
            destabilize(player, data, definition, (int) definition.setting("heat_destabilize_ticks", 60));
        }
    }

    @SubscribeEvent
    public static void loudSound(PlayLevelSoundEvent.AtPosition event) {
        Level level = event.getLevel();
        if (level.isClientSide || event.getSound() == null || !Settings.get().enableVenomWeaknesses()) {
            return;
        }
        SoundEvent sound = event.getSound().value();
        double radius;
        if (sound == SoundEvents.BELL_BLOCK || sound == SoundEvents.WARDEN_SONIC_BOOM) {
            radius = 10D;
        } else if (sound == SoundEvents.GENERIC_EXPLODE) {
            radius = 16D;
        } else {
            return;
        }
        sonicPulse((ServerLevel) level, event.getPosition(), radius);
    }

    @SubscribeEvent
    public static void noteBlock(NoteBlockEvent.Play event) {
        if (event.getLevel() instanceof ServerLevel level && Settings.get().enableVenomWeaknesses()) {
            sonicPulse(level, Vec3.atCenterOf(event.getPos()), 6D);
        }
    }

    private static void sonicPulse(ServerLevel level, Vec3 origin, double radius) {
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(origin, origin).inflate(radius))) {
            PowersPlayerData data = PowersPlayerData.get(player).orElse(null);
            SuitDefinition definition = data == null ? null : definitionWith(player, data, SYSTEM_WEAKNESSES);
            if (definition != null && player.position().distanceTo(origin) <= radius) {
                destabilize(player, data, definition, (int) definition.setting("sonic_destabilize_ticks", 100) / 2);
            }
        }
    }

    public static void destabilize(ServerPlayer player, PowersPlayerData data, SuitDefinition definition, int ticks) {
        boolean wasStable = !player.hasEffect(ModEffects.SYMBIOTE_DESTABILIZED.get());
        player.addEffect(new MobEffectInstance(ModEffects.SYMBIOTE_DESTABILIZED.get(), Math.max(20, ticks), 0, false, true, true));
        data.activeEnergy(false).ifPresent(pool -> pool.drain((float) definition.setting("weakness_biomass_drain", 12D)));
        data.setShieldActive(false);
        data.setForm("");
        if (wasStable) {
            player.level().playSound(null, player.blockPosition(), ModSounds.SYMBIOTE_HISS.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.serverLevel().sendParticles(ModParticles.SYMBIOTE_GOO.get(), player.getX(), player.getY(1.0D), player.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
            player.displayClientMessage(Component.translatable("message.powersnj.symbiote.destabilized").withStyle(ChatFormatting.DARK_AQUA), true);
        }
    }

    public static boolean isDestabilized(LivingEntity entity) {
        return entity.hasEffect(ModEffects.SYMBIOTE_DESTABILIZED.get());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void feedOnKill(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PowersPlayerData data = PowersPlayerData.get(player).orElse(null);
        SuitDefinition definition = data == null ? null : definitionWith(player, data, SYSTEM_FEEDING);
        if (definition != null) {
            float amount = (float) (definition.setting("biomass_per_kill", 12D) * Math.max(0.5D, event.getEntity().getMaxHealth() / 20D));
            data.activeEnergy(false).ifPresent(pool -> pool.add(amount));
        }
    }

    @SubscribeEvent
    public static void feedOnFood(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        FoodProperties food = event.getItem().getFoodProperties(player);
        if (food == null) {
            return;
        }
        PowersPlayerData data = PowersPlayerData.get(player).orElse(null);
        SuitDefinition definition = data == null ? null : definitionWith(player, data, SYSTEM_FEEDING);
        if (definition != null) {
            EnergyPool pool = data.activeEnergy(false).orElse(null);
            if (pool != null) {
                pool.add((float) (food.getNutrition() * definition.setting("biomass_per_food_point", 2.5D)));
            }
        }
    }
}
