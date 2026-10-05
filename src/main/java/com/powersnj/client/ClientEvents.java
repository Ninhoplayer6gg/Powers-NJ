package com.powersnj.client;

import com.powersnj.PowersNJ;
import com.powersnj.animation.SuitAnimationClient;
import com.powersnj.client.screen.SkillTreeScreen;
import com.powersnj.core.net.MovementSnapshot;
import com.powersnj.core.suit.SuitDefinitions;
import com.powersnj.hud.PowersHudOverlay;
import com.powersnj.movement.MovementControllers;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * Client game-bus events: key handling, client tick (cooldown countdown, visuals), FOV hooks and
 * session cleanup.
 */
@Mod.EventBusSubscriber(modid = PowersNJ.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        ClientPowerState.tick();
        while (KeyBindings.OPEN_SKILL_TREE.consumeClick()) {
            if (minecraft.screen == null) {
                minecraft.setScreen(new SkillTreeScreen());
            }
        }
        while (KeyBindings.TOGGLE_HUD.consumeClick()) {
            PowersHudOverlay.toggleVisible();
        }
        SpeedsterVisuals.tick(minecraft);
        SuitAnimationClient.tick(minecraft);
    }

    /**
     * FOV hook: high speeds widen the view a little but never as wildly as the raw movement-speed
     * attribute would.
     */
    @SubscribeEvent
    public static void fov(ComputeFovModifierEvent event) {
        MovementSnapshot movement = ClientPowerState.movement(event.getPlayer().getId()).orElse(null);
        if (movement == null || !movement.has(MovementSnapshot.FLAG_ACTIVE)) {
            return;
        }
        if (MovementControllers.SPEEDSTER.equals(movement.controller())) {
            float extra = Math.min(0.25F, Math.max(0F, movement.speed() - 1F) * 0.03F);
            event.setNewFovModifier(Math.min(event.getNewFovModifier(), 1.15F) + extra);
        } else if (MovementControllers.FLIGHT.equals(movement.controller()) && ("HIGH_SPEED".equals(movement.mode()) || "BOOST".equals(movement.mode()))) {
            event.setNewFovModifier(event.getNewFovModifier() + 0.1F);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientPowerState.reset();
        SuitAnimationClient.clear();
        SuitDefinitions.CLIENT.replaceAll(Map.of(), Map.of());
    }
}
