package com.powersnj.core.animation;

import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnimationClipParserTest {

    private static final String FILE = """
            {
              "format_version": "1.8.0",
              "animations": {
                "animation.test.loop": {
                  "loop": true,
                  "animation_length": 2,
                  "bones": {
                    "right_arm": {
                      "rotation": {
                        "0.0": [0, 0, 0],
                        "1.0": {"post": [-90, "5", 0], "lerp_mode": "catmullrom"},
                        "2.0": [0, 0, 0]
                      },
                      "position": [0, 1, 0]
                    },
                    "cape": {"rotation": {"0.5": {"pre": [10, 0, 0], "post": [20, 0, 0]}}},
                    "ignored": {"scale": [2, 2, 2]}
                  }
                },
                "animation.test.hold": {"loop": "hold_on_last_frame", "bones": {"head": {"rotation": {"0.0": [0, 0, 0], "0.4": 30}}}},
                "animation.test.once": {"bones": {}}
              }
            }
            """;

    @Test
    void parsesLoopModesLengthsAndValueForms() {
        Map<String, AnimationClip> clips = AnimationClipParser.parse(FILE);
        assertEquals(3, clips.size());
        AnimationClip loop = clips.get("animation.test.loop");
        assertEquals(LoopMode.LOOP, loop.loop());
        assertEquals(2D, loop.length());
        assertEquals(2, loop.bones().size(), "bones without rotation/position are skipped");

        BoneTrack arm = loop.bones().get("right_arm");
        assertEquals(Interpolation.CATMULLROM, arm.rotation().key(1).mode());
        float[] out = new float[AnimationPose.SIZE];
        arm.sample(1D, out);
        assertArrayEquals(new float[]{-90F, 5F, 0F, 0F, 1F, 0F}, out, 1e-5F);

        Keyframe split = loop.bones().get("cape").rotation().key(0);
        assertEquals(10F, split.pre()[0]);
        assertEquals(20F, split.post()[0]);

        AnimationClip hold = clips.get("animation.test.hold");
        assertEquals(LoopMode.HOLD, hold.loop());
        assertEquals(0.4D, hold.length(), 1e-9, "length defaults to the last keyframe");
        hold.bones().get("head").sample(5D, out);
        assertEquals(30F, out[0], "a single number fills the three axes");
        assertEquals(30F, out[2]);

        assertEquals(LoopMode.ONCE, clips.get("animation.test.once").loop());
    }

    @Test
    void localTimeWrapsLoopsAndClampsOthers() {
        Map<String, AnimationClip> clips = AnimationClipParser.parse(FILE);
        assertEquals(0.5D, clips.get("animation.test.loop").localTime(2.5D), 1e-9);
        assertEquals(0.4D, clips.get("animation.test.hold").localTime(3D), 1e-9);
        assertFalse(clips.get("animation.test.hold").isFinished(10D), "hold never finishes");
    }

    @Test
    void rejectsMolangAndBrokenFiles() {
        assertThrows(JsonParseException.class, () -> AnimationClipParser.parse(
                "{\"animations\":{\"a\":{\"bones\":{\"b\":{\"rotation\":[\"math.sin(query.anim_time)\",0,0]}}}}}"));
        assertThrows(JsonParseException.class, () -> AnimationClipParser.parse("{\"no\":1}"));
        assertThrows(JsonParseException.class, () -> AnimationClipParser.parse("{\"animations\":{\"a\":{\"bones\":{\"b\":{\"rotation\":[1,2]}}}}}"));
    }
}
