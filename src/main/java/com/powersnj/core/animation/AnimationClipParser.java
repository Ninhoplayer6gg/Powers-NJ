package com.powersnj.core.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses Bedrock animation files ({@code .animation.json}, the format exported by Blockbench and read
 * by GeckoLib). Only constant values are supported (no Molang): suits animate from keyframes only.
 */
public final class AnimationClipParser {

    private AnimationClipParser() {
    }

    public static Map<String, AnimationClip> parse(String json) {
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (RuntimeException e) {
            throw new JsonParseException("Invalid animation JSON: " + e.getMessage(), e);
        }
        if (!root.isJsonObject() || !root.getAsJsonObject().has("animations")) {
            throw new JsonParseException("Animation file has no \"animations\" object");
        }
        Map<String, AnimationClip> clips = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().getAsJsonObject("animations").entrySet()) {
            clips.put(entry.getKey(), parseClip(entry.getKey(), entry.getValue().getAsJsonObject()));
        }
        return clips;
    }

    static AnimationClip parseClip(String name, JsonObject object) {
        LoopMode loop = LoopMode.ONCE;
        if (object.has("loop")) {
            JsonPrimitive value = object.getAsJsonPrimitive("loop");
            if (value.isBoolean()) {
                loop = value.getAsBoolean() ? LoopMode.LOOP : LoopMode.ONCE;
            } else if ("hold_on_last_frame".equals(value.getAsString())) {
                loop = LoopMode.HOLD;
            } else if ("true".equals(value.getAsString())) {
                loop = LoopMode.LOOP;
            }
        }
        Map<String, BoneTrack> bones = new LinkedHashMap<>();
        double lastKey = 0D;
        JsonObject boneObjects = object.has("bones") ? object.getAsJsonObject("bones") : new JsonObject();
        for (Map.Entry<String, JsonElement> bone : boneObjects.entrySet()) {
            JsonObject channels = bone.getValue().getAsJsonObject();
            Channel rotation = channels.has("rotation") ? parseChannel(name, bone.getKey(), channels.get("rotation")) : null;
            Channel position = channels.has("position") ? parseChannel(name, bone.getKey(), channels.get("position")) : null;
            if (rotation == null && position == null) {
                continue;
            }
            lastKey = Math.max(lastKey, Math.max(rotation == null ? 0D : rotation.lastTime(), position == null ? 0D : position.lastTime()));
            bones.put(bone.getKey(), new BoneTrack(rotation, position));
        }
        double length = object.has("animation_length") ? object.get("animation_length").getAsDouble() : lastKey;
        return new AnimationClip(name, length, loop, bones);
    }

    private static Channel parseChannel(String clip, String bone, JsonElement element) {
        List<Keyframe> keys = new ArrayList<>();
        if (element.isJsonObject() && !isKeyObject(element.getAsJsonObject())) {
            for (Map.Entry<String, JsonElement> frame : element.getAsJsonObject().entrySet()) {
                double time;
                try {
                    time = Double.parseDouble(frame.getKey());
                } catch (NumberFormatException e) {
                    throw new JsonParseException(clip + "/" + bone + ": invalid keyframe time \"" + frame.getKey() + "\"");
                }
                keys.add(parseKey(clip, bone, time, frame.getValue()));
            }
        } else {
            keys.add(parseKey(clip, bone, 0D, element));
        }
        return keys.isEmpty() ? null : new Channel(keys);
    }

    private static boolean isKeyObject(JsonObject object) {
        return object.has("pre") || object.has("post") || object.has("lerp_mode");
    }

    private static Keyframe parseKey(String clip, String bone, double time, JsonElement value) {
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            JsonElement postElement = object.has("post") ? object.get("post") : object.get("pre");
            JsonElement preElement = object.has("pre") ? object.get("pre") : object.get("post");
            if (postElement == null) {
                throw new JsonParseException(clip + "/" + bone + " @" + time + ": keyframe needs pre or post");
            }
            String lerp = object.has("lerp_mode") ? object.get("lerp_mode").getAsString() : "linear";
            Interpolation mode = switch (lerp) {
                case "catmullrom" -> Interpolation.CATMULLROM;
                case "step" -> Interpolation.STEP;
                default -> Interpolation.LINEAR;
            };
            return new Keyframe(time, vector(clip, bone, preElement), vector(clip, bone, postElement), mode);
        }
        float[] v = vector(clip, bone, value);
        return new Keyframe(time, v, v.clone(), Interpolation.LINEAR);
    }

    private static float[] vector(String clip, String bone, JsonElement element) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (array.size() != 3) {
                throw new JsonParseException(clip + "/" + bone + ": expected 3 values, got " + array.size());
            }
            return new float[]{number(clip, bone, array.get(0)), number(clip, bone, array.get(1)), number(clip, bone, array.get(2))};
        }
        float n = number(clip, bone, element);
        return new float[]{n, n, n};
    }

    private static float number(String clip, String bone, JsonElement element) {
        if (!element.isJsonPrimitive()) {
            throw new JsonParseException(clip + "/" + bone + ": expected a number");
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isNumber()) {
            return primitive.getAsFloat();
        }
        try {
            return Float.parseFloat(primitive.getAsString().trim());
        } catch (NumberFormatException e) {
            throw new JsonParseException(clip + "/" + bone + ": Molang expressions are not supported (\"" + primitive.getAsString() + "\")");
        }
    }
}
