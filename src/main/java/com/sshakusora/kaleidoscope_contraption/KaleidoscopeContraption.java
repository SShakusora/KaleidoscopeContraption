package com.sshakusora.kaleidoscope_contraption;

import com.mojang.logging.LogUtils;
import com.sshakusora.kaleidoscope_contraption.network.KCPacketHandler;
import com.sshakusora.kaleidoscope_contraption.registry.KCInteractionBehaviours;
import com.sshakusora.kaleidoscope_contraption.registry.KCMovementBehaviours;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(KaleidoscopeContraption.MOD_ID)
public class KaleidoscopeContraption {
    public static final String MOD_ID = "kaleidoscope_contraption";
    private static final Logger LOGGER = LogUtils.getLogger();

    public KaleidoscopeContraption() {
        ModLoadingContext modLoadingContext = ModLoadingContext.get();

        IEventBus modEventBus = FMLJavaModLoadingContext.get()
                .getModEventBus();
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;

        modEventBus.addListener(KaleidoscopeContraption::init);
    }

    public static void init(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 注册网络包
            KCPacketHandler.register();
            // 注册交互行为
            KCMovementBehaviours.registerDefaults();
            KCInteractionBehaviours.registerDefaults();
        });
    }
}
