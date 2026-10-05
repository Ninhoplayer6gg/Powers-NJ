package com.powersnj.render;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client cache answering "does this asset exist in the loaded resource packs?". Renderers use it to
 * switch from placeholders to final art/models automatically as soon as the asset files from
 * ASSET_REQUIREMENTS.md are added. Cleared on every resource reload (F3+T).
 */
public final class AssetAvailability implements ResourceManagerReloadListener {

    public static final AssetAvailability INSTANCE = new AssetAvailability();
    private static final Map<ResourceLocation, Boolean> CACHE = new ConcurrentHashMap<>();

    private AssetAvailability() {
    }

    public static boolean has(ResourceLocation location) {
        return CACHE.computeIfAbsent(location, loc -> Minecraft.getInstance().getResourceManager().getResource(loc).isPresent());
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        CACHE.clear();
    }
}
