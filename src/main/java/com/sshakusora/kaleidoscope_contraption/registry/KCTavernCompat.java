package com.sshakusora.kaleidoscope_contraption.registry;

import com.sshakusora.kaleidoscope_contraption.network.KCTavernPacketHandler;

/** Registration entry point for the optional Kaleidoscope Tavern integration. */
public final class KCTavernCompat {
    private KCTavernCompat() {
    }

    public static void register() {
        KCTavernPacketHandler.register();
        KCTavernBlockMovementChecks.registerDefaults();
        KCTavernInteractionBehaviours.registerDefaults();
        KCTavernMovementBehaviours.registerDefaults();
    }
}
