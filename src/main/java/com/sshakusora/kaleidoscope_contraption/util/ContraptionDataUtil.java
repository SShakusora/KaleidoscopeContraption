package com.sshakusora.kaleidoscope_contraption.util;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.mixin.accessor.ContraptionAccessor;
import com.sshakusora.kaleidoscope_contraption.network.KCContraptionChangedPacket;
import com.sshakusora.kaleidoscope_contraption.network.KCPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.commons.lang3.tuple.MutablePair;

public class ContraptionDataUtil {
    public static void setContraptionActorData(AbstractContraptionEntity contraptionEntity, int index,
                                         StructureTemplate.StructureBlockInfo info, MovementContext ctx) {
        contraptionEntity.getContraption().getActors().set(index, MutablePair.of(info, ctx));
        if (contraptionEntity.level().isClientSide)
            contraptionEntity.getContraption()
                    .invalidateClientContraptionChildren();
    }

    public static void setContraptionActorData(Contraption contraption, int index,
                                               StructureTemplate.StructureBlockInfo info, MovementContext ctx) {
        setContraptionActorData(contraption.entity, index, info, ctx);
    }

    public static void setContraptionActorData(int index,
                                               StructureTemplate.StructureBlockInfo info, MovementContext ctx) {
        setContraptionActorData(ctx.contraption, index, info, ctx);
    }

    public static void updateContraptionData(MovementContext context, BlockState state, CompoundTag newNbt) {
        updateContraptionData(context, state, newNbt, true);
    }

    /**
     * 更新Contraption数据
     * @param needSync 是否需要同步到客户端
     */
    public static void updateContraptionData(MovementContext context, BlockState state, CompoundTag newNbt, boolean needSync) {
        StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                context.localPos, state, newNbt);

        // 更新blocks
        context.contraption.getBlocks().put(context.localPos, newInfo);

        // 更新updateTags
        ((ContraptionAccessor) context.contraption).getUpdateTags().put(context.localPos, newNbt);

        // 更新actors列表
        var actors = context.contraption.getActors();
        for (int i = 0; i < actors.size(); i++) {
            MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor = actors.get(i);
            if (actor.getLeft().pos().equals(context.localPos)) {
                ContraptionDataUtil.setContraptionActorData(i, newInfo, context);
                break;
            }
        }

        // 发送数据包同步到客户端
        if (needSync && !context.world.isClientSide && context.contraption.entity != null) {
            KCPacketHandler.sendToTracking(
                    new KCContraptionChangedPacket(
                            context.contraption.entity.getId(),
                            context.localPos,
                            state,
                            newNbt
                    ),
                    context.contraption.entity
            );
        }
    }
}
