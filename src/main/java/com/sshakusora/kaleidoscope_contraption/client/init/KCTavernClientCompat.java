package com.sshakusora.kaleidoscope_contraption.client.init;

import com.sshakusora.kaleidoscope_contraption.KaleidoscopeContraption;
import com.sshakusora.kaleidoscope_contraption.client.gui.overlay.ContraptionShakerOverlay;
import com.sshakusora.kaleidoscope_contraption.client.render.KCTavernClientRenderHooks;
import com.sshakusora.kaleidoscope_contraption.registry.KCCompatMods;
import com.sshakusora.kaleidoscope_contraption.util.KCContraptionRenderHooks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.CROSSHAIR;

/** Client-only registration entry point for the optional Kaleidoscope Tavern integration. */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT,
        modid = KaleidoscopeContraption.MOD_ID)
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
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        if (!ModList.get().isLoaded(KCCompatMods.TAVERN_ID)) {
            return;
        }
        event.registerAbove(CROSSHAIR.id(), "contraption_shaker_overlay", new ContraptionShakerOverlay());
    }
}
