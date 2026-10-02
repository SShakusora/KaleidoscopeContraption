package com.sshakusora.kaleidoscope_contraption.api.seat;

import com.sshakusora.kaleidoscope_contraption.KaleidoscopeContraption;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Registry of entity/lifecycle backends used by the unified seat API. */
public final class ContraptionSeatBackends {
    public static final ResourceLocation COOKERY =
            KaleidoscopeContraption.asResource("cookery");
    public static final ResourceLocation TAVERN =
            KaleidoscopeContraption.asResource("tavern");

    private static final Map<ResourceLocation, ContraptionSeatBackend> BACKENDS = new HashMap<>();

    private ContraptionSeatBackends() {
    }

    /** Registers an optional integration backend. */
    public static synchronized void register(ResourceLocation id, ContraptionSeatBackend backend) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(backend, "backend");
        if (BACKENDS.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate contraption seat backend: " + id);
        }
        BACKENDS.put(id, backend);
    }

    /** Returns a loaded backend, or {@code null} when its optional mod is absent. */
    public static synchronized ContraptionSeatBackend get(ResourceLocation id) {
        return BACKENDS.get(Objects.requireNonNull(id, "id"));
    }
}
