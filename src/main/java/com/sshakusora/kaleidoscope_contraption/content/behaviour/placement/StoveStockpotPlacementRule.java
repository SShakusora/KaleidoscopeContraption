package com.sshakusora.kaleidoscope_contraption.content.behaviour.placement;

import com.github.ysbbbbbb.kaleidoscopecookery.advancements.critereon.ModEventTriggerType;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.StockpotBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.StockpotRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModSoupBases;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModTrigger;
import com.sshakusora.kaleidoscope_contraption.api.placement.*;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

public class StoveStockpotPlacementRule implements ContraptionPlacementRule {
    @Override
    public boolean matches(ContraptionPlacementContext context) {
        return Block.byItem(context.heldItem().getItem()) instanceof StockpotBlock;
    }

    @Override
    public ContraptionPlacementResult createPlacement(ContraptionPlacementContext context) {
        StockpotBlock block = (StockpotBlock) Block.byItem(context.heldItem().getItem());
        BlockState state = block.defaultBlockState()
                .setValue(StockpotBlock.FACING, context.player().getDirection().getOpposite())
                .setValue(StockpotBlock.HAS_BASE, false);

        CompoundTag nbt = new CompoundTag();
        nbt.put("Inputs", ContainerHelper.saveAllItems(new CompoundTag(),
                NonNullList.withSize(StockpotRecipe.RECIPES_SIZE, ItemStack.EMPTY)));
        nbt.putString("RecipeId", "kaleidoscope_cookery:stockpot/empty");
        nbt.putString("SoupBaseId", ModSoupBases.WATER.toString());
        nbt.put("Result", ItemStack.EMPTY.serializeNBT());
        nbt.putInt("Status", 0);
        nbt.putInt("CurrentTick", -1);
        nbt.putInt("TakeoutCount", 0);
        nbt.putString("id", "kaleidoscope_cookery:stockpot");
        return single(context, state, nbt);
    }

    @Override
    public void afterPlaced(ContraptionPlacementContext context, ContraptionPlacementResult result) {
        if (isLit(context)) {
            ModTrigger.EVENT.trigger(context.player(), ModEventTriggerType.PLACE_STOCKPOT_ON_HEAT_SOURCE);
        }
    }

    @Override
    public Optional<ContraptionRemovalResult> createRemoval(ContraptionRemovalContext context) {
        BlockState state = context.targetInfo().state();
        if (!(state.getBlock() instanceof StockpotBlock) || state.getValue(StockpotBlock.HAS_LID)) {
            return Optional.empty();
        }

        CompoundTag nbt = context.targetInfo().nbt();
        int status = nbt == null ? 0 : nbt.getInt("Status");
        if (status != 0 && status != 1) {
            return Optional.empty();
        }
        if (status == 1 && !isContainerEmpty(nbt)) {
            return Optional.empty();
        }
        return Optional.of(ContraptionRemovalResult.single(
                context.targetPos(), new ItemStack(ModBlocks.STOCKPOT.get())));
    }

    private ContraptionPlacementResult single(ContraptionPlacementContext context, BlockState state,
                                              CompoundTag nbt) {
        return ContraptionPlacementResult.single(new StructureTemplate.StructureBlockInfo(
                context.targetPos(), state, nbt));
    }

    private boolean isLit(ContraptionPlacementContext context) {
        BlockState state = context.supportInfo().state();
        return state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
    }

    private boolean isContainerEmpty(CompoundTag nbt) {
        if (nbt == null || !nbt.contains("Inputs", Tag.TAG_COMPOUND)) {
            return true;
        }
        NonNullList<ItemStack> items = NonNullList.withSize(StockpotRecipe.RECIPES_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbt.getCompound("Inputs"), items);
        return items.stream().allMatch(ItemStack::isEmpty);
    }
}
