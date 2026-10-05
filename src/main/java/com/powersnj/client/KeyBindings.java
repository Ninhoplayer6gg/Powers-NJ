package com.powersnj.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Powers NJ key bindings. Ability keys are Palladium's (ability bar / ability wheel).
 */
public final class KeyBindings {

    public static final String CATEGORY = "key.categories.powersnj";

    public static final KeyMapping OPEN_SKILL_TREE = new KeyMapping("key.powersnj.skill_tree", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);
    public static final KeyMapping TOGGLE_HUD = new KeyMapping("key.powersnj.toggle_hud", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY);

    private KeyBindings() {
    }
}
