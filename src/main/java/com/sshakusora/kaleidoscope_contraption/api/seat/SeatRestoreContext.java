package com.sshakusora.kaleidoscope_contraption.api.seat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Context passed after a moving passenger has been restored to a static seat. */
public record SeatRestoreContext(
        Level level,
        BlockPos worldPos,
        BlockState state,
        Entity passenger,
        Entity seatEntity
) {
}
