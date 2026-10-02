package com.sshakusora.kaleidoscope_contraption.api.seat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

/**
 * Entity-specific part of the unified seat API.
 *
 * <p>Backends are supplied by KC's optional Cookery and Tavern integrations;
 * addon authors normally only select one of the built-in backend IDs.</p>
 */
public interface ContraptionSeatBackend {
    /** Finds the passenger of a static seat entity in the supplied block bounds. */
    Optional<Entity> findPassenger(Level level, AABB bounds);

    /** Creates the backend's static seat entity at the requested base height. */
    Entity createSeatEntity(Level level, BlockPos pos, double seatHeight);

    /** Offset matching the backend entity's passenger riding position. */
    default double getPassengerOffset() {
        return -0.25;
    }

    /**
     * Gives a backend a chance to reject restoration when its supporting block
     * contract is not satisfied, such as a missing sittable tag.
     */
    default boolean isValidSupportingBlock(BlockState state) {
        return true;
    }

    /** Sets the entity rotation before it is added to the level. */
    default void setSeatFacing(Entity seatEntity, Direction facing) {
        seatEntity.setYRot(facing.toYRot());
    }

    /** Backend-specific bookkeeping after the passenger has started riding. */
    default void onSeatEntityRestored(SeatRestoreContext context) {
    }
}
