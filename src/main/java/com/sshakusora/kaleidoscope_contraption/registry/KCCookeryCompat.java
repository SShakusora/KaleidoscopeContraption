package com.sshakusora.kaleidoscope_contraption.registry;

import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.tag.TagMod;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;

/** Registration entry point for the optional Kaleidoscope Cookery integration. */
public final class KCCookeryCompat {
    private KCCookeryCompat() {
    }

    public static void register() {
        ContraptionInteractionUtil.registerAdditionalHeatSourcePredicate(
                state -> state.is(TagMod.HEAT_SOURCE_BLOCKS_WITHOUT_LIT));
        MountedItemStorageType.REGISTRY.register(
                ModBlocks.BAMBOO_TRAY.get(), AllMountedStorageTypes.SIMPLE.get());
        MountedItemStorageType.REGISTRY.register(
                ModBlocks.TEAPOT.get(), AllMountedStorageTypes.SIMPLE.get());
        KCCookeryInteractionBehaviours.registerDefaults();
        KCMovementBehaviours.registerDefaults();
        KCContraptionPlacements.registerDefaults();
        KCBlockMovementChecks.registerDefaults();
    }
}
