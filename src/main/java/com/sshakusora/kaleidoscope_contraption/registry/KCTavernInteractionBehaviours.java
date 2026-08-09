package com.sshakusora.kaleidoscope_contraption.registry;

import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.sshakusora.kaleidoscope_contraption.api.placement.ContraptionPlacementMovingInteraction;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction.*;

/** Registrations that require Kaleidoscope Tavern. */
public final class KCTavernInteractionBehaviours {
    private KCTavernInteractionBehaviours() {
    }

    public static void registerDefaults() {
        MovingInteractionBehaviour.REGISTRY.register(
                ModBlocks.PRESSING_TUB.get(), new TavernPressingTubMovingInteraction());
        MovingInteractionBehaviour.REGISTRY.register(
                ModBlocks.BARREL.get(), new TavernBarrelMovingInteraction());
        MovingInteractionBehaviour.REGISTRY.register(
                ModBlocks.TAP.get(), new TavernTapMovingInteraction());

        MovingInteractionBehaviour.REGISTRY.register(
                ModBlocks.TABLE.get(), new ContraptionPlacementMovingInteraction(KCTavernPlacements.TAVERN_TABLE_TOP));
        MovingInteractionBehaviour.REGISTRY.register(
                ModBlocks.BAR_COUNTER.get(),
                new ContraptionPlacementMovingInteraction(KCTavernPlacements.TAVERN_BAR_COUNTER_TOP));

        DrinkBlockMovingInteraction drinkInteraction = new DrinkBlockMovingInteraction();
        registerDrinkBlocks(drinkInteraction);

        MovingInteractionBehaviour.REGISTRY.register(
                ModBlocks.SIGNATURE_COCKTAIL.get(), new SignatureCocktailBlockMovingInteraction());

        TavernGlasswareBlockMovingInteraction glasswareInteraction =
                new TavernGlasswareBlockMovingInteraction();
        registerGlasswareBlocks(glasswareInteraction);

        MovingInteractionBehaviour.REGISTRY.register(
                ModBlocks.MOLOTOV.get(), new TavernMolotovBlockMovingInteraction());

        MovingInteractionBehaviour.REGISTRY.register(
                ModBlocks.SHAKER.get(), new ShakerBlockMovingInteraction());
        MovingInteractionBehaviour.REGISTRY.register(
                ModBlocks.POTION_BOTTLE.get(), new PotionBottleBlockMovingInteraction());

        TavernStorageBlockMovingInteraction storageInteraction = new TavernStorageBlockMovingInteraction();
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.HOLDER.get(), storageInteraction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.TILTED_RACK.get(), storageInteraction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CIRCULAR_RACK.get(), storageInteraction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CELLAR_CABINET.get(), storageInteraction);

        TavernBarCabinetMovingInteraction cabinetInteraction = new TavernBarCabinetMovingInteraction();
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.BAR_CABINET.get(), cabinetInteraction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.GLASS_BAR_CABINET.get(), cabinetInteraction);

        MovingInteractionBehaviour.REGISTRY.register(
                ModBlocks.GLASSWARE_HOLDER.get(), new TavernGlasswareHolderMovingInteraction());

        TavernTextBoardMovingInteraction textBoardInteraction = new TavernTextBoardMovingInteraction();
        registerTextBoards(textBoardInteraction);

        TavernSeatMovingInteraction seatInteraction = new TavernSeatMovingInteraction();
        registerSeats(seatInteraction);
    }

    private static void registerTextBoards(TavernTextBoardMovingInteraction interaction) {
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CHALKBOARD.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.BASE_SANDWICH_BOARD.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.GRASS_SANDWICH_BOARD.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.ALLIUM_SANDWICH_BOARD.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.AZURE_BLUET_SANDWICH_BOARD.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CORNFLOWER_SANDWICH_BOARD.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.ORCHID_SANDWICH_BOARD.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.PEONY_SANDWICH_BOARD.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.PINK_PETALS_SANDWICH_BOARD.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.PITCHER_PLANT_SANDWICH_BOARD.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.POPPY_SANDWICH_BOARD.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.SUNFLOWER_SANDWICH_BOARD.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.TORCHFLOWER_SANDWICH_BOARD.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.TULIP_SANDWICH_BOARD.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.WITHER_ROSE_SANDWICH_BOARD.get(), interaction);
    }

    private static void registerSeats(TavernSeatMovingInteraction interaction) {
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.WHITE_SOFA.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.LIGHT_GRAY_SOFA.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.GRAY_SOFA.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.BLACK_SOFA.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.BROWN_SOFA.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.RED_SOFA.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.ORANGE_SOFA.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.YELLOW_SOFA.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.LIME_SOFA.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.GREEN_SOFA.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CYAN_SOFA.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.LIGHT_BLUE_SOFA.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.BLUE_SOFA.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.PURPLE_SOFA.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.MAGENTA_SOFA.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.PINK_SOFA.get(), interaction);

        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.WHITE_BAR_STOOL.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.LIGHT_GRAY_BAR_STOOL.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.GRAY_BAR_STOOL.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.BLACK_BAR_STOOL.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.BROWN_BAR_STOOL.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.RED_BAR_STOOL.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.ORANGE_BAR_STOOL.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.YELLOW_BAR_STOOL.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.LIME_BAR_STOOL.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.GREEN_BAR_STOOL.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CYAN_BAR_STOOL.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.LIGHT_BLUE_BAR_STOOL.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.BLUE_BAR_STOOL.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.PURPLE_BAR_STOOL.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.MAGENTA_BAR_STOOL.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.PINK_BAR_STOOL.get(), interaction);
    }

    private static void registerDrinkBlocks(DrinkBlockMovingInteraction interaction) {
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.WINE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CHAMPAGNE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.VODKA.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.BRANDY.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CARIGNAN.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.SAKURA_WINE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.PLUM_WINE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.WHISKEY.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.ICE_WINE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.POLARIS_SWEET_WHITE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.HONEY_WINE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.RED_QUEEN.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.MINERS_STAR.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.RUM.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.RIESLING_DRY_WHITE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.SUNSET_GLOW.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.MADAME_SHEXIANG.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.SWEET_BERRY_WINE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.SHERRY.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.MOTHER_SNOW.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.LUMINOUS_BRIDE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.GLOWFLOWER_BREW.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.SAUVIGNON_BLANC_DRY_WHITE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.VINEGAR.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.WATERMELON_JUICE.get(), interaction);
    }

    private static void registerGlasswareBlocks(TavernGlasswareBlockMovingInteraction interaction) {
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.EMPTY_GLASSWARE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.MYSTERY_COCKTAIL.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.WHITE_LADY.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.EMERALD.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.BRASS_HEART.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.GODFATHER.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.GRASSHOPPER.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.SCREWDRIVER.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.MOJITO.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.ALLIUM_GARDEN.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.DEPTH_CHARGE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.NETHER_SPECIAL.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.BLOODY_MARY.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.SCULK_SPECIAL.get(), interaction);
    }
}
