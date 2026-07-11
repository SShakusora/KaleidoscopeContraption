package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

abstract class BlockEntityDelegatingMovingInteraction extends MovingInteractionBehaviour {
    protected <T extends BlockEntity> T loadBlockEntity(T blockEntity, CompoundTag tag,
                                                         AbstractContraptionEntity contraptionEntity) {
        blockEntity.setLevel(contraptionEntity.level());
        blockEntity.loadWithComponents(tag == null ? new CompoundTag() : tag.copy(),
                contraptionEntity.level().registryAccess());
        return blockEntity;
    }

    protected void saveBlockEntity(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                   StructureTemplate.StructureBlockInfo info, BlockEntity blockEntity) {
        CompoundTag tag = blockEntity.saveWithFullMetadata(contraptionEntity.level().registryAccess());
        tag.remove("x");
        tag.remove("y");
        tag.remove("z");
        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos,
                new StructureTemplate.StructureBlockInfo(info.pos(), info.state(), tag));
    }
}
