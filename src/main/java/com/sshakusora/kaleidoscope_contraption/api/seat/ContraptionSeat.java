package com.sshakusora.kaleidoscope_contraption.api.seat;

import net.minecraft.resources.ResourceLocation;

/**
 * Optional direct implementation form of the unified contraption seat API.
 * Addons that do not want their block class to depend on this interface can
 * use {@link ContraptionSeatRegistry#register} instead.
 */
public interface ContraptionSeat extends ContraptionSeatProvider {
    /** Identifies the entity/lifecycle backend used by this seat. */
    ResourceLocation getSeatBackend();
}
