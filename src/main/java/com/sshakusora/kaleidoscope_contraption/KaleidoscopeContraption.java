package com.sshakusora.kaleidoscope_contraption;

import com.mojang.logging.LogUtils;
import com.sshakusora.kaleidoscope_contraption.registry.KCCompatBootstrap;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(KaleidoscopeContraption.MOD_ID)
public class KaleidoscopeContraption {
    public static final String MOD_ID = "kaleidoscope_contraption";
    private static final Logger LOGGER = LogUtils.getLogger();

    public KaleidoscopeContraption(FMLJavaModLoadingContext loadingContext) {
        IEventBus modEventBus = loadingContext.getModEventBus();

        modEventBus.addListener(KaleidoscopeContraption::init);
    }

    public static void init(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            KCCompatBootstrap.register();
        });
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
