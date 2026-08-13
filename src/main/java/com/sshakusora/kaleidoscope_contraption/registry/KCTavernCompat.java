package com.sshakusora.kaleidoscope_contraption.registry;

/** Registration entry point for the optional Kaleidoscope Tavern integration. */
public final class KCTavernCompat {
    private KCTavernCompat() {
    }

    public static void register() {
        KCTavernBlockMovementChecks.registerDefaults();
        KCTavernPlacements.registerDefaults();
        KCTavernInteractionBehaviours.registerDefaults();
        KCTavernMovementBehaviours.registerDefaults();
    }
}
