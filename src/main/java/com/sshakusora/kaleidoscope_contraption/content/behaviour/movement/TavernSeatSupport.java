package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.BarStoolBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.SofaBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.deco.BarStoolBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.entity.SitEntity;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import org.apache.commons.lang3.tuple.MutablePair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Shared seat geometry and lifecycle helpers for Tavern seats. */
public final class TavernSeatSupport {
    private static final double SOFA_SEAT_HEIGHT = 0.5125;
    private static final double BAR_STOOL_SEAT_HEIGHT = 0.875;
    private static final double TAVERN_SIT_ENTITY_OFFSET = -0.25;

    private TavernSeatSupport() {
    }

    public static boolean isTavernSeat(BlockState state) {
        return state != null && (state.getBlock() instanceof SofaBlock
                || state.getBlock() instanceof BarStoolBlock);
    }

    public static double getSeatHeight(BlockState state) {
        if (state.getBlock() instanceof SofaBlock) {
            return SOFA_SEAT_HEIGHT;
        }
        if (state.getBlock() instanceof BarStoolBlock) {
            return BAR_STOOL_SEAT_HEIGHT;
        }
        throw new IllegalArgumentException("Not a Tavern seat: " + state);
    }

    /** Returns the position used by Tavern's static SitEntity, transformed into world space. */
    public static Vec3 getSeatEntityPosition(AbstractContraptionEntity contraptionEntity,
                                              BlockPos localPos, float partialTicks) {
        if (contraptionEntity.getContraption() == null) {
            return null;
        }

        StructureTemplate.StructureBlockInfo info =
                contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !isTavernSeat(info.state())) {
            return null;
        }

        Vec3 localCenter = Vec3.atCenterOf(localPos);
        return contraptionEntity.toGlobalVector(
                localCenter.add(0, getSeatHeight(info.state()) - 0.5, 0), partialTicks);
    }

    /** Matches the final passenger height produced by Tavern's SitEntity. */
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
                TAVERN_SIT_ENTITY_OFFSET + passenger.getMyRidingOffset(), 0);
    }

    /** Ejects occupants before a Tavern seat block is removed from a moving Contraption. */
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

    /** Removes a Tavern seat and keeps every remaining Create seat index valid. */
    public static void removeSeat(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        Contraption contraption = contraptionEntity.getContraption();
        int removedIndex = contraption.getSeats().indexOf(localPos);
        if (removedIndex < 0) {
            return;
        }

        contraption.getSeats().remove(removedIndex);

        Iterator<Map.Entry<UUID, Integer>> mappingIterator =
                contraption.getSeatMapping().entrySet().iterator();
        while (mappingIterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = mappingIterator.next();
            Integer seatIndex = entry.getValue();
            if (seatIndex == null || seatIndex == removedIndex) {
                mappingIterator.remove();
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

    /** Restores one moving passenger to Tavern's normal static seat entity after disassembly. */
    public static boolean restorePassenger(Level level, BlockPos worldPos,
                                           BlockState state, Entity passenger) {
        if (level.isClientSide || !isTavernSeat(state)) {
            return false;
        }

        BlockState placedState = level.getBlockState(worldPos);
        if (!isTavernSeat(placedState)) {
            return false;
        }
        state = placedState;

        SitEntity sitEntity = new SitEntity(level, worldPos, getSeatHeight(state));
        Direction facing = state.getBlock() instanceof SofaBlock
                ? state.getValue(SofaBlock.FACING)
                : state.getValue(BarStoolBlock.FACING);
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
        if (level.getBlockEntity(worldPos) instanceof BarStoolBlockEntity barStool) {
            barStool.setSitEntity(sitEntity);
        }
        return true;
    }
}
