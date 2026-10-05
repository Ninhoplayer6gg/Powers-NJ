package com.powersnj.core.animation;

import java.util.List;

/**
 * Bone names shared by every suit rig (see tools/suit-assets/README.md). The humanoid parts are
 * driven through the player's model parts; every other bone is a suit bone animated locally inside
 * the GeckoLib armor model.
 */
public final class HumanoidBones {

    /** Whole body: moves the player's render pose (pivot at the feet). */
    public static final String ROOT = "root";
    public static final String HEAD = "head";
    public static final String BODY = "body";
    public static final String RIGHT_ARM = "right_arm";
    public static final String LEFT_ARM = "left_arm";
    public static final String RIGHT_LEG = "right_leg";
    public static final String LEFT_LEG = "left_leg";

    /** Parts replaced (blended by weight) on top of the vanilla pose. */
    public static final List<String> OVERRIDE_PARTS = List.of(BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG);
    /** All bones that map to the player model instead of a suit bone. */
    public static final List<String> PLAYER_BONES = List.of(ROOT, HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG);

    private HumanoidBones() {
    }

    public static boolean isPlayerBone(String bone) {
        return PLAYER_BONES.contains(bone);
    }
}
