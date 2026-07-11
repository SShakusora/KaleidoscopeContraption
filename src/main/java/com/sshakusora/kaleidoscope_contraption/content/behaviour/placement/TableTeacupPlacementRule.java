package com.sshakusora.kaleidoscope_contraption.content.behaviour.placement;

import com.github.ysbbbbbb.kaleidoscopecookery.block.drink.EmptyCupBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.drink.TeacupBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.item.EmptyCupItem;
import com.github.ysbbbbbb.kaleidoscopecookery.item.TeacupItem;
import com.sshakusora.kaleidoscope_contraption.api.placement.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TableTeacupPlacementRule implements ContraptionPlacementRule {
    @Override
    public boolean matches(ContraptionPlacementContext context) {
        return context.player().isShiftKeyDown()
                && (context.heldItem().getItem() instanceof TeacupItem
                || context.heldItem().getItem() instanceof EmptyCupItem);
    }

    @Override
    public ContraptionPlacementResult createPlacement(ContraptionPlacementContext context) {
        Block block = ((BlockItem) context.heldItem().getItem()).getBlock();
        BlockState state = block.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING,
                        context.player().getDirection().getOpposite());
        return ContraptionPlacementResult.single(new StructureTemplate.StructureBlockInfo(
                context.targetPos(), state, null));
    }

    @Override
    public Optional<ContraptionRemovalResult> createRemoval(ContraptionRemovalContext context) {
        BlockState state = context.targetInfo().state();
        List<ItemStack> returnedItems = new ArrayList<>();
        if (state.getBlock() instanceof EmptyCupBlock) {
            returnedItems.add(new ItemStack(state.getBlock().asItem(), state.getValue(EmptyCupBlock.CUP_COUNT)));
        } else if (state.getBlock() instanceof TeacupBlock teacup) {
            int cups = state.getValue(teacup.getCupCountProperty());
            int tea = state.getValue(teacup.getTeaCountProperty());
            if (cups > tea) {
                returnedItems.add(new ItemStack(ModItems.EMPTY_CUP.get(),
                        cups - tea));
            }
            if (tea > 0) {
                returnedItems.add(new ItemStack(state.getBlock().asItem(), tea));
            }
        } else {
            return Optional.empty();
        }
        return Optional.of(new ContraptionRemovalResult(
                List.of(context.targetPos()), List.of(), returnedItems, true));
    }
}
