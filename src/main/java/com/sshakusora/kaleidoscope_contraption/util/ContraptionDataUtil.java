package com.sshakusora.kaleidoscope_contraption.util;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.commons.lang3.tuple.MutablePair;

public class ContraptionDataUtil {
    public static void setContraptionActorData(AbstractContraptionEntity contraptionEntity, int index,
                                         StructureTemplate.StructureBlockInfo info, MovementContext ctx) {
        contraptionEntity.getContraption().getActors().remove(index);
        contraptionEntity.getContraption().getActors().add(index, MutablePair.of(info, ctx));
        if (contraptionEntity.level().isClientSide)
            contraptionEntity.getContraption()
                    .invalidateClientContraptionChildren();
    }
}
