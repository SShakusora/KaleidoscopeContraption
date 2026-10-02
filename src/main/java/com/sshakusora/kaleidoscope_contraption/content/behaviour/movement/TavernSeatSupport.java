package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.api.seat.ContraptionSeatBackends;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Backwards-compatible Tavern facade over the unified seat support. */
public final class TavernSeatSupport {
    private TavernSeatSupport() {
    }

    public static boolean isTavernSeat(BlockState state) {
        return ContraptionSeatSupport.isSeat(state, ContraptionSeatBackends.TAVERN);
    }

    public static double getSeatHeight(BlockState state) {
        return ContraptionSeatSupport.getSeatHeight(state);
    }

    public static Vec3 getSeatEntityPosition(AbstractContraptionEntity contraptionEntity,
                                              BlockPos localPos, float partialTicks) {
        return ContraptionSeatSupport.getSeatEntityPosition(contraptionEntity, localPos, partialTicks);
    }

    public static Vec3 getPassengerPosition(AbstractContraptionEntity contraptionEntity,
                                             Entity passenger, float partialTicks) {
        return ContraptionSeatSupport.getPassengerPosition(contraptionEntity, passenger, partialTicks);
    }

    public static void ejectPassengers(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        ContraptionSeatSupport.ejectPassengers(contraptionEntity, localPos);
    }

    public static void removeSeat(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        ContraptionSeatSupport.removeSeat(contraptionEntity, localPos);
    }

    public static boolean restorePassenger(Level level, BlockPos worldPos,
                                           BlockState state, Entity passenger) {
        return ContraptionSeatSupport.restorePassenger(level, worldPos, state, passenger);
    }
}
