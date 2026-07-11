package com.sshakusora.kaleidoscope_contraption.api.placement;

import java.util.Optional;

public interface ContraptionPlacementRule {
    boolean matches(ContraptionPlacementContext context);

    ContraptionPlacementResult createPlacement(ContraptionPlacementContext context);

    default int priority() {
        return 0;
    }

    default void afterPlaced(ContraptionPlacementContext context, ContraptionPlacementResult result) {
    }

    default Optional<ContraptionRemovalResult> createRemoval(ContraptionRemovalContext context) {
        return Optional.empty();
    }

    default void afterRemoved(ContraptionRemovalContext context, ContraptionRemovalResult result) {
    }
}
