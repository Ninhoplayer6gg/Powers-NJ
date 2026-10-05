package com.powersnj.player;

import com.powersnj.PowersNJ;
import com.powersnj.power.PowerManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Lifecycle of the player capability: attach, copy on respawn / dimension change, full sync on
 * login. Suits themselves are normal armor: on death they follow the vanilla {@code keepInventory}
 * gamerule (kept when true, dropped when false) - nothing here interferes with that.
 */
@Mod.EventBusSubscriber(modid = PowersNJ.MOD_ID)
public final class PlayerDataEvents {

    private PlayerDataEvents() {
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            PowersPlayerDataProvider provider = new PowersPlayerDataProvider();
            event.addCapability(PowersPlayerDataProvider.ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        PowersPlayerData.get(original).ifPresent(old ->
                PowersPlayerData.get(event.getEntity()).ifPresent(fresh -> fresh.copyFrom(old, event.isWasDeath())));
        original.invalidateCaps();
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        // Suit definitions are sent by SuitDefinitionLoader on OnDatapackSyncEvent (fired at login too).
        if (event.getEntity() instanceof ServerPlayer player) {
            PowersPlayerData.get(player).ifPresent(PowersPlayerData::requestFullSync);
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PowersPlayerData.get(player).ifPresent(PowersPlayerData::requestFullSync);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PowersPlayerData.get(player).ifPresent(PowersPlayerData::requestFullSync);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PowerManager.deactivate(player);
        }
    }

    /**
     * Mod bus: capability registration.
     */
    @Mod.EventBusSubscriber(modid = PowersNJ.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {

        private ModBus() {
        }

        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            event.register(PowersPlayerData.class);
        }
    }
}
