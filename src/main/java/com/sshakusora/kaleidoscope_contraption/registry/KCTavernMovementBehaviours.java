package com.sshakusora.kaleidoscope_contraption.registry;

import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.movement.TavernSeatMovementBehaviour;

/** Movement actor registration for Tavern seats. */
public final class KCTavernMovementBehaviours {
    private KCTavernMovementBehaviours() {
    }

    public static void registerDefaults() {
        TavernSeatMovementBehaviour seatMovement = new TavernSeatMovementBehaviour();
        registerSeats(seatMovement);
    }

    private static void registerSeats(TavernSeatMovementBehaviour movement) {
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
}
