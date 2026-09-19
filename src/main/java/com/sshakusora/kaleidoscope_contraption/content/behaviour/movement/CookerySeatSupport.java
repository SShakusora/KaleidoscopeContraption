package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.ChairBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.CookStoolBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.LongBenchBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.entity.SitEntity;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Shared seat geometry and lifecycle helpers for Cookery's SitEntity-backed seats. */
public final class CookerySeatSupport {
    private static final double CHAIR_SEAT_HEIGHT = 0.5125;
    private static final double STOOL_SEAT_HEIGHT = 0.4375;
    private static final double BENCH_SEAT_HEIGHT = 0.5;
    private static final double SIT_ENTITY_PASSENGER_OFFSET = -0.25;

    private CookerySeatSupport() {
    }

    public static boolean isCookerySeat(BlockState state) {
        return state != null && (state.getBlock() instanceof ChairBlock
                || state.getBlock() instanceof CookStoolBlock
                || state.getBlock() instanceof LongBenchBlock);
    }

    public static double getSeatHeight(BlockState state) {
        if (state.getBlock() instanceof ChairBlock) {
            return CHAIR_SEAT_HEIGHT;
        }
        if (state.getBlock() instanceof CookStoolBlock) {
            return STOOL_SEAT_HEIGHT;
        }
        if (state.getBlock() instanceof LongBenchBlock) {
            return BENCH_SEAT_HEIGHT;
        }
        throw new IllegalArgumentException("Not a Cookery seat: " + state);
    }

    public static Vec3 getSeatEntityPosition(AbstractContraptionEntity contraptionEntity,
                                              BlockPos localPos, float partialTicks) {
        if (contraptionEntity.getContraption() == null) {
            return null;
        }
        StructureTemplate.StructureBlockInfo info =
                contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !isCookerySeat(info.state())) {
            return null;
        }
        return contraptionEntity.toGlobalVector(
                Vec3.atCenterOf(localPos).add(0, getSeatHeight(info.state()) - 0.5, 0), partialTicks);
    }

    public static Vec3 getPassengerPosition(AbstractContraptionEntity contraptionEntity,
                                             Entity passenger, float partialTicks) {
        if (contraptionEntity.getContraption() == null) {
            return null;
        }
        BlockPos localPos = contraptionEntity.getContraption().getSeatOf(passenger.getUUID());
        if (localPos == null) {
            return null;
        }
        Vec3 seatEntityPosition = getSeatEntityPosition(contraptionEntity, localPos, partialTicks);
        if (seatEntityPosition == null) {
            return null;
        }
        return seatEntityPosition.add(0,
                SIT_ENTITY_PASSENGER_OFFSET - passenger.getVehicleAttachmentPoint(contraptionEntity).y,
                0);
    }

    public static void ejectPassengers(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        Contraption contraption = contraptionEntity.getContraption();
        int seatIndex = contraption.getSeats().indexOf(localPos);
        if (seatIndex < 0) {
            return;
        }

        List<Entity> passengers = new ArrayList<>(contraptionEntity.getPassengers());
        Map<java.util.UUID, Integer> seatMapping = contraption.getSeatMapping();
        for (Entity passenger : passengers) {
            if (!Integer.valueOf(seatIndex).equals(seatMapping.get(passenger.getUUID()))) {
                continue;
            }
            Vec3 position = getPassengerPosition(contraptionEntity, passenger, 1.0F);
            passenger.stopRiding();
            seatMapping.remove(passenger.getUUID());
            if (position != null) {
                passenger.teleportTo(position.x, position.y, position.z);
            }
            passenger.getPersistentData().remove("ContraptionDismountLocation");
        }
    }

    /** Removes a Cookery seat and keeps all remaining Create seat indexes valid. */
    public static void removeSeat(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        Contraption contraption = contraptionEntity.getContraption();
        int removedIndex = contraption.getSeats().indexOf(localPos);
        if (removedIndex < 0) {
            return;
        }

        contraption.getSeats().remove(removedIndex);
        Iterator<Map.Entry<java.util.UUID, Integer>> iterator =
                contraption.getSeatMapping().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<java.util.UUID, Integer> entry = iterator.next();
            Integer seatIndex = entry.getValue();
            if (seatIndex == null || seatIndex == removedIndex) {
                iterator.remove();
            } else if (seatIndex > removedIndex) {
                entry.setValue(seatIndex - 1);
            }
        }

        for (MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor : contraption.getActors()) {
            MovementContext context = actor.getRight();
            if (context == null || !context.data.contains("SeatIndex")) {
                continue;
            }
            int seatIndex = context.data.getInt("SeatIndex");
            if (seatIndex == removedIndex) {
                context.data.putInt("SeatIndex", -1);
            } else if (seatIndex > removedIndex) {
                context.data.putInt("SeatIndex", seatIndex - 1);
            }
        }
    }

    /** Restores one passenger to Cookery's normal static SitEntity after disassembly. */
    public static boolean restorePassenger(Level level, BlockPos worldPos,
                                           BlockState state, Entity passenger) {
        if (level.isClientSide || !isCookerySeat(state)) {
            return false;
        }
        BlockState placedState = level.getBlockState(worldPos);
        if (!isCookerySeat(placedState)) {
            return false;
        }

        SitEntity sitEntity = new SitEntity(level, worldPos, getSeatHeight(placedState));
        Direction facing = getSeatFacing(placedState);
        sitEntity.setYRot(facing.toYRot());
        if (!level.addFreshEntity(sitEntity)) {
            return false;
        }

        passenger.stopRiding();
        if (!passenger.startRiding(sitEntity, true)) {
            sitEntity.discard();
            return false;
        }
        passenger.getPersistentData().remove("ContraptionDismountLocation");
        return true;
    }

    private static Direction getSeatFacing(BlockState state) {
        if (state.getBlock() instanceof ChairBlock) {
            return state.getValue(ChairBlock.FACING);
        }
        if (state.getBlock() instanceof CookStoolBlock) {
            return state.getValue(CookStoolBlock.FACING);
        }
        return state.getValue(LongBenchBlock.AXIS) == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
    }
}
