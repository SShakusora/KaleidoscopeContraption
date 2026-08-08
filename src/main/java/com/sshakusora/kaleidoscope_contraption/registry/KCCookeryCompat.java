package com.sshakusora.kaleidoscope_contraption.registry;

import com.github.ysbbbbbb.kaleidoscopecookery.init.tag.TagMod;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;

/** Registration entry point for the optional Kaleidoscope Cookery integration. */
public final class KCCookeryCompat {
    private KCCookeryCompat() {
    }

    public static void register() {
        ContraptionInteractionUtil.registerAdditionalHeatSourcePredicate(
                state -> state.is(TagMod.HEAT_SOURCE_BLOCKS_WITHOUT_LIT));
        KCCookeryInteractionBehaviours.registerDefaults();
        KCMovementBehaviours.registerDefaults();
        KCContraptionPlacements.registerDefaults();
        KCBlockMovementChecks.registerDefaults();
    }
}
