package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

/**
 * Keeps Pressing Tub registered as a movement actor. Entity impact processing
 * lives in its moving interaction because Create exposes collision callbacks there.
 */
public final class TavernPressingTubMovementBehaviour implements MovementBehaviour {
    @Override
    public void tick(MovementContext context) {
        // The tub has no passive world tick; its state changes through local interaction NBT.
    }
}
