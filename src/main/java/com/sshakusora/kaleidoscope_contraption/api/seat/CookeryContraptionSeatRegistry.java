package com.sshakusora.kaleidoscope_contraption.api.seat;

import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;
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
    private static final Map<Block, CookeryContraptionSeat> PROVIDERS = new HashMap<>();

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
        if (PROVIDERS.containsKey(block)) {
            throw new IllegalArgumentException("Duplicate Cookery contraption seat provider for block: " + block);
        }
        PROVIDERS.put(block, provider);
    }

    /**
     * Removes a provider, primarily for integration teardown and tests.
     */
    public static synchronized boolean unregister(Block block) {
        return PROVIDERS.remove(Objects.requireNonNull(block, "block")) != null;
    }

    /**
     * Returns the provider registered for the exact block instance, if any.
     */
    public static synchronized CookeryContraptionSeat get(Block block) {
        return PROVIDERS.get(block);
    }
}
