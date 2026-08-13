package com.sshakusora.kaleidoscope_contraption.registry;

import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.movement.*;

/** Movement actor registration for Tavern seats. */
public final class KCTavernMovementBehaviours {
    private KCTavernMovementBehaviours() {
    }

    public static void registerDefaults() {
        MovementBehaviour.REGISTRY.register(
                ModBlocks.PRESSING_TUB.get(), new TavernPressingTubMovementBehaviour());
        MovementBehaviour.REGISTRY.register(
                ModBlocks.BARREL.get(), new TavernBarrelMovementBehaviour());
        MovementBehaviour.REGISTRY.register(
                ModBlocks.TAP.get(), new TavernTapMovementBehaviour());

        TavernIncenseMovementBehaviour incenseMovement = new TavernIncenseMovementBehaviour();
        registerIncenseBlocks(incenseMovement);

        registerSofas(new TavernSeatMovementBehaviour());
        registerBarStools(new TavernBarStoolMovementBehaviour());
    }

    private static void registerSofas(TavernSeatMovementBehaviour movement) {
        MovementBehaviour.REGISTRY.register(ModBlocks.WHITE_SOFA.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.LIGHT_GRAY_SOFA.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.GRAY_SOFA.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.BLACK_SOFA.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.BROWN_SOFA.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.RED_SOFA.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.ORANGE_SOFA.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.YELLOW_SOFA.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.LIME_SOFA.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.GREEN_SOFA.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.CYAN_SOFA.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.LIGHT_BLUE_SOFA.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.BLUE_SOFA.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.PURPLE_SOFA.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.MAGENTA_SOFA.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.PINK_SOFA.get(), movement);
    }

    private static void registerBarStools(TavernBarStoolMovementBehaviour movement) {
        MovementBehaviour.REGISTRY.register(ModBlocks.WHITE_BAR_STOOL.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.LIGHT_GRAY_BAR_STOOL.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.GRAY_BAR_STOOL.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.BLACK_BAR_STOOL.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.BROWN_BAR_STOOL.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.RED_BAR_STOOL.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.ORANGE_BAR_STOOL.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.YELLOW_BAR_STOOL.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.LIME_BAR_STOOL.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.GREEN_BAR_STOOL.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.CYAN_BAR_STOOL.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.LIGHT_BLUE_BAR_STOOL.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.BLUE_BAR_STOOL.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.PURPLE_BAR_STOOL.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.MAGENTA_BAR_STOOL.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.PINK_BAR_STOOL.get(), movement);
    }

    private static void registerIncenseBlocks(TavernIncenseMovementBehaviour movement) {
        MovementBehaviour.REGISTRY.register(ModBlocks.SAKURA_INCENSE.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.PINE_INCENSE.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.GINKGO_INCENSE.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.SPORE_INCENSE.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.CATNIP_INCENSE.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.SNOW_INCENSE.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.BUTTERFLY_INCENSE.get(), movement);
        MovementBehaviour.REGISTRY.register(ModBlocks.FIREFLY_INCENSE.get(), movement);
    }
}

