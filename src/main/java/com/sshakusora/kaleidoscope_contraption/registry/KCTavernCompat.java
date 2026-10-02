package com.sshakusora.kaleidoscope_contraption.registry;

import com.sshakusora.kaleidoscope_contraption.content.behaviour.movement.TavernSeatBackend;

/** Registration entry point for the optional Kaleidoscope Tavern integration. */
public final class KCTavernCompat {
    private KCTavernCompat() {
    }

    public static void register() {
        TavernSeatBackend.registerDefaults();
        KCTavernBlockMovementChecks.registerDefaults();
        KCTavernPlacements.registerDefaults();
        KCTavernInteractionBehaviours.registerDefaults();
        KCTavernMovementBehaviours.registerDefaults();
    }
}
