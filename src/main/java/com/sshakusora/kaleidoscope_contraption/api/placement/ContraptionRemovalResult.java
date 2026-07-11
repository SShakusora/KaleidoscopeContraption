package com.sshakusora.kaleidoscope_contraption.api.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;

public record ContraptionRemovalResult(
        List<BlockPos> positions,
        List<StructureTemplate.StructureBlockInfo> updatedBlocks,
        List<ItemStack> returnedItems,
        boolean playBreakSound
) {
    public ContraptionRemovalResult {
        positions = List.copyOf(positions);
        updatedBlocks = List.copyOf(updatedBlocks);
        returnedItems = List.copyOf(returnedItems);
        if (positions.isEmpty() && updatedBlocks.isEmpty()) {
            throw new IllegalArgumentException("A removal result must remove or update at least one block");
        }
    }

    public static ContraptionRemovalResult single(BlockPos position, ItemStack returnedItem) {
        List<ItemStack> returnedItems = returnedItem.isEmpty() ? List.of() : List.of(returnedItem);
        return new ContraptionRemovalResult(List.of(position), List.of(), returnedItems, true);
    }
}
