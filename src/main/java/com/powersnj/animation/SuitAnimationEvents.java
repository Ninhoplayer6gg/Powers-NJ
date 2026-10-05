package com.powersnj.animation;

import com.powersnj.PowersNJ;
import com.powersnj.combat.CombatService;
import com.powersnj.core.combat.TargetKind;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.progression.SuitProgressEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Server events that play suit animations: victory on level up and on killing a player or a boss,
 * recoil when a hit takes at least {@link #HEAVY_HIT_DAMAGE} health.
 */
@Mod.EventBusSubscriber(modid = PowersNJ.MOD_ID)
public final class SuitAnimationEvents {

    public static final float HEAVY_HIT_DAMAGE = 8F;
    private static final int HEAVY_HIT_COOLDOWN = 15;
    private static final Map<ServerPlayer, Integer> LAST_HEAVY_HIT = new WeakHashMap<>();

    private SuitAnimationEvents() {
    }

    private static boolean suited(ServerPlayer player) {
        return PowersPlayerData.get(player).map(PowersPlayerData::hasActiveSuit).orElse(false);
    }

    @SubscribeEvent
    public static void levelUp(SuitProgressEvent.LevelUp event) {
        SuitAnimations.play(event.getPlayer(), SuitAnimations.EVENT_LEVEL_UP);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void kill(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getSource().getEntity() instanceof ServerPlayer player) || event.getEntity() == player || !suited(player)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        TargetKind kind = CombatService.kindOf(victim);
        if (kind == TargetKind.PLAYER || kind == TargetKind.BOSS) {
            SuitAnimations.play(player, SuitAnimations.EVENT_KILL);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void heavyHit(LivingDamageEvent event) {
        if (event.isCanceled() || event.getAmount() < HEAVY_HIT_DAMAGE || !(event.getEntity() instanceof ServerPlayer player) || !suited(player)) {
            return;
        }
        Integer last = LAST_HEAVY_HIT.get(player);
        if (last != null && player.tickCount - last >= 0 && player.tickCount - last < HEAVY_HIT_COOLDOWN) {
            return;
        }
        LAST_HEAVY_HIT.put(player, player.tickCount);
        SuitAnimations.play(player, SuitAnimations.EVENT_HEAVY_HIT);
    }
}
