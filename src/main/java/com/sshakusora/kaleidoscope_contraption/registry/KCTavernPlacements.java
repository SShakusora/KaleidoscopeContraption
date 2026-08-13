package com.sshakusora.kaleidoscope_contraption.registry;

import com.sshakusora.kaleidoscope_contraption.KaleidoscopeContraption;
import com.sshakusora.kaleidoscope_contraption.api.placement.ContraptionPlacementRegistry;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.placement.TavernSurfacePlacementRule;
import net.minecraft.resources.ResourceLocation;

/** Placement registrations that require Kaleidoscope Tavern. */
public final class KCTavernPlacements {
    public static final ResourceLocation TAVERN_TABLE_TOP =
            KaleidoscopeContraption.asResource("tavern/table_top");
    public static final ResourceLocation TAVERN_BAR_COUNTER_TOP =
            KaleidoscopeContraption.asResource("tavern/bar_counter_top");

    private static final ResourceLocation TAVERN_TABLE_TOP_RULE =
            KaleidoscopeContraption.asResource("tavern/table_top/items");
    private static final ResourceLocation TAVERN_BAR_COUNTER_TOP_RULE =
            KaleidoscopeContraption.asResource("tavern/bar_counter_top/items");

    private KCTavernPlacements() {
    }

    public static void registerDefaults() {
        ContraptionPlacementRegistry.register(TAVERN_TABLE_TOP, TAVERN_TABLE_TOP_RULE,
                new TavernSurfacePlacementRule());
        ContraptionPlacementRegistry.register(TAVERN_BAR_COUNTER_TOP, TAVERN_BAR_COUNTER_TOP_RULE,
                new TavernSurfacePlacementRule());
    }
}

