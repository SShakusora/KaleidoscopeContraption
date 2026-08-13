package com.sshakusora.kaleidoscope_contraption.client.init;

import com.sshakusora.kaleidoscope_contraption.KaleidoscopeContraption;
import com.sshakusora.kaleidoscope_contraption.client.gui.overlay.ContraptionShakerOverlay;
import com.sshakusora.kaleidoscope_contraption.client.render.KCTavernClientRenderHooks;
import com.sshakusora.kaleidoscope_contraption.registry.KCCompatMods;
import com.sshakusora.kaleidoscope_contraption.util.KCContraptionRenderHooks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Client-only registration entry point for the optional Kaleidoscope Tavern integration. */
@EventBusSubscriber(value = Dist.CLIENT, modid = KaleidoscopeContraption.MOD_ID)
public final class KCTavernClientCompat {
    private KCTavernClientCompat() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!ModList.get().isLoaded(KCCompatMods.TAVERN_ID)) {
            return;
        }
        event.enqueueWork(() -> KCContraptionRenderHooks.register(new KCTavernClientRenderHooks()));
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiLayersEvent event) {
        if (!ModList.get().isLoaded(KCCompatMods.TAVERN_ID)) {
            return;
        }
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, KaleidoscopeContraption.asResource("contraption_shaker_overlay"), new ContraptionShakerOverlay());
    }
}
