package com.sshakusora.kaleidoscope_contraption.api.placement;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public record ContraptionPlacementContext(
        ResourceLocation pointType,
        Player player,
        InteractionHand hand,
        ItemStack heldItem,
        BlockPos supportPos,
        BlockPos targetPos,
        StructureTemplate.StructureBlockInfo supportInfo,
        AbstractContraptionEntity contraptionEntity
) {
}
