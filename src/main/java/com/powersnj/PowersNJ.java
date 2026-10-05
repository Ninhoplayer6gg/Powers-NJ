package com.powersnj;

import com.mojang.logging.LogUtils;
import com.powersnj.compat.palladium.PalladiumCompat;
import com.powersnj.config.PowersServerConfig;
import com.powersnj.network.PowersNetwork;
import com.powersnj.registry.ModRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Powers NJ entry point. Only wiring lives here; every system registers itself through its own
 * class (registries, config, network, Palladium compat). Forge-bus listeners use
 * {@code @Mod.EventBusSubscriber} in their own packages.
 */
@Mod(PowersNJ.MOD_ID)
public final class PowersNJ {

    public static final String MOD_ID = "powersnj";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PowersNJ() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModRegistries.register(modBus);
        PalladiumCompat.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, PowersServerConfig.SPEC);

        modBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.powersnj.client.ClientBootstrap::init);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            PowersNetwork.register();
            PalladiumCompat.commonSetup();
        });
        LOGGER.info("Powers NJ common setup complete");
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
