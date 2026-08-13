package com.sshakusora.kaleidoscope_contraption;

import com.mojang.logging.LogUtils;
import com.sshakusora.kaleidoscope_contraption.network.KCPacketHandler;
import com.sshakusora.kaleidoscope_contraption.registry.KCCompatBootstrap;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(KaleidoscopeContraption.MOD_ID)
public class KaleidoscopeContraption {
    public static final String MOD_ID = "kaleidoscope_contraption";
    private static final Logger LOGGER = LogUtils.getLogger();

    public KaleidoscopeContraption(IEventBus modEventBus) {
        modEventBus.addListener(KaleidoscopeContraption::init);
        modEventBus.addListener(KCPacketHandler::register);
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
