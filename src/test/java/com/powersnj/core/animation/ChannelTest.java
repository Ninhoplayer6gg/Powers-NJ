package com.powersnj.core.animation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChannelTest {

    private static float[] at(Channel channel, double time) {
        float[] out = new float[3];
        channel.sample(time, out, 0);
        return out;
    }

    private static Keyframe key(double time, float x, Interpolation mode) {
        float[] v = {x, 0F, 0F};
        return new Keyframe(time, v, v.clone(), mode);
    }

    @Test
    void linearInterpolatesAndClampsOutsideTheKeys() {
        Channel channel = new Channel(List.of(key(0.5, 10F, Interpolation.LINEAR), key(1.5, 30F, Interpolation.LINEAR)));
        assertEquals(10F, at(channel, 0D)[0], 1e-5);
        assertEquals(20F, at(channel, 1.0D)[0], 1e-5);
        assertEquals(30F, at(channel, 9D)[0], 1e-5);
    }

    @Test
    void keysAreSortedByTime() {
        Channel channel = new Channel(List.of(key(1D, 10F, Interpolation.LINEAR), key(0D, 0F, Interpolation.LINEAR)));
        assertEquals(5F, at(channel, 0.5D)[0], 1e-5);
    }

    @Test
    void catmullRomPassesThroughKeysAndUsesNeighbours() {
        Channel channel = new Channel(List.of(
                key(0D, 0F, Interpolation.CATMULLROM), key(1D, 10F, Interpolation.CATMULLROM),
                key(2D, 10F, Interpolation.CATMULLROM), key(3D, 0F, Interpolation.CATMULLROM)));
        assertEquals(10F, at(channel, 1D)[0], 1e-4);
        assertEquals(10F, at(channel, 2D)[0], 1e-4);
        // symmetric neighbours: the middle segment bulges above the keys
        assertTrue(at(channel, 1.5D)[0] > 10F);
        // same formula as tools/suit-assets/suitlib/clips.py
        float expected = Channel.catmullRom(0F, 10F, 10F, 0F, 0.5F);
        assertEquals(expected, at(channel, 1.5D)[0], 1e-5);
    }

    @Test
    void catmullRomClampsNeighboursAtTheEnds() {
        Channel channel = new Channel(List.of(key(0D, 0F, Interpolation.CATMULLROM), key(1D, 10F, Interpolation.CATMULLROM)));
        // p0 = p1 and p3 = p2: an ease curve between the two keys
        assertEquals(Channel.catmullRom(0F, 0F, 10F, 10F, 0.25F), at(channel, 0.25D)[0], 1e-5);
        assertEquals(5F, at(channel, 0.5D)[0], 1e-4);
    }

    @Test
    void stepHoldsThePreviousValue() {
        Channel channel = new Channel(List.of(key(0D, 1F, Interpolation.LINEAR), key(1D, 9F, Interpolation.STEP)));
        assertEquals(1F, at(channel, 0.99D)[0], 1e-5);
        assertEquals(9F, at(channel, 1D)[0], 1e-5);
    }

    @Test
    void prePostSplitsAKeyframe() {
        Channel channel = new Channel(List.of(
                key(0D, 0F, Interpolation.LINEAR),
                new Keyframe(1D, new float[]{10F, 0F, 0F}, new float[]{-10F, 0F, 0F}, Interpolation.LINEAR),
                key(2D, 0F, Interpolation.LINEAR)));
        assertEquals(5F, at(channel, 0.5D)[0], 1e-5);
        assertEquals(-5F, at(channel, 1.5D)[0], 1e-5);
    }

    @Test
    void rejectsEmptyChannels() {
        assertThrows(IllegalArgumentException.class, () -> new Channel(List.of()));
    }
}
