package com.powersnj.client;

import com.powersnj.PowersNJ;
import com.powersnj.animation.SuitAnimationLibrary;
import com.powersnj.client.particle.PowersParticle;
import com.powersnj.client.screen.SuitForgeScreen;
import com.powersnj.hud.PowersHudOverlay;
import com.powersnj.registry.ModBlockEntities;
import com.powersnj.registry.ModEntities;
import com.powersnj.registry.ModMenus;
import com.powersnj.registry.ModParticles;
import com.powersnj.render.AssetAvailability;
import com.powersnj.render.SuitStandRenderer;
import com.powersnj.render.SymbioteFormLayer;
import com.powersnj.render.TendrilRenderer;
import com.powersnj.suit.SuitArmorItem;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.threetag.palladium.entity.BodyPart;

/**
 * Client registrations on the mod bus: screens, renderers, layers, particles, HUD overlay,
 * key bindings and the asset availability cache.
 */
@Mod.EventBusSubscriber(modid = PowersNJ.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.SUIT_FORGE.get(), SuitForgeScreen::new);
            // A worn suit piece replaces the skin's second layer (hat, jacket, sleeves, pants) under it,
            // so the suit never fights with it; the full set also hides the skin (hide_body_part ability).
            ForgeRegistries.ITEMS.getValues().stream()
                    .filter(item -> item instanceof SuitArmorItem && !BodyPart.HIDES_LAYER.contains(item))
                    .forEach(BodyPart.HIDES_LAYER::add);
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.TENDRIL.get(), TendrilRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SUIT_STAND.get(), SuitStandRenderer::new);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new SymbioteFormLayer(renderer));
            }
        }
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SPEED_SPARK.get(), PowersParticle.Provider::new);
        event.registerSpriteSet(ModParticles.NEGATIVE_LIGHTNING.get(), PowersParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SYMBIOTE_GOO.get(), PowersParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SHOCKWAVE_DUST.get(), PowersParticle.Provider::new);
        event.registerSpriteSet(ModParticles.VILTRUMITE_IMPACT.get(), PowersParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll(PowersHudOverlay.ID, PowersHudOverlay.INSTANCE);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.OPEN_SKILL_TREE);
        event.register(KeyBindings.TOGGLE_HUD);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(AssetAvailability.INSTANCE);
        event.registerReloadListener(SuitAnimationLibrary.INSTANCE);
    }
}
