package com.sshakusora.kaleidoscope_contraption.api.seat;

import net.minecraft.world.level.block.Block;

import java.util.Objects;

/**
 * Optional provider registry for Cookery-compatible seats.
 *
 * <p>This is useful when an addon wants to keep its block class independent
 * of Kaleidoscope Contraption and register compatibility from a separate
 * integration class. Direct interface implementations take precedence over
 * entries in this registry.</p>
 */
public final class CookeryContraptionSeatRegistry {
    private CookeryContraptionSeatRegistry() {
    }

    /**
     * Registers the provider used for the given block.
     *
     * @throws NullPointerException if the block or provider is null
     * @throws IllegalArgumentException if the block already has a provider
     */
    public static synchronized void register(Block block, CookeryContraptionSeat provider) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(provider, "provider");
        ContraptionSeatRegistry.register(block, ContraptionSeatBackends.COOKERY, provider);
    }

    /**
     * Removes a provider, primarily for integration teardown and tests.
     */
    public static synchronized boolean unregister(Block block) {
        return ContraptionSeatRegistry.unregister(Objects.requireNonNull(block, "block"));
    }

    /**
     * Returns the provider registered for the exact block instance, if any.
     */
    public static synchronized CookeryContraptionSeat get(Block block) {
        ContraptionSeatDefinition definition = ContraptionSeatRegistry.getRegistered(block);
        if (definition == null || !ContraptionSeatBackends.COOKERY.equals(definition.backend())) {
            return null;
        }
        return definition.provider() instanceof CookeryContraptionSeat provider ? provider : null;
    }
}
