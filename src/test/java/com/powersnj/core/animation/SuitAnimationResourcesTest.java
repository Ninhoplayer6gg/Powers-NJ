package com.powersnj.core.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.powersnj.core.suit.ResourceRoot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every shipped suit model and animation set must be loadable by the runtime: sets reference
 * existing clips with the right loop modes, clips only animate bones of the suit rig, the GeckoLib
 * geometry has the armor bones with UVs inside the texture, and abilities only play known clips.
 */
class SuitAnimationResourcesTest {

    private static final List<String> ARMOR_BONES = List.of("armorHead", "armorBody", "armorRightArm", "armorLeftArm",
            "armorRightLeg", "armorRightBoot", "armorLeftLeg", "armorLeftBoot");

    private static final Path ASSETS = ResourceRoot.get().resolve("assets/powersnj");
    private static final Path DATA = ResourceRoot.get().resolve("data/powersnj");

    private static List<Path> sets() throws IOException {
        Path folder = ASSETS.resolve("animation_sets");
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
    }

    private static String suitOf(Path set) {
        String name = set.getFileName().toString();
        return name.substring(0, name.length() - ".json".length());
    }

    private static Path resource(String location) {
        String[] parts = location.split(":", 2);
        return ResourceRoot.get().resolve("assets/" + parts[0] + "/" + parts[1]);
    }

    private static AnimationSet load(Path set) throws IOException {
        String json = Files.readString(set);
        Path clips = resource(AnimationSetParser.clipFile(json));
        assertTrue(Files.exists(clips), set + " references missing clip file " + clips);
        return AnimationSetParser.parse(suitOf(set), json, AnimationClipParser.parse(Files.readString(clips)));
    }

    @Test
    void thraggShipsAModelAnimationsAndASet() {
        assertTrue(Files.exists(ASSETS.resolve("geo/suits/thragg.geo.json")));
        assertTrue(Files.exists(ASSETS.resolve("textures/suits/thragg/thragg_geo.png")));
        assertTrue(Files.exists(ASSETS.resolve("animations/suits/thragg.animation.json")));
        assertTrue(Files.exists(ASSETS.resolve("animation_sets/thragg.json")));
    }

    @Test
    void setsReferenceExistingClipsWithMatchingLoopModes() throws IOException {
        for (Path file : sets()) {
            AnimationSet set = load(file);
            assertEquals(List.of(), set.missingClips(), file + " references missing clips");
            for (Map.Entry<LocomotionState, String> state : set.states().entrySet()) {
                LoopMode loop = set.clip(state.getValue()).loop();
                LoopMode expected = switch (state.getKey()) {
                    case JUMP -> LoopMode.HOLD;
                    case TAKEOFF, LAND -> LoopMode.ONCE;
                    default -> LoopMode.LOOP;
                };
                assertEquals(expected, loop, file + ": " + state.getKey() + " clip " + state.getValue());
            }
            for (AnimationClip walkOrRun : new AnimationClip[]{set.walk(), set.run()}) {
                if (walkOrRun != null) {
                    assertEquals(LoopMode.LOOP, walkOrRun.loop(), walkOrRun.name());
                }
            }
            List<String> actions = new ArrayList<>(set.meleeChain());
            if (!set.meleeAfterSprint().isEmpty()) {
                actions.add(set.meleeAfterSprint());
            }
            actions.addAll(set.events().values());
            for (String action : actions) {
                assertEquals(LoopMode.ONCE, set.clip(action).loop(), file + ": one-shot clip " + action);
            }
        }
    }

    @Test
    void clipsOnlyAnimateBonesOfTheSuitGeometry() throws IOException {
        for (Path file : sets()) {
            AnimationSet set = load(file);
            Set<String> geoBones = geoBones(suitOf(file));
            for (AnimationClip clip : set.clips().values()) {
                assertTrue(clip.length() > 0, clip.name() + " has no length");
                assertTrue(clip.name().startsWith("animation." + suitOf(file) + "."), clip.name() + " must use the suit prefix");
                for (String bone : clip.bones().keySet()) {
                    assertTrue(HumanoidBones.isPlayerBone(bone) || geoBones.contains(bone),
                            clip.name() + " animates " + bone + ", which is neither a humanoid part nor a bone of the suit model");
                }
                for (Map.Entry<String, BoneTrack> track : clip.bones().entrySet()) {
                    for (Channel channel : new Channel[]{track.getValue().rotation(), track.getValue().position()}) {
                        if (channel != null) {
                            assertTrue(channel.lastTime() <= clip.length() + 1e-6, clip.name() + "/" + track.getKey() + " has keys after the end");
                        }
                    }
                }
            }
        }
    }

    @Test
    void geometryIsAGeckoLibArmorWithUvsInsideTheTexture() throws IOException {
        Path folder = ASSETS.resolve("geo/suits");
        if (!Files.isDirectory(folder)) {
            return;
        }
        try (Stream<Path> files = Files.list(folder)) {
            for (Path geo : files.filter(p -> p.toString().endsWith(".geo.json")).toList()) {
                String suit = geo.getFileName().toString().replace(".geo.json", "");
                JsonObject model = JsonParser.parseString(Files.readString(geo)).getAsJsonObject()
                        .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
                JsonObject description = model.getAsJsonObject("description");
                int width = description.get("texture_width").getAsInt();
                int height = description.get("texture_height").getAsInt();
                Path texture = ASSETS.resolve("textures/suits/" + suit + "/" + suit + "_geo.png");
                assertTrue(Files.exists(texture), "missing " + texture);
                byte[] png = Files.readAllBytes(texture);
                int pngWidth = ((png[16] & 0xFF) << 24) | ((png[17] & 0xFF) << 16) | ((png[18] & 0xFF) << 8) | (png[19] & 0xFF);
                int pngHeight = ((png[20] & 0xFF) << 24) | ((png[21] & 0xFF) << 16) | ((png[22] & 0xFF) << 8) | (png[23] & 0xFF);
                assertEquals(width * pngHeight, height * pngWidth, suit + ": texture aspect differs from the geometry UV size");

                Set<String> names = new HashSet<>();
                Set<String> parents = new HashSet<>();
                for (JsonElement element : model.getAsJsonArray("bones")) {
                    JsonObject bone = element.getAsJsonObject();
                    assertTrue(names.add(bone.get("name").getAsString()), suit + ": duplicated bone " + bone.get("name"));
                    if (bone.has("parent")) {
                        parents.add(bone.get("parent").getAsString());
                    }
                    if (bone.has("cubes")) {
                        for (JsonElement cube : bone.getAsJsonArray("cubes")) {
                            checkUv(suit, bone.get("name").getAsString(), cube.getAsJsonObject(), width, height);
                        }
                    }
                }
                assertTrue(names.containsAll(ARMOR_BONES), suit + " misses GeckoLib armor bones: " + ARMOR_BONES);
                assertTrue(names.containsAll(parents), suit + " has bones with unknown parents");
            }
        }
    }

    private static void checkUv(String suit, String bone, JsonObject cube, int width, int height) {
        if (!cube.get("uv").isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> face : cube.getAsJsonObject("uv").entrySet()) {
            JsonObject uv = face.getValue().getAsJsonObject();
            JsonArray start = uv.getAsJsonArray("uv");
            JsonArray size = uv.getAsJsonArray("uv_size");
            for (int axis = 0; axis < 2; axis++) {
                double a = start.get(axis).getAsDouble();
                double b = a + size.get(axis).getAsDouble();
                int limit = axis == 0 ? width : height;
                assertTrue(Math.min(a, b) >= -1e-6 && Math.max(a, b) <= limit + 1e-6,
                        suit + "/" + bone + " " + face.getKey() + " UV outside the texture: " + uv);
            }
        }
    }

    @Test
    void abilityAnimationsExistInTheSuitSet() throws IOException {
        for (Path file : sets()) {
            String suit = suitOf(file);
            Path power = DATA.resolve("palladium/powers/" + suit + ".json");
            if (!Files.exists(power)) {
                continue;
            }
            AnimationSet set = load(file);
            JsonObject abilities = JsonParser.parseString(Files.readString(power)).getAsJsonObject().getAsJsonObject("abilities");
            for (Map.Entry<String, JsonElement> ability : abilities.entrySet()) {
                JsonObject object = ability.getValue().getAsJsonObject();
                if (object.has("animation")) {
                    String clip = object.get("animation").getAsString();
                    assertNotNull(set.clip(clip), suit + "/" + ability.getKey() + " plays unknown clip " + clip);
                    assertEquals(LoopMode.ONCE, set.clip(clip).loop(), clip + " is played once by an ability");
                }
            }
        }
    }

    private static Set<String> geoBones(String suit) throws IOException {
        Path geo = ASSETS.resolve("geo/suits/" + suit + ".geo.json");
        assertTrue(Files.exists(geo), "animation set " + suit + " has no model " + geo);
        Set<String> names = new HashSet<>();
        JsonObject model = JsonParser.parseString(Files.readString(geo)).getAsJsonObject()
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        for (JsonElement bone : model.getAsJsonArray("bones")) {
            names.add(bone.getAsJsonObject().get("name").getAsString());
        }
        return names;
    }
}
