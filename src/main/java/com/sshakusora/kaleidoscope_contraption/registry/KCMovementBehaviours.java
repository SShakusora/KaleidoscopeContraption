package com.sshakusora.kaleidoscope_contraption.registry;

import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.movement.PotBlockMovementBehaviour;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.movement.ShawarmaSpitBlockMovementBehaviour;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.movement.SteamerBlockMovementBehaviour;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.movement.StockpotBlockMovementBehaviour;

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
    }
}
