package com.sshakusora.kaleidoscope_contraption.content.behaviour.placement;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.ChoppingBoardBlock;
import com.sshakusora.kaleidoscope_contraption.api.placement.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

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

    @Override
    public Optional<ContraptionRemovalResult> createRemoval(ContraptionRemovalContext context) {
        BlockState state = context.targetInfo().state();
        if (!(state.getBlock() instanceof ChoppingBoardBlock)) {
            return Optional.empty();
        }

        CompoundTag nbt = context.targetInfo().nbt();
        if (nbt != null && nbt.contains("CurrentCutStack")
                && !ItemStack.parseOptional(context.contraptionEntity().level().registryAccess(),
                nbt.getCompound("CurrentCutStack")).isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(ContraptionRemovalResult.single(
                context.targetPos(), new ItemStack(state.getBlock().asItem())));
    }
}
