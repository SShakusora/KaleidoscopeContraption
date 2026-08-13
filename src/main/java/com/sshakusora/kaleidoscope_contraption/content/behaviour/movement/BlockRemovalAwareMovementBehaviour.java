package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;

/** Allows a movement behaviour to clean up Contraption metadata when its block is removed. */
public interface BlockRemovalAwareMovementBehaviour {
    void onBlockRemoved(AbstractContraptionEntity contraptionEntity, BlockPos localPos);
}

