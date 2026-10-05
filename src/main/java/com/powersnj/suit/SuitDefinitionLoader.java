package com.powersnj.suit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.powersnj.PowersNJ;
import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.core.suit.SuitDefinitionParser;
import com.powersnj.core.suit.SuitDefinitions;
import com.powersnj.movement.MovementControllers;
import com.powersnj.network.PowersNetwork;
import com.powersnj.player.PowersPlayerData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads suit definitions from {@code data/<namespace>/powersnj/suits/*.json} on server start and
 * {@code /reload}, then pushes them to clients. Broken files are logged and skipped; they never take
 * down the server.
 */
@Mod.EventBusSubscriber(modid = PowersNJ.MOD_ID)
public final class SuitDefinitionLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public SuitDefinitionLoader() {
        super(GSON, "powersnj/suits");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, SuitDefinition> definitions = new LinkedHashMap<>();
        Map<String, String> raw = new LinkedHashMap<>();
        files.forEach((id, json) -> {
            try {
                SuitDefinition definition = SuitDefinitionParser.parse(id.toString(), json.getAsJsonObject());
                if (!MovementControllers.ids().contains(definition.movement())) {
                    PowersNJ.LOGGER.warn("Suit {} references unknown movement controller {}", id, definition.movement());
                }
                definitions.put(id.toString(), definition);
                raw.put(id.toString(), GSON.toJson(json));
            } catch (RuntimeException e) {
                PowersNJ.LOGGER.error("Invalid Powers NJ suit definition {}: {}", id, e.getMessage());
            }
        });
        SuitDefinitions.SERVER.replaceAll(definitions, raw);
        for (SuitKind kind : SuitKinds.all()) {
            if (!definitions.containsKey(kind.suitId())) {
                PowersNJ.LOGGER.warn("Registered suit {} has no definition (data/powersnj/powersnj/suits/{}.json); its power set stays inactive", kind.suitId(), kind.name());
            }
        }
        PowersNJ.LOGGER.info("Loaded {} Powers NJ suit definitions", definitions.size());
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new SuitDefinitionLoader());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        ServerPlayer target = event.getPlayer();
        if (target != null) {
            PowersNetwork.sendDefinitions(target);
            return;
        }
        PowersNetwork.broadcastDefinitions();
        for (ServerPlayer player : event.getPlayerList().getPlayers()) {
            PowersPlayerData.get(player).ifPresent(data -> {
                // Definitions may have changed (energy caps, controllers): rebuild the active profile.
                data.setActiveSuit("__reload__");
                data.requestFullSync();
            });
        }
    }
}
