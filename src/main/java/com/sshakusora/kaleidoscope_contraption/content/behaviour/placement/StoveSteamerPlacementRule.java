package com.sshakusora.kaleidoscope_contraption.content.behaviour.placement;

import com.github.ysbbbbbb.kaleidoscopecookery.advancements.critereon.ModEventTriggerType;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.SteamerBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModTrigger;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.api.placement.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;
import java.util.Optional;

public class StoveSteamerPlacementRule implements ContraptionPlacementRule {
    @Override
    public boolean matches(ContraptionPlacementContext context) {
        return Block.byItem(context.heldItem().getItem()) instanceof SteamerBlock;
    }

    @Override
    public ContraptionPlacementResult createPlacement(ContraptionPlacementContext context) {
        SteamerBlock block = (SteamerBlock) Block.byItem(context.heldItem().getItem());
        BlockState state = block.defaultBlockState()
                .setValue(SteamerBlock.FACING, context.player().getDirection().getOpposite())
                .setValue(SteamerBlock.HALF, true)
                .setValue(SteamerBlock.HAS_LID, false)
                .setValue(SteamerBlock.HAS_BASE, false)
                .setValue(SteamerBlock.WATERLOGGED, false);
        if (context.supportInfo().state().hasProperty(BlockStateProperties.LIT)
                && context.supportInfo().state().getValue(BlockStateProperties.LIT)) {
            ModTrigger.EVENT.get().trigger(context.player(), ModEventTriggerType.USE_STEAMER);
        }
        return single(context, state, createSteamerNbt(context.heldItem(), context.contraptionEntity().level()));
    }

    @Override
    public Optional<ContraptionRemovalResult> createRemoval(ContraptionRemovalContext context) {
        StructureTemplate.StructureBlockInfo info = context.targetInfo();
        if (!(info.state().getBlock() instanceof SteamerBlock)
                || isSteamer(context.contraptionEntity(), context.targetPos().above())) {
            return Optional.empty();
        }
        ItemStack returnedItem = createSteamerItem(info, context.contraptionEntity().level());
        if (info.state().getValue(SteamerBlock.HALF)) {
            return Optional.of(ContraptionRemovalResult.single(context.targetPos(), returnedItem));
        }
        return Optional.of(new ContraptionRemovalResult(
                List.of(), List.of(reduceSteamerToSingleLayer(info, context.contraptionEntity().level())), List.of(returnedItem), true));
    }

    private ContraptionPlacementResult single(ContraptionPlacementContext context, BlockState state,
                                              CompoundTag nbt) {
        return ContraptionPlacementResult.single(new StructureTemplate.StructureBlockInfo(
                context.targetPos(), state, nbt));
    }

    private CompoundTag createSteamerNbt(ItemStack heldItem, Level level) {
        CompoundTag nbt = new CompoundTag();
        var customData = heldItem.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag handData = customData == null ? null : customData.copyTag();
        NonNullList<ItemStack> items = NonNullList.withSize(8, ItemStack.EMPTY);
        int[] progress = new int[8];
        int[] time = new int[8];

        if (handData != null) {
            NonNullList<ItemStack> handItems = NonNullList.withSize(4, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(handData, handItems, level.registryAccess());
            for (int i = 0; i < handItems.size(); i++) {
                items.set(i, handItems.get(i));
            }

            int[] handProgress = handData.getIntArray("CookingProgress");
            int[] handTime = handData.getIntArray("CookingTime");
            System.arraycopy(handProgress, 0, progress, 0, Math.min(4, handProgress.length));
            System.arraycopy(handTime, 0, time, 0, Math.min(4, handTime.length));
        }

        ContainerHelper.saveAllItems(nbt, items, true, level.registryAccess());
        nbt.putIntArray("CookingProgress", progress);
        nbt.putIntArray("CookingTime", time);
        nbt.putString("id", "kaleidoscope_cookery:steamer");
        return nbt;
    }

    private ItemStack createSteamerItem(StructureTemplate.StructureBlockInfo info, Level level) {
        ItemStack stack = ModItems.STEAMER.get().getDefaultInstance();
        CompoundTag nbt = info.nbt() == null ? new CompoundTag() : info.nbt();
        NonNullList<ItemStack> items = NonNullList.withSize(8, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbt, items, level.registryAccess());

        int[] cookingProgress = normalizeArray(nbt.getIntArray("CookingProgress"));
        int[] cookingTime = normalizeArray(nbt.getIntArray("CookingTime"));
        boolean half = info.state().getValue(SteamerBlock.HALF);
        int startIndex = half ? 0 : 4;

        NonNullList<ItemStack> savedItems = NonNullList.withSize(4, ItemStack.EMPTY);
        int[] savedProgress = new int[4];
        int[] savedTime = new int[4];
        for (int i = 0; i < 4; i++) {
            savedItems.set(i, items.get(startIndex + i));
            savedProgress[i] = cookingProgress[startIndex + i];
            savedTime[i] = cookingTime[startIndex + i];
        }

        CompoundTag blockEntityData = new CompoundTag();
        ContainerHelper.saveAllItems(blockEntityData, savedItems, false, level.registryAccess());
        if (!blockEntityData.isEmpty()) {
            blockEntityData.putIntArray("CookingProgress", savedProgress);
            blockEntityData.putIntArray("CookingTime", savedTime);
            BlockItem.setBlockEntityData(stack, ModBlocks.STEAMER_BE.get(), blockEntityData);
        }
        return stack;
    }

    private boolean isSteamer(AbstractContraptionEntity entity, BlockPos pos) {
        StructureTemplate.StructureBlockInfo info = entity.getContraption().getBlocks().get(pos);
        return info != null && info.state().getBlock() instanceof SteamerBlock;
    }

    private StructureTemplate.StructureBlockInfo reduceSteamerToSingleLayer(
            StructureTemplate.StructureBlockInfo info, Level level) {
        CompoundTag nbt = info.nbt() == null ? new CompoundTag() : info.nbt().copy();
        NonNullList<ItemStack> items = NonNullList.withSize(8, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbt, items, level.registryAccess());
        int[] cookingProgress = normalizeArray(nbt.getIntArray("CookingProgress"));
        int[] cookingTime = normalizeArray(nbt.getIntArray("CookingTime"));
        for (int i = 4; i < 8; i++) {
            items.set(i, ItemStack.EMPTY);
            cookingProgress[i] = 0;
            cookingTime[i] = 0;
        }
        ContainerHelper.saveAllItems(nbt, items, true, level.registryAccess());
        nbt.putIntArray("CookingProgress", cookingProgress);
        nbt.putIntArray("CookingTime", cookingTime);
        BlockState state = info.state().setValue(SteamerBlock.HALF, true);
        return new StructureTemplate.StructureBlockInfo(info.pos(), state, nbt);
    }

    private int[] normalizeArray(int[] source) {
        if (source.length >= 8) {
            return source;
        }
        int[] result = new int[8];
        System.arraycopy(source, 0, result, 0, source.length);
        return result;
    }
}
