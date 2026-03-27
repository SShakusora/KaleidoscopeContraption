package com.sshakusora.kaleidoscope_contraption.registry;

import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.actors.seat.SeatMovementBehaviour;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.movement.*;

public class KCMovementBehaviours {
    public static void registerDefaults() {
        // 注册炒锅的移动行为（用于处理Tick逻辑）
        MovementBehaviour.REGISTRY.register(ModBlocks.POT.get(), new PotBlockMovementBehaviour());

        // 注册汤锅的移动行为（用于处理Tick逻辑）
        MovementBehaviour.REGISTRY.register(ModBlocks.STOCKPOT.get(), new StockpotBlockMovementBehaviour());

        // 注册沙威玛烤架的移动行为（用于处理Tick逻辑）
        MovementBehaviour.REGISTRY.register(ModBlocks.SHAWARMA_SPIT.get(), new ShawarmaSpitBlockMovementBehaviour());

        // 注册蒸笼的移动行为（用于处理Tick逻辑）
        MovementBehaviour.REGISTRY.register(ModBlocks.STEAMER.get(), new SteamerBlockMovementBehaviour());

        // 注册椅子的移动行为（用于处理坐下实体）
//        ChairBlockMovementBehaviour chairMovement = new ChairBlockMovementBehaviour();
//        MovementBehaviour.REGISTRY.register(ModBlocks.CHAIR_OAK.get(), chairMovement);
//        MovementBehaviour.REGISTRY.register(ModBlocks.CHAIR_SPRUCE.get(), chairMovement);
//        MovementBehaviour.REGISTRY.register(ModBlocks.CHAIR_ACACIA.get(), chairMovement);
//        MovementBehaviour.REGISTRY.register(ModBlocks.CHAIR_BAMBOO.get(), chairMovement);
//        MovementBehaviour.REGISTRY.register(ModBlocks.CHAIR_BIRCH.get(), chairMovement);
//        MovementBehaviour.REGISTRY.register(ModBlocks.CHAIR_CHERRY.get(), chairMovement);
//        MovementBehaviour.REGISTRY.register(ModBlocks.CHAIR_CRIMSON.get(), chairMovement);
//        MovementBehaviour.REGISTRY.register(ModBlocks.CHAIR_DARK_OAK.get(), chairMovement);
//        MovementBehaviour.REGISTRY.register(ModBlocks.CHAIR_JUNGLE.get(), chairMovement);
//        MovementBehaviour.REGISTRY.register(ModBlocks.CHAIR_MANGROVE.get(), chairMovement);
//        MovementBehaviour.REGISTRY.register(ModBlocks.CHAIR_WARPED.get(), chairMovement);

        // 注册厨娘凳的交互行为
        SeatMovementBehaviour seatMovementBehaviour = new SeatMovementBehaviour();
        MovementBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_OAK.get(), seatMovementBehaviour);
        MovementBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_SPRUCE.get(), seatMovementBehaviour);
        MovementBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_ACACIA.get(), seatMovementBehaviour);
        MovementBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_BAMBOO.get(), seatMovementBehaviour);
        MovementBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_BIRCH.get(), seatMovementBehaviour);
        MovementBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_CHERRY.get(), seatMovementBehaviour);
        MovementBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_CRIMSON.get(), seatMovementBehaviour);
        MovementBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_DARK_OAK.get(), seatMovementBehaviour);
        MovementBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_JUNGLE.get(), seatMovementBehaviour);
        MovementBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_MANGROVE.get(), seatMovementBehaviour);
        MovementBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_WARPED.get(), seatMovementBehaviour);
    }
}
