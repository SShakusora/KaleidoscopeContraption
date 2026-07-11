package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

abstract class SyncedMovingInteractionBehaviour extends MovingInteractionBehaviour {
    @Override
    protected final void setContraptionBlockData(AbstractContraptionEntity contraptionEntity, BlockPos pos,
                                                 StructureTemplate.StructureBlockInfo info) {
        if (contraptionEntity.level().isClientSide) {
            return;
        }
        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, pos, info);
    }

    protected final void setContraptionBlockDataLocally(AbstractContraptionEntity contraptionEntity, BlockPos pos,
                                                        StructureTemplate.StructureBlockInfo info) {
        if (contraptionEntity.level().isClientSide) {
            return;
        }
        ContraptionInteractionUtil.updateContraptionDataLocally(contraptionEntity, pos, info);
    }
}
