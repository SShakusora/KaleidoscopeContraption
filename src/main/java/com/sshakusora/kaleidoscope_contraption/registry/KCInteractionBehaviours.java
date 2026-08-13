package com.sshakusora.kaleidoscope_contraption.registry;

import net.neoforged.fml.ModList;

/** Compatibility-aware interaction registration entry point. */
public final class KCInteractionBehaviours {
    private KCInteractionBehaviours() {
    }

    public static void registerDefaults() {
        if (ModList.get().isLoaded(KCCompatMods.COOKERY_ID)) {
            KCCookeryInteractionBehaviours.registerDefaults();
        }
        if (ModList.get().isLoaded(KCCompatMods.TAVERN_ID)) {
            KCTavernInteractionBehaviours.registerDefaults();
        }
    }
}
