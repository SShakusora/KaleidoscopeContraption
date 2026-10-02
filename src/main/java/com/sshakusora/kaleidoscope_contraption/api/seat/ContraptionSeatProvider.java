package com.sshakusora.kaleidoscope_contraption.api.seat;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Describes the geometry of a block that can act as an entity-backed seat on
 * a Create contraption.
 *
 * <p>The provider intentionally does not mention Cookery or Tavern. The
 * entity implementation is selected by the backend passed to the registry,
 * which keeps both optional integrations behind one public API.</p>
 */
@FunctionalInterface
public interface ContraptionSeatProvider {
    /** Returns the seat entity base height measured from the bottom of a block. */
    double getSeatHeight(BlockState state);

    /** Returns the direction used when a passenger is restored to a static seat entity. */
    default Direction getSeatFacing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.SOUTH;
    }

    /**
     * Allows a provider-owned block entity to retain the newly restored seat
     * entity. Most seats do not need this hook.
     */
    default void onSeatEntityRestored(SeatRestoreContext context) {
    }
}
