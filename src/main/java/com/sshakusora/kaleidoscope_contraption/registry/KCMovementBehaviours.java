package com.sshakusora.kaleidoscope_contraption.registry;

import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.movement.PotBlockMovementBehaviour;

public class KCMovementBehaviours {
    public static void registerDefaults() {
        // 注册炒锅的移动行为（用于处理Tick逻辑）
        MovementBehaviour.REGISTRY.register(ModBlocks.POT.get(), new PotBlockMovementBehaviour());
    }
}
