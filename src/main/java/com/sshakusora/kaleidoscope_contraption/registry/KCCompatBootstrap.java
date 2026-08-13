package com.sshakusora.kaleidoscope_contraption.registry;

import net.neoforged.fml.ModList;

/** Dispatches optional integrations after NeoForge has finished loading the mod list. */
public final class KCCompatBootstrap {
    private KCCompatBootstrap() {
    }

    public static void register() {
        if (ModList.get().isLoaded(KCCompatMods.COOKERY_ID)) {
            KCCookeryCompat.register();
        }
        if (ModList.get().isLoaded(KCCompatMods.TAVERN_ID)) {
            KCTavernCompat.register();
        }
    }
}
