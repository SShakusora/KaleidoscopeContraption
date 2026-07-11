package com.sshakusora.kaleidoscope_contraption.content.behaviour.placement;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.SteamerBlock;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.api.placement.ContraptionPlacementContext;
import com.sshakusora.kaleidoscope_contraption.api.placement.ContraptionPlacementResult;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Objects;

public class SteamerTopPlacementRule extends StoveSteamerPlacementRule {
    @Override
    public boolean matches(ContraptionPlacementContext context) {
        return Block.byItem(context.heldItem().getItem()) instanceof SteamerBlock
                && findTopSupport(context) != null;
    }

    @Override
    public ContraptionPlacementResult createPlacement(ContraptionPlacementContext context) {
        StructureTemplate.StructureBlockInfo top = Objects.requireNonNull(findTopSupport(context));
        ContraptionPlacementContext topContext = new ContraptionPlacementContext(
                context.pointType(), context.player(), context.hand(), context.heldItem(), top.pos(), top.pos().above(),
                top, context.contraptionEntity());
        ContraptionPlacementResult result = super.createPlacement(topContext);
        BlockState state = result.blocks().get(0).state()
                .setValue(SteamerBlock.FACING, top.state().getValue(SteamerBlock.FACING));
        StructureTemplate.StructureBlockInfo placed = new StructureTemplate.StructureBlockInfo(
                top.pos().above(), state, result.blocks().get(0).nbt());
        return ContraptionPlacementResult.single(placed);
    }

    @Override
    public void afterPlaced(ContraptionPlacementContext context, ContraptionPlacementResult result) {
        BlockPos supportPos = result.blocks().get(0).pos().below();
        AbstractContraptionEntity entity = context.contraptionEntity();
        StructureTemplate.StructureBlockInfo support = entity.getContraption().getBlocks().get(supportPos);
        if (support != null && support.state().getBlock() instanceof SteamerBlock) {
            BlockState state = support.state().setValue(SteamerBlock.HAS_LID, false);
            ContraptionInteractionUtil.updateContraptionData(entity, supportPos,
                    new StructureTemplate.StructureBlockInfo(support.pos(), state, support.nbt()));
        }
    }

    private StructureTemplate.StructureBlockInfo findTopSupport(ContraptionPlacementContext context) {
        AbstractContraptionEntity entity = context.contraptionEntity();
        StructureTemplate.StructureBlockInfo current = context.supportInfo();
        while (current != null && current.state().getBlock() instanceof SteamerBlock) {
            if (current.state().getValue(SteamerBlock.HALF)
                    || current.state().getValue(SteamerBlock.HAS_LID)) {
                return null;
            }
            StructureTemplate.StructureBlockInfo above = entity.getContraption().getBlocks().get(current.pos().above());
            if (above == null || above.state().isAir()) {
                return current;
            }
            if (!(above.state().getBlock() instanceof SteamerBlock)) {
                return null;
            }
            current = above;
        }
        return null;
    }
}
