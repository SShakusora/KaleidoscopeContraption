package com.sshakusora.kaleidoscope_contraption.api.placement;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public record ContraptionRemovalContext(
        Player player,
        BlockPos targetPos,
        StructureTemplate.StructureBlockInfo targetInfo,
        AbstractContraptionEntity contraptionEntity
) {
}
