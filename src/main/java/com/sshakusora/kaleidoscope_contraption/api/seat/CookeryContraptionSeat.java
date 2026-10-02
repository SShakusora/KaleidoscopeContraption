package com.sshakusora.kaleidoscope_contraption.api.seat;

import net.minecraft.resources.ResourceLocation;

/**
 * Describes the geometry needed to use a block as a Cookery seat on a
 * Create contraption.
 *
 * <p>A block may implement this interface directly, or an equivalent
 * provider may be registered in {@link CookeryContraptionSeatRegistry}. The
 * state is passed to both methods so a block can vary its seat geometry by
 * state property.</p>
 */
public interface CookeryContraptionSeat extends ContraptionSeat {
    /** Keeps existing direct implementations on the Cookery backend. */
    @Override
    default ResourceLocation getSeatBackend() {
        return ContraptionSeatBackends.COOKERY;
    }
}
