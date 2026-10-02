package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.actors.seat.SeatMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.BlockPos;

/** Standard Create movement behaviour for any registered entity-backed seat. */
public class ContraptionSeatMovementBehaviour extends SeatMovementBehaviour
        implements BlockRemovalAwareMovementBehaviour {
    @Override
    public void startMoving(MovementContext context) {
        super.startMoving(context);
        int index = context.contraption.getSeats().indexOf(context.localPos);
        if (index < 0) {
            context.contraption.getSeats().add(context.localPos);
            index = context.contraption.getSeats().size() - 1;
        }
        context.data.putInt("SeatIndex", index);
    }

    @Override
    public void onBlockRemoved(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        ContraptionSeatSupport.ejectPassengers(contraptionEntity, localPos);
        ContraptionSeatSupport.removeSeat(contraptionEntity, localPos);
    }
}
