package com.sshakusora.kaleidoscope_contraption.registry;

import com.sshakusora.kaleidoscope_contraption.KaleidoscopeContraption;
import com.sshakusora.kaleidoscope_contraption.api.placement.ContraptionPlacementRegistry;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.placement.*;
import net.minecraft.resources.ResourceLocation;

public final class KCContraptionPlacements {
    public static final ResourceLocation STOVE_TOP = KaleidoscopeContraption.asResource("stove_top");
    public static final ResourceLocation STOVE_TOP_POT = KaleidoscopeContraption.asResource("stove_top/pot");
    public static final ResourceLocation STOVE_TOP_STOCKPOT = KaleidoscopeContraption.asResource("stove_top/stockpot");
    public static final ResourceLocation STOVE_TOP_STEAMER = KaleidoscopeContraption.asResource("stove_top/steamer");
    public static final ResourceLocation STOVE_TOP_TEAPOT = KaleidoscopeContraption.asResource("stove_top/teapot");
    public static final ResourceLocation TABLE_TOP = KaleidoscopeContraption.asResource("table_top");
    public static final ResourceLocation TABLE_TOP_FOOD = KaleidoscopeContraption.asResource("table_top/food");
    public static final ResourceLocation TABLE_TOP_CHOPPING_BOARD = KaleidoscopeContraption.asResource("table_top/chopping_board");
    public static final ResourceLocation TABLE_TOP_TEACUP = KaleidoscopeContraption.asResource("table_top/teacup");
    public static final ResourceLocation STEAMER_TOP = KaleidoscopeContraption.asResource("steamer_top");
    public static final ResourceLocation STEAMER_TOP_STEAMER = KaleidoscopeContraption.asResource("steamer_top/steamer");

    private KCContraptionPlacements() {
    }

    public static void registerDefaults() {
        ContraptionPlacementRegistry.register(STOVE_TOP, STOVE_TOP_POT, new StovePotPlacementRule());
        ContraptionPlacementRegistry.register(STOVE_TOP, STOVE_TOP_STOCKPOT, new StoveStockpotPlacementRule());
        ContraptionPlacementRegistry.register(STOVE_TOP, STOVE_TOP_STEAMER, new StoveSteamerPlacementRule());
        ContraptionPlacementRegistry.register(STOVE_TOP, STOVE_TOP_TEAPOT, new StoveTeapotPlacementRule());
        ContraptionPlacementRegistry.register(TABLE_TOP, TABLE_TOP_FOOD, new TableFoodPlacementRule());
        ContraptionPlacementRegistry.register(TABLE_TOP, TABLE_TOP_CHOPPING_BOARD,
                new TableChoppingBoardPlacementRule());
        ContraptionPlacementRegistry.register(TABLE_TOP, TABLE_TOP_TEACUP,
                new TableTeacupPlacementRule());
        ContraptionPlacementRegistry.register(STEAMER_TOP, STEAMER_TOP_STEAMER,
                new SteamerTopPlacementRule());
    }
}
