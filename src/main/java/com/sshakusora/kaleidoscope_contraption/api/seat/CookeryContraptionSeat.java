package com.sshakusora.kaleidoscope_contraption.api.seat;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Describes the geometry needed to use a block as a Cookery seat on a
 * Create contraption.
 *
 * <p>A block may implement this interface directly, or an equivalent
 * provider may be registered in {@link CookeryContraptionSeatRegistry}. The
 * state is passed to both methods so a block can vary its seat geometry by
 * state property.</p>
 */
public interface CookeryContraptionSeat {

    /**
     * Returns the SitEntity base height measured from the bottom of the
     * block.
     */
    double getSeatHeight(BlockState state);

    /**
     * Returns the facing used when a moving passenger is restored to a
     * static Cookery SitEntity.
     *
     * <p>Blocks with a horizontal facing property get that value by default;
     * non-directional blocks use south as a stable fallback.</p>
     */
    default Direction getSeatFacing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.SOUTH;
    }
}
