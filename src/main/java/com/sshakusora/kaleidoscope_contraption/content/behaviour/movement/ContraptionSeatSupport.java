package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.api.seat.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.*;

/** Shared seat geometry and Create seat-index lifecycle for every backend. */
public final class ContraptionSeatSupport {
    private ContraptionSeatSupport() {
    }

    public static boolean isSeat(BlockState state) {
        return ContraptionSeatRegistry.resolve(state) != null;
    }

    public static boolean isSeat(BlockState state, net.minecraft.resources.ResourceLocation backend) {
        ContraptionSeatDefinition definition = ContraptionSeatRegistry.resolve(state);
        return definition != null && definition.backend().equals(backend);
    }

    public static double getSeatHeight(BlockState state) {
        return requireDefinition(state).provider().getSeatHeight(state);
    }

    public static Vec3 getSeatEntityPosition(AbstractContraptionEntity contraptionEntity,
                                              BlockPos localPos, float partialTicks) {
        if (contraptionEntity.getContraption() == null) {
            return null;
        }

        StructureTemplate.StructureBlockInfo info =
                contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !isSeat(info.state())) {
            return null;
        }

        Vec3 localCenter = Vec3.atCenterOf(localPos);
        return contraptionEntity.toGlobalVector(
                localCenter.add(0, getSeatHeight(info.state()) - 0.5, 0), partialTicks);
    }

    /** Returns the position produced by the backend's static seat entity. */
    public static Vec3 getPassengerPosition(AbstractContraptionEntity contraptionEntity,
                                             Entity passenger, float partialTicks) {
        if (contraptionEntity.getContraption() == null) {
            return null;
        }

        BlockPos localPos = contraptionEntity.getContraption().getSeatOf(passenger.getUUID());
        if (localPos == null) {
            return null;
        }

        StructureTemplate.StructureBlockInfo info =
                contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return null;
        }
        ContraptionSeatDefinition definition = ContraptionSeatRegistry.resolve(info.state());
        if (definition == null) {
            return null;
        }
        ContraptionSeatBackend backend = ContraptionSeatBackends.get(definition.backend());
        if (backend == null) {
            return null;
        }

        Vec3 seatEntityPosition = getSeatEntityPosition(contraptionEntity, localPos, partialTicks);
        if (seatEntityPosition == null) {
            return null;
        }

        return seatEntityPosition.add(0,
                backend.getPassengerOffset() - passenger.getVehicleAttachmentPoint(contraptionEntity).y,
                0);
    }

    /** Finds a passenger on a static backend entity before Create captures the block. */
    public static Optional<Entity> findStaticPassenger(Level level, BlockPos worldPos,
                                                       BlockState state) {
        ContraptionSeatDefinition definition = ContraptionSeatRegistry.resolve(state);
        if (definition == null) {
            return Optional.empty();
        }
        ContraptionSeatBackend backend = ContraptionSeatBackends.get(definition.backend());
        return backend == null
                ? Optional.empty()
                : backend.findPassenger(level, new AABB(worldPos));
    }

    /** Ejects occupants before an entity-backed seat block is removed. */
    public static void ejectPassengers(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        Contraption contraption = contraptionEntity.getContraption();
        int seatIndex = contraption.getSeats().indexOf(localPos);
        if (seatIndex < 0) {
            return;
        }

        List<Entity> passengers = new ArrayList<>(contraptionEntity.getPassengers());
        Map<UUID, Integer> seatMapping = contraption.getSeatMapping();
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

    /** Removes one seat and adjusts every remaining Create seat index. */
    public static void removeSeat(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        Contraption contraption = contraptionEntity.getContraption();
        int removedIndex = contraption.getSeats().indexOf(localPos);
        if (removedIndex < 0) {
            return;
        }

        contraption.getSeats().remove(removedIndex);
        Iterator<Map.Entry<UUID, Integer>> iterator =
                contraption.getSeatMapping().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
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

    /** Restores a passenger to the correct backend's static seat entity. */
    public static boolean restorePassenger(Level level, BlockPos worldPos,
                                           BlockState state, Entity passenger) {
        if (level.isClientSide || ContraptionSeatRegistry.resolve(state) == null) {
            return false;
        }

        BlockState placedState = level.getBlockState(worldPos);
        ContraptionSeatDefinition definition = ContraptionSeatRegistry.resolve(placedState);
        if (definition == null) {
            return false;
        }
        ContraptionSeatBackend backend = ContraptionSeatBackends.get(definition.backend());
        if (backend == null || !backend.isValidSupportingBlock(placedState)) {
            return false;
        }

        ContraptionSeatProvider provider = definition.provider();
        Entity seatEntity = backend.createSeatEntity(
                level, worldPos, provider.getSeatHeight(placedState));
        backend.setSeatFacing(seatEntity, provider.getSeatFacing(placedState));

        if (!level.addFreshEntity(seatEntity)) {
            return false;
        }

        passenger.stopRiding();
        if (!passenger.startRiding(seatEntity, true)) {
            seatEntity.discard();
            return false;
        }

        passenger.getPersistentData().remove("ContraptionDismountLocation");
        SeatRestoreContext context =
                new SeatRestoreContext(level, worldPos, placedState, passenger, seatEntity);
        provider.onSeatEntityRestored(context);
        backend.onSeatEntityRestored(context);
        return true;
    }

    private static ContraptionSeatDefinition requireDefinition(BlockState state) {
        ContraptionSeatDefinition definition = ContraptionSeatRegistry.resolve(state);
        if (definition == null) {
            throw new IllegalArgumentException("Not a registered contraption seat: " + state);
        }
        return definition;
    }
}
