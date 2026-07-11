package com.sshakusora.kaleidoscope_contraption.content.behaviour.placement;

import com.github.ysbbbbbb.kaleidoscopecookery.advancements.critereon.ModEventTriggerType;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.PotBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.PotRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModTrigger;
import com.sshakusora.kaleidoscope_contraption.api.placement.*;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

public class StovePotPlacementRule implements ContraptionPlacementRule {
    @Override
    public boolean matches(ContraptionPlacementContext context) {
        return Block.byItem(context.heldItem().getItem()) instanceof PotBlock;
    }

    @Override
    public ContraptionPlacementResult createPlacement(ContraptionPlacementContext context) {
        PotBlock block = (PotBlock) Block.byItem(context.heldItem().getItem());
        BlockState state = block.defaultBlockState()
                .setValue(PotBlock.FACING, context.player().getDirection().getOpposite())
                .setValue(PotBlock.HAS_BASE, false);

        CompoundTag nbt = new CompoundTag();
        nbt.put("Inputs", ContainerHelper.saveAllItems(new CompoundTag(),
                NonNullList.withSize(PotRecipe.RECIPES_SIZE, ItemStack.EMPTY)));
        nbt.putString("Carrier", Ingredient.EMPTY.toJson().toString());
        nbt.put("Result", ItemStack.EMPTY.serializeNBT());
        nbt.putInt("Status", 0);
        nbt.putInt("CurrentTick", 0);
        nbt.putInt("StirFryCount", 0);
        nbt.putLong("Seed", System.currentTimeMillis());
        nbt.putString("id", "kaleidoscope_cookery:pot");
        return single(context, state, nbt);
    }

    @Override
    public void afterPlaced(ContraptionPlacementContext context, ContraptionPlacementResult result) {
        if (isLit(context)) {
            ModTrigger.EVENT.trigger(context.player(), ModEventTriggerType.PLACE_POT_ON_HEAT_SOURCE);
        }
    }

    @Override
    public Optional<ContraptionRemovalResult> createRemoval(ContraptionRemovalContext context) {
        BlockState state = context.targetInfo().state();
        CompoundTag nbt = context.targetInfo().nbt();
        if (!(state.getBlock() instanceof PotBlock)
                || state.getValue(PotBlock.HAS_OIL)
                || (nbt != null && nbt.getInt("Status") != 0)
                || !isContainerEmpty(nbt)
                || (nbt != null && !ItemStack.of(nbt.getCompound("Result")).isEmpty())) {
            return Optional.empty();
        }
        return Optional.of(ContraptionRemovalResult.single(
                context.targetPos(), new ItemStack(ModBlocks.POT.get())));
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
        NonNullList<ItemStack> items = NonNullList.withSize(PotRecipe.RECIPES_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbt.getCompound("Inputs"), items);
        return items.stream().allMatch(ItemStack::isEmpty);
    }
}
