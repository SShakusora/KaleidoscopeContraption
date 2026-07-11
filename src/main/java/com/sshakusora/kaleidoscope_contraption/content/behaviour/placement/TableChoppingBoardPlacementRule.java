package com.sshakusora.kaleidoscope_contraption.content.behaviour.placement;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.ChoppingBoardBlock;
import com.sshakusora.kaleidoscope_contraption.api.placement.ContraptionPlacementContext;
import com.sshakusora.kaleidoscope_contraption.api.placement.ContraptionPlacementResult;
import com.sshakusora.kaleidoscope_contraption.api.placement.ContraptionPlacementRule;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class TableChoppingBoardPlacementRule implements ContraptionPlacementRule {
    @Override
    public boolean matches(ContraptionPlacementContext context) {
        return context.player().isShiftKeyDown()
                && Block.byItem(context.heldItem().getItem()) instanceof ChoppingBoardBlock;
    }

    @Override
    public ContraptionPlacementResult createPlacement(ContraptionPlacementContext context) {
        ChoppingBoardBlock block = (ChoppingBoardBlock) Block.byItem(context.heldItem().getItem());
        BlockState state = block.defaultBlockState()
                .setValue(ChoppingBoardBlock.FACING, context.player().getDirection().getOpposite());
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("MaxCutCount", 0);
        nbt.putInt("CurrentCutCount", 0);
        nbt.put("CurrentCutStack", ItemStack.EMPTY.saveOptional(context.contraptionEntity().level().registryAccess()));
        nbt.put("ResultItem", ItemStack.EMPTY.saveOptional(context.contraptionEntity().level().registryAccess()));
        nbt.putString("id", "kaleidoscope_cookery:chopping_board");
        return ContraptionPlacementResult.single(new StructureTemplate.StructureBlockInfo(
                context.targetPos(), state, nbt));
    }
}
