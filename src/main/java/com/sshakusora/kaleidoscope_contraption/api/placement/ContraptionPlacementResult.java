package com.sshakusora.kaleidoscope_contraption.api.placement;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;

public record ContraptionPlacementResult(
        List<StructureTemplate.StructureBlockInfo> blocks,
        int consumedItems,
        boolean playPlaceSound
) {
    public ContraptionPlacementResult {
        blocks = List.copyOf(blocks);
        if (blocks.isEmpty()) {
            throw new IllegalArgumentException("A placement result must contain at least one block");
        }
        if (consumedItems < 0) {
            throw new IllegalArgumentException("consumedItems cannot be negative");
        }
    }

    public static ContraptionPlacementResult single(StructureTemplate.StructureBlockInfo block) {
        return new ContraptionPlacementResult(List.of(block), 1, true);
    }
}
