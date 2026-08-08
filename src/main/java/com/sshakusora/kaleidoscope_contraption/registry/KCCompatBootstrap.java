package com.sshakusora.kaleidoscope_contraption.registry;

import com.sshakusora.kaleidoscope_contraption.network.KCPacketHandler;
import net.minecraftforge.fml.ModList;

/** Dispatches optional integrations after Forge has finished loading the mod list. */
public final class KCCompatBootstrap {
    private KCCompatBootstrap() {
    }

    public static void register() {
        KCPacketHandler.register();

        if (ModList.get().isLoaded(KCCompatMods.COOKERY_ID)) {
            KCCookeryCompat.register();
        }
        if (ModList.get().isLoaded(KCCompatMods.TAVERN_ID)) {
            KCTavernCompat.register();
        }
    }
}
