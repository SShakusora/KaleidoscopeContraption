package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.simibubi.create.content.contraptions.actors.seat.SeatMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

public class ChairBlockMovementBehaviour extends SeatMovementBehaviour {

    @Override
    public void startMoving(MovementContext context) {
        super.startMoving(context);
        // 记录椅子在Contraption中的索引位置
        int indexOf = context.contraption.getSeats().indexOf(context.localPos);
        if (indexOf == -1) {
            // 如果不在seats列表中，添加进去
            context.contraption.getSeats().add(context.localPos);
            indexOf = context.contraption.getSeats().size() - 1;
        }
        context.data.putInt("SeatIndex", indexOf);
    }
}
