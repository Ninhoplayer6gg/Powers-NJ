package com.powersnj.animation;

import com.powersnj.PowersNJ;
import com.powersnj.core.animation.AnimationClip;
import com.powersnj.core.animation.AnimationClipParser;
import com.powersnj.core.animation.AnimationSet;
import com.powersnj.core.animation.AnimationSetParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client resource listener: loads every {@code assets/<ns>/animation_sets/<suit>.json} with the clip
 * file it references. Broken files are logged and skipped (the suit then uses vanilla poses);
 * resource packs can replace both files, F3+T reloads them.
 */
public final class SuitAnimationLibrary extends SimplePreparableReloadListener<Map<String, AnimationSet>> {

    public static final SuitAnimationLibrary INSTANCE = new SuitAnimationLibrary();
    private static final String FOLDER = "animation_sets";

    private volatile Map<String, AnimationSet> sets = Map.of();

    private SuitAnimationLibrary() {
    }

    /**
     * Set of a suit ({@code SuitKind#name()}), or null.
     */
    public AnimationSet get(String suit) {
        return suit == null ? null : this.sets.get(suit);
    }

    public Map<String, AnimationSet> all() {
        return this.sets;
    }

    @Override
    protected Map<String, AnimationSet> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<String, AnimationSet> result = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources(FOLDER, path -> path.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation file = entry.getKey();
            String id = file.getPath().substring(FOLDER.length() + 1, file.getPath().length() - ".json".length());
            try {
                String json = read(entry.getValue());
                ResourceLocation clipFile = ResourceLocation.tryParse(AnimationSetParser.clipFile(json));
                if (clipFile == null) {
                    PowersNJ.LOGGER.error("Animation set {} references an invalid clip file", file);
                    continue;
                }
                Resource clips = manager.getResource(clipFile).orElse(null);
                if (clips == null) {
                    PowersNJ.LOGGER.error("Animation set {}: clip file {} not found", file, clipFile);
                    continue;
                }
                Map<String, AnimationClip> parsed = AnimationClipParser.parse(read(clips));
                AnimationSet set = AnimationSetParser.parse(id, json, parsed);
                List<String> missing = set.missingClips();
                if (!missing.isEmpty()) {
                    PowersNJ.LOGGER.warn("Animation set {} references missing clips {}", file, missing);
                }
                result.put(id, set);
            } catch (IOException | RuntimeException e) {
                PowersNJ.LOGGER.error("Could not load animation set {}: {}", file, e.getMessage());
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<String, AnimationSet> prepared, ResourceManager manager, ProfilerFiller profiler) {
        this.sets = Collections.unmodifiableMap(prepared);
        SuitAnimationClient.clear();
        PowersNJ.LOGGER.debug("Loaded {} suit animation set(s)", prepared.size());
    }

    private static String read(Resource resource) throws IOException {
        try (Reader reader = resource.openAsReader()) {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[8192];
            int n;
            while ((n = reader.read(buffer)) > 0) {
                builder.append(buffer, 0, n);
            }
            return builder.toString();
        }
    }
}
