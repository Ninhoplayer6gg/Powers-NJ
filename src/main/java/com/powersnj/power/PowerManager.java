package com.powersnj.power;

import com.powersnj.PowersNJ;
import com.powersnj.core.config.PowersSettings;
import com.powersnj.core.config.Settings;
import com.powersnj.core.energy.EnergyPool;
import com.powersnj.core.net.CooldownSnapshot;
import com.powersnj.core.net.EnergySnapshot;
import com.powersnj.core.net.MovementSnapshot;
import com.powersnj.core.net.PowerStateSnapshot;
import com.powersnj.core.net.ProgressSnapshot;
import com.powersnj.core.progression.SuitProgress;
import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.core.suit.SuitDefinitions;
import com.powersnj.movement.MovementController;
import com.powersnj.movement.MovementControllers;
import com.powersnj.network.CooldownPacket;
import com.powersnj.network.EnergyPacket;
import com.powersnj.network.FormPacket;
import com.powersnj.network.MovementPacket;
import com.powersnj.network.PowerStatePacket;
import com.powersnj.network.PowersNetwork;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.progression.ProgressionAPI;
import com.powersnj.registry.ModSounds;
import com.powersnj.suit.SuitDetector;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Server heartbeat of the power framework: per player and per tick it
 * <ol>
 *     <li>detects the worn suit and activates/deactivates its Power Profile,</li>
 *     <li>regenerates energy (server {@code energyRegenerationMultiplier}) and counts cooldowns down,</li>
 *     <li>ticks the movement controller and special systems of the active suit,</li>
 *     <li>synchronises only what changed (see {@link PowersNetwork} bandwidth rules).</li>
 * </ol>
 * Palladium abilities tick in between (living tick), which is why traits are cleared at the start
 * of the player tick and consumed at the end.
 */
@Mod.EventBusSubscriber(modid = PowersNJ.MOD_ID)
public final class PowerManager {

    private PowerManager() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        PowersPlayerData data = PowersPlayerData.get(player).orElse(null);
        if (data == null) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            preTick(player, data);
        } else {
            tick(player, data);
        }
    }

    /**
     * Start of the player tick, before Palladium ticks abilities: refresh the active suit and clear
     * the per-tick traits that enabled trait abilities will fill again.
     */
    public static void preTick(ServerPlayer player, PowersPlayerData data) {
        data.traits().clear();
        String worn = SuitDetector.activeSuitId(player, false);
        if (!worn.equals(data.activeSuit())) {
            switchSuit(player, data, worn);
        }
    }

    public static void tick(ServerPlayer player, PowersPlayerData data) {
        PowersSettings settings = Settings.get();
        for (EnergyPool pool : data.energy().pools()) {
            pool.tick(settings.energyRegenerationMultiplier());
        }
        data.cooldowns().tick();

        SuitDefinition definition = data.activeDefinition(false).orElse(null);
        if (definition != null) {
            MovementController movement = data.movement();
            if (movement != null) {
                movement.tick(player, data, definition);
            }
            PowerSystems.tick(player, data, definition);
        }
        sync(player, data);
    }

    private static void switchSuit(ServerPlayer player, PowersPlayerData data, String newSuit) {
        deactivate(player, data);
        data.setActiveSuit(newSuit);
        SuitDefinition definition = SuitDefinitions.SERVER.get(newSuit).orElse(null);
        if (definition != null) {
            SuitProgress progress = data.ledger().progress(newSuit);
            data.energy().configure(definition.energy(), progress.level());
            MovementController movement = MovementControllers.create(definition);
            movement.load(data.movementState(movement.id()));
            movement.activate(player, data);
            data.setMovement(movement);
            player.level().playSound(null, player.blockPosition(), ModSounds.SUIT_EQUIP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.powersnj.suit.activated",
                    Component.translatable("suit." + newSuit.replace(':', '.')), progress.level()).withStyle(ChatFormatting.GOLD), true);
            PowersNJ.LOGGER.debug("{} activated suit {}", player.getGameProfile().getName(), newSuit);
        }
        data.requestFullSync();
    }

    /**
     * Removes every runtime effect of the active suit (attribute modifiers, movement state).
     */
    public static void deactivate(ServerPlayer player) {
        PowersPlayerData.get(player).ifPresent(data -> deactivate(player, data));
    }

    private static void deactivate(ServerPlayer player, PowersPlayerData data) {
        MovementController movement = data.movement();
        if (movement != null) {
            movement.deactivate(player, data);
            data.storeMovementState(movement.id(), movement.save());
            data.setMovement(null);
        }
        data.setShieldActive(false);
        data.setForm("");
        data.activeAbilities().clear();
        data.setActiveTendril(-1);
        data.setActiveSuit("");
    }

    private static void sync(ServerPlayer player, PowersPlayerData data) {
        if (data.consumeFullSyncRequest()) {
            sendFullState(player, data);
            return;
        }
        List<EnergySnapshot> energies = new ArrayList<>();
        for (EnergyPool pool : data.energy().pools()) {
            if (pool.needsSync()) {
                energies.add(EnergySnapshot.of(pool));
                pool.markSynced();
            }
        }
        if (!energies.isEmpty()) {
            PowersNetwork.sendTo(player, new EnergyPacket(energies));
        }

        Set<String> dirtyCooldowns = data.cooldowns().drainDirty();
        if (!dirtyCooldowns.isEmpty()) {
            List<CooldownSnapshot> cooldowns = new ArrayList<>(dirtyCooldowns.size());
            for (String key : dirtyCooldowns) {
                cooldowns.add(new CooldownSnapshot(key, data.cooldowns().remaining(key), data.cooldowns().total(key)));
            }
            PowersNetwork.sendTo(player, new CooldownPacket(cooldowns));
        }

        data.activeProgress().filter(SuitProgress::isDirty).ifPresent(progress ->
                ProgressionAPI.syncProgress(player, data, progress.suitId(), false));

        syncMovement(player, data, false);
        if (data.consumeFormDirty()) {
            PowersNetwork.sendToTrackingAndSelf(player, new FormPacket(player.getId(), data.form()));
        }
    }

    private static void syncMovement(ServerPlayer player, PowersPlayerData data, boolean force) {
        MovementController movement = data.movement();
        MovementSnapshot snapshot = movement == null ? new MovementSnapshot(player.getId(), MovementControllers.NONE, "NONE", 0F, 0, 0) : movement.snapshot(player);
        if (force || snapshot.differsSignificantly(data.lastSentMovement())) {
            data.setLastSentMovement(snapshot);
            PowersNetwork.sendToTrackingAndSelf(player, new MovementPacket(snapshot));
        }
    }

    public static void sendFullState(ServerPlayer player, PowersPlayerData data) {
        List<EnergySnapshot> energies = new ArrayList<>();
        for (EnergyPool pool : data.energy().pools()) {
            energies.add(EnergySnapshot.of(pool));
            pool.markSynced();
        }
        data.cooldowns().drainDirty();
        List<CooldownSnapshot> cooldowns = new ArrayList<>();
        for (String key : data.cooldowns().activeKeys()) {
            cooldowns.add(new CooldownSnapshot(key, data.cooldowns().remaining(key), data.cooldowns().total(key)));
        }
        ProgressSnapshot progress = null;
        if (data.hasActiveSuit()) {
            SuitProgress p = data.ledger().progress(data.activeSuit());
            progress = ProgressSnapshot.of(p, SuitDefinitions.SERVER.curveOf(data.activeSuit()));
            p.markClean();
        }
        PowersNetwork.sendTo(player, new PowerStatePacket(new PowerStateSnapshot(data.activeSuit(), progress, energies, cooldowns, data.form())));
        syncMovement(player, data, true);
        data.consumeFormDirty();
        PowersNetwork.sendToTrackingAndSelf(player, new FormPacket(player.getId(), data.form()));
    }

    /**
     * Players starting to track another player get its movement state and form immediately.
     */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer watcher) {
            PowersPlayerData.get(target).ifPresent(data -> {
                if (data.lastSentMovement() != null) {
                    PowersNetwork.sendTo(watcher, new MovementPacket(data.lastSentMovement()));
                }
                PowersNetwork.sendTo(watcher, new FormPacket(target.getId(), data.form()));
            });
        }
    }
}
