package com.sshakusora.kaleidoscope_contraption.api.seat;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Resolved provider/backend pair for one block. */
public record ContraptionSeatDefinition(
        ResourceLocation backend,
        ContraptionSeatProvider provider
) {
    public ContraptionSeatDefinition {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(provider, "provider");
    }
}
