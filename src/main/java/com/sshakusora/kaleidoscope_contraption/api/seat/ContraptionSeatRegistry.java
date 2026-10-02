package com.sshakusora.kaleidoscope_contraption.api.seat;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction.ContraptionSeatMovingInteraction;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.movement.ContraptionSeatMovementBehaviour;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/** Unified registry for Cookery-, Tavern-, and future entity-backed seats. */
public final class ContraptionSeatRegistry {
    private static final Map<Block, ContraptionSeatDefinition> PROVIDERS = new IdentityHashMap<>();
    private static final Map<Block, ContraptionSeatDefinition> DEFAULTS = new IdentityHashMap<>();

    private ContraptionSeatRegistry() {
    }

    /**
     * Registers an addon-defined provider. Explicit registrations take
     * precedence over built-in definitions for the same block.
     */
    public static synchronized void register(Block block, ResourceLocation backend,
                                              ContraptionSeatProvider provider) {
        Objects.requireNonNull(block, "block");
        ContraptionSeatDefinition definition = new ContraptionSeatDefinition(backend, provider);
        if (PROVIDERS.containsKey(block)) {
            throw new IllegalArgumentException("Duplicate contraption seat provider for block: " + block);
        }
        PROVIDERS.put(block, definition);
    }

    /** Registers a KC-owned built-in provider without blocking addon overrides. */
    public static synchronized void registerDefault(Block block, ResourceLocation backend,
                                                     ContraptionSeatProvider provider) {
        Objects.requireNonNull(block, "block");
        ContraptionSeatDefinition definition = new ContraptionSeatDefinition(backend, provider);
        if (DEFAULTS.containsKey(block)) {
            throw new IllegalArgumentException("Duplicate default contraption seat provider for block: " + block);
        }
        DEFAULTS.put(block, definition);
    }

    /** Removes an explicit provider; built-in defaults remain available. */
    public static synchronized boolean unregister(Block block) {
        return PROVIDERS.remove(Objects.requireNonNull(block, "block")) != null;
    }

    /** Returns an explicit provider definition, if one was registered. */
    public static synchronized ContraptionSeatDefinition getRegistered(Block block) {
        return PROVIDERS.get(Objects.requireNonNull(block, "block"));
    }

    /**
     * Resolves a state only when its backend is currently loaded. This keeps
     * the base mod safe when either optional integration is absent.
     */
    public static synchronized ContraptionSeatDefinition resolve(BlockState state) {
        if (state == null) {
            return null;
        }

        Block block = state.getBlock();
        ContraptionSeatDefinition definition = null;
        if (block instanceof ContraptionSeat direct) {
            definition = new ContraptionSeatDefinition(direct.getSeatBackend(), direct);
        }
        if (definition == null) {
            definition = PROVIDERS.get(block);
        }
        if (definition == null) {
            definition = DEFAULTS.get(block);
        }
        return definition != null && ContraptionSeatBackends.get(definition.backend()) != null
                ? definition : null;
    }

    /** Registers the standard generic Create movement and interaction behaviours. */
    public static void registerStandardBehaviours(Block block) {
        Objects.requireNonNull(block, "block");
        ContraptionSeatDefinition definition = getDefinition(block);
        if (definition == null) {
            throw new IllegalArgumentException("No contraption seat provider registered for block: " + block);
        }
        registerStandardBehaviours(block, definition.backend());
    }

    /** Registers the standard generic Create movement and interaction behaviours. */
    public static void registerStandardBehaviours(Block block, ResourceLocation backend) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(backend, "backend");
        ContraptionSeatDefinition definition = getDefinition(block);
        if (definition == null || !backend.equals(definition.backend())) {
            throw new IllegalArgumentException(
                    "Seat backend does not match the registered provider for block: " + block);
        }
        if (ContraptionSeatBackends.get(backend) == null) {
            throw new IllegalStateException("Contraption seat backend is not loaded: " + backend);
        }
        MovementBehaviour.REGISTRY.register(block, new ContraptionSeatMovementBehaviour());
        MovingInteractionBehaviour.REGISTRY.register(
                block, new ContraptionSeatMovingInteraction(backend));
    }

    private static synchronized ContraptionSeatDefinition getDefinition(Block block) {
        if (block instanceof ContraptionSeat direct) {
            return new ContraptionSeatDefinition(direct.getSeatBackend(), direct);
        }
        ContraptionSeatDefinition definition = PROVIDERS.get(block);
        return definition != null ? definition : DEFAULTS.get(block);
    }
}
