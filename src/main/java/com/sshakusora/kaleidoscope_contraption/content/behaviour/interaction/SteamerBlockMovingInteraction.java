package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.SteamerBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.SteamerRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.github.ysbbbbbb.kaleidoscopecookery.item.SteamerItem;
import com.github.ysbbbbbb.kaleidoscopecookery.util.ItemUtils;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.api.placement.ContraptionPlacementManager;
import com.sshakusora.kaleidoscope_contraption.api.placement.ContraptionRemovalManager;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.registry.KCContraptionPlacements;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;


public class SteamerBlockMovingInteraction extends SyncedMovingInteractionBehaviour {

    // NBT键名
    private static final String ITEMS_TAG = "Items";
    private static final String COOKING_PROGRESS_TAG = "CookingProgress";
    private static final String COOKING_TIME_TAG = "CookingTime";

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return false;
        }

        BlockState state = info.state();
        if (!(state.getBlock() instanceof SteamerBlock)) {
            return false;
        }

        ItemStack itemInHand = player.getItemInHand(activeHand);

        // 处理按下移除键取下蒸笼
        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            ContraptionRemovalManager.Result result = ContraptionRemovalManager.tryRemove(
                    player, localPos, contraptionEntity);
            if (result == ContraptionRemovalManager.Result.NOT_REGISTERED) {
                return removeSteamerBlock(player, contraptionEntity, localPos, activeHand);
            }
            return result == ContraptionRemovalManager.Result.REMOVED;
        }

        CompoundTag nbt = info.nbt();
        if (nbt == null) {
            nbt = new CompoundTag();
        }

        // 空手 Shift 右击盖盖子、去掉盖子
        // 需要检查上方是否有方块，如果有方块则不能盖盖子
        Boolean hasLid = state.getValue(SteamerBlock.HAS_LID);
        if (itemInHand.isEmpty() && player.isSecondaryUseActive() && (hasLid || !isAboveBlocked(contraptionEntity, localPos))) {
            toggleLid(player, contraptionEntity, localPos, state, nbt, info);
            return true;
        }

        // 手持蒸笼，右击可以摞上去
        if (itemInHand.getItem() instanceof SteamerItem) {
            // 先尝试在当前位置堆叠（单层变双层）
            if (stackSteamer(player, contraptionEntity, localPos, state, nbt, itemInHand, activeHand, info)) {
                return true;
            }
            if (ContraptionPlacementManager.tryPlace(KCContraptionPlacements.STEAMER_TOP,
                    player, activeHand, localPos, contraptionEntity)) {
                return true;
            }
        }

        // 其他情况：放入/取出食物
        if (placeFood(player, contraptionEntity, localPos, state, nbt, itemInHand, info)) {
            return true;
        }

        if (takeFood(player, contraptionEntity, localPos, state, nbt, info)) {
            return true;
        }

        return false;
    }

    /**
     * 检查上方是否被阻挡
     */
    private boolean isAboveBlocked(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        BlockPos aboveLocalPos = localPos.above();
        StructureTemplate.StructureBlockInfo aboveInfo = contraptionEntity.getContraption().getBlocks().get(aboveLocalPos);
        if (aboveInfo != null && !aboveInfo.state().isAir()) {
            return true;
        }
        return false;
    }

    /**
     * 切换盖子状态
     */
    private void toggleLid(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                           BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        if (contraptionEntity.level().isClientSide) {
            return;
        }

        boolean newHasLid = !state.getValue(SteamerBlock.HAS_LID);
        BlockState newState = state.setValue(SteamerBlock.HAS_LID, newHasLid);

        StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                info.pos(), newState, nbt);
        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);

        // 播放音效
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
        contraptionEntity.level().playSound(null, soundPos,
                newHasLid ? SoundEvents.WOOD_PLACE : SoundEvents.WOOD_HIT,
                SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /**
     * 堆叠蒸笼
     */
    private boolean stackSteamer(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                 BlockState state, CompoundTag nbt, ItemStack itemInHand, InteractionHand hand,
                                 StructureTemplate.StructureBlockInfo info) {
        // 当目标是完整的未加盖蒸笼方块，继续向上搜索
        BlockPos placePos = localPos;
        BlockState blockState = state;

        while (true) {
            BlockPos abovePos = placePos.above();
            StructureTemplate.StructureBlockInfo aboveInfo = contraptionEntity.getContraption().getBlocks().get(abovePos);

            if (aboveInfo == null || aboveInfo.state().isAir()) {
                // 上方没有方块，检查是否可以放置
                break;
            }

            if (aboveInfo.state().is(ModBlocks.STEAMER.get())) {
                if (!aboveInfo.state().getValue(SteamerBlock.HAS_LID) && !aboveInfo.state().getValue(SteamerBlock.HALF)) {
                    // 上方是未加盖的完整蒸笼，继续向上
                    placePos = abovePos;
                    blockState = aboveInfo.state();
                    continue;
                }
            }
            // 上方有其他方块阻挡，无法放置
            return false;
        }

        // 检查最终位置是否可以放置
        StructureTemplate.StructureBlockInfo targetInfo = contraptionEntity.getContraption().getBlocks().get(placePos);
        if (targetInfo != null && targetInfo.state().is(ModBlocks.STEAMER.get())) {
            BlockState targetState = targetInfo.state();
            CompoundTag targetNbt = targetInfo.nbt();
            if (targetNbt == null) targetNbt = new CompoundTag();

            if (targetState.getValue(SteamerBlock.HALF) && !targetState.getValue(SteamerBlock.HAS_LID)) {
                // 目标是单层蒸笼，将其变为完整蒸笼
                if (!contraptionEntity.level().isClientSide) {
                    BlockState newState = targetState.setValue(SteamerBlock.HALF, false);

                    // 合并物品数据
                    NonNullList<ItemStack> targetItems = readItems(targetNbt, contraptionEntity.level());
                    int[] targetProgress = targetNbt.getIntArray(COOKING_PROGRESS_TAG);
                    int[] targetTime = targetNbt.getIntArray(COOKING_TIME_TAG);

                    if (targetProgress.length < 8) targetProgress = new int[8];
                    if (targetTime.length < 8) targetTime = new int[8];

                    // 从手持物品中读取数据
                    var blockEntityData = itemInHand.get(DataComponents.BLOCK_ENTITY_DATA);
                    CompoundTag handData = blockEntityData == null ? null : blockEntityData.copyTag();
                    if (handData != null) {
                        NonNullList<ItemStack> handItems = NonNullList.withSize(4, ItemStack.EMPTY);
                        ContainerHelper.loadAllItems(handData, handItems, contraptionEntity.level().registryAccess());
                        int[] handProgress = handData.getIntArray(COOKING_PROGRESS_TAG);
                        int[] handTime = handData.getIntArray(COOKING_TIME_TAG);

                        // 合并到上层槽位 (4-7)
                        for (int i = 0; i < 4; i++) {
                            targetItems.set(i + 4, handItems.get(i));
                            if (handProgress.length > i) targetProgress[i + 4] = handProgress[i];
                            if (handTime.length > i) targetTime[i + 4] = handTime[i];
                        }
                    }

                    CompoundTag newNbt = targetNbt.copy();
                    saveItems(newNbt, targetItems, contraptionEntity.level());
                    newNbt.putIntArray(COOKING_PROGRESS_TAG, targetProgress);
                    newNbt.putIntArray(COOKING_TIME_TAG, targetTime);

                    StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                            targetInfo.pos(), newState, newNbt);
                    ContraptionInteractionUtil.updateContraptionData(contraptionEntity, placePos, newInfo);

                    // 消耗物品
                    if (!player.isCreative()) {
                        itemInHand.shrink(1);
                    }

                    // 播放音效
                    Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(placePos), 1.0f);
                    BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
                    contraptionEntity.level().playSound(null, soundPos,
                            SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
                }
                return true;
            }
        }

        return false;
    }

    /**
     * 放入食物
     */
    private boolean placeFood(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                              BlockState state, CompoundTag nbt, ItemStack food, StructureTemplate.StructureBlockInfo info) {
        if (food.isEmpty()) {
            return false;
        }

        // 先检查这层是否是能交互的
        // 上层必须不能阻拦
        if (isAboveBlocked(contraptionEntity, localPos)) {
            return false;
        }

        // 然后检查配方
        SingleRecipeInput input = new SingleRecipeInput(food);
        var recipeOptional = contraptionEntity.level().getRecipeManager()
                .getRecipeFor(ModRecipes.STEAMER_RECIPE, input, contraptionEntity.level());

        if (recipeOptional.isEmpty()) {
            return false;
        }

        SteamerRecipe recipe = recipeOptional.get().value();
        int cookTime = recipe.getCookTick();
        if (cookTime <= 0) {
            return false;
        }

        // 读取当前物品
        NonNullList<ItemStack> items = readItems(nbt, contraptionEntity.level());
        int[] cookingProgress = nbt.getIntArray(COOKING_PROGRESS_TAG);
        int[] cookingTime = nbt.getIntArray(COOKING_TIME_TAG);

        if (cookingProgress.length < 8) cookingProgress = new int[8];
        if (cookingTime.length < 8) cookingTime = new int[8];

        boolean half = state.getValue(SteamerBlock.HALF);
        int endIndex = half ? 4 : 8;

        // 一次性放入
        boolean added = false;
        for (int i = 0; i < endIndex && !food.isEmpty(); i++) {
            ItemStack itemstack = items.get(i);
            if (itemstack.isEmpty()) {
                cookingTime[i] = cookTime;
                cookingProgress[i] = 0;
                items.set(i, food.split(1));
                added = true;
            }
        }

        if (added && !contraptionEntity.level().isClientSide) {
            CompoundTag newNbt = nbt.copy();
            saveItems(newNbt, items, contraptionEntity.level());
            newNbt.putIntArray(COOKING_PROGRESS_TAG, cookingProgress);
            newNbt.putIntArray(COOKING_TIME_TAG, cookingTime);
            StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                    info.pos(), state, newNbt);
            ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);

            // 播放音效
            Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
            BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
            contraptionEntity.level().playSound(null, soundPos,
                    SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 1.0F, 0.5F);
        }

        return added;
    }

    /**
     * 取出食物
     */
    private boolean takeFood(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                             BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        // 先检查这层是否是能交互的
        // 上层必须不能阻拦
        if (isAboveBlocked(contraptionEntity, localPos)) {
            return false;
        }

        // 读取当前物品
        NonNullList<ItemStack> items = readItems(nbt, contraptionEntity.level());
        int[] cookingProgress = nbt.getIntArray(COOKING_PROGRESS_TAG);
        int[] cookingTime = nbt.getIntArray(COOKING_TIME_TAG);

        if (cookingProgress.length < 8) cookingProgress = new int[8];
        if (cookingTime.length < 8) cookingTime = new int[8];

        boolean isAllEmpty = true;
        boolean half = state.getValue(SteamerBlock.HALF);
        int preferredSlot = player.getInventory().selected;
        int endIndex = half ? 4 : 8;

        // 一次性取出所有物品
        boolean taken = false;
        for (int i = 0; i < endIndex; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            isAllEmpty = false;

            if (!contraptionEntity.level().isClientSide) {
                ItemUtils.getItemToLivingEntity(player, stack, preferredSlot);
            }
            items.set(i, ItemStack.EMPTY);
            cookingTime[i] = 0;
            cookingProgress[i] = 0;
            taken = true;
        }

        if (!taken) {
            return false;
        }

        boolean hasLid = state.getValue(SteamerBlock.HAS_LID);
        boolean isAboveSteamer = isAboveSteamer(contraptionEntity, localPos);

        // 全为空，未加盖且上层不是蒸笼，那么拆掉一层
        if (isAllEmpty && !hasLid && !isAboveSteamer && !contraptionEntity.level().isClientSide) {
            ItemStack steamerItem = ModItems.STEAMER.get().getDefaultInstance();
            ItemUtils.getItemToLivingEntity(player, steamerItem, preferredSlot);

            // 把对应槽位全部清空
            for (int i = endIndex - 4; i < endIndex; i++) {
                items.set(i, ItemStack.EMPTY);
                cookingTime[i] = 0;
                cookingProgress[i] = 0;
            }

            // 播放破坏音效
            Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
            BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
            contraptionEntity.level().playSound(null, soundPos,
                    state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 0.8F);

            if (half) {
                // 单层直接移除方块
                removeSteamerBlockInternal(contraptionEntity, localPos);
            } else {
                // 完整蒸笼变为单层
                BlockState newState = state.setValue(SteamerBlock.HALF, true);
                CompoundTag newNbt = nbt.copy();
                saveItems(newNbt, items, contraptionEntity.level());
                newNbt.putIntArray(COOKING_PROGRESS_TAG, cookingProgress);
                newNbt.putIntArray(COOKING_TIME_TAG, cookingTime);
                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        info.pos(), newState, newNbt);
                ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);
            }
        } else if (!contraptionEntity.level().isClientSide) {
            // 更新数据
            CompoundTag newNbt = nbt.copy();
            saveItems(newNbt, items, contraptionEntity.level());
            newNbt.putIntArray(COOKING_PROGRESS_TAG, cookingProgress);
            newNbt.putIntArray(COOKING_TIME_TAG, cookingTime);
            StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                    info.pos(), state, newNbt);
            ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);
        }

        return true;
    }

    /**
     * 检查上方是否是蒸笼
     */
    private boolean isAboveSteamer(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        BlockPos aboveLocalPos = localPos.above();
        StructureTemplate.StructureBlockInfo aboveInfo = contraptionEntity.getContraption().getBlocks().get(aboveLocalPos);
        return aboveInfo != null && aboveInfo.state().is(ModBlocks.STEAMER.get());
    }

    /**
     * 移除蒸笼方块（玩家主动移除）
     */
    private boolean removeSteamerBlock(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos, InteractionHand activeHand) {
        StructureTemplate.StructureBlockInfo steamerInfo = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (steamerInfo == null || !(steamerInfo.state().getBlock() instanceof SteamerBlock)) {
            return false;
        }
        if (isAboveSteamer(contraptionEntity, localPos)) {
            return false;
        }

        // 在服务端执行移除逻辑
        if (!contraptionEntity.level().isClientSide) {
            CompoundTag nbt = steamerInfo.nbt();
            if (nbt == null) nbt = new CompoundTag();

            // 创建蒸笼物品并保存数据
            ItemStack steamerStack = createSteamerItemStack(steamerInfo.state(), nbt, contraptionEntity.level());

            if (steamerInfo.state().getValue(SteamerBlock.HALF)) {
                // 单层蒸笼：移除剩余的下半部分
                ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);
                var updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);
                contraptionEntity.getContraption().invalidateColliders();
                ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, updatedBounds);
            } else {
                // 双层蒸笼：先取下上半部分，下半部分继续留在Contraption中
                StructureTemplate.StructureBlockInfo reducedInfo = reduceSteamerToSingleLayer(steamerInfo, nbt, contraptionEntity.level());
                var updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);
                ContraptionInteractionUtil.updateContraptionDataWithBound(
                        contraptionEntity, localPos, reducedInfo, updatedBounds);
                contraptionEntity.getContraption().invalidateColliders();
            }

            // 掉落蒸笼物品给玩家
            if (!player.isCreative()) {
                player.getInventory().placeItemBackInInventory(steamerStack);
            }

            // 播放破坏音效
            ContraptionInteractionUtil.playBreakSound(contraptionEntity, localPos, steamerInfo.state());
        }

        return true;
    }

    /**
     * 内部移除蒸笼方块（取出食物后自动移除）
     */
    private void removeSteamerBlockInternal(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        // 从Contraption中移除方块
        ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);

        // 更新Contraption的bounds
        var updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);

        // 通知客户端
        ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, updatedBounds);
    }

    /**
     * 创建蒸笼物品堆，包含方块数据
     */
    private ItemStack createSteamerItemStack(BlockState state, CompoundTag nbt, Level level) {
        ItemStack stack = ModItems.STEAMER.get().getDefaultInstance();

        boolean half = state.getValue(SteamerBlock.HALF);

        // 读取物品和进度
        NonNullList<ItemStack> items = readItems(nbt, level);
        int[] cookingProgress = nbt.getIntArray(COOKING_PROGRESS_TAG);
        int[] cookingTime = nbt.getIntArray(COOKING_TIME_TAG);

        if (cookingProgress.length < 8) cookingProgress = new int[8];
        if (cookingTime.length < 8) cookingTime = new int[8];

        // 保存对应层的数据
        int startIndex = half ? 0 : 4;
        int endIndex = half ? 4 : 8;

        NonNullList<ItemStack> saveItems = NonNullList.withSize(4, ItemStack.EMPTY);
        int[] saveProgress = new int[4];
        int[] saveTime = new int[4];

        for (int i = startIndex; i < endIndex; i++) {
            int saveIndex = i - startIndex;
            saveItems.set(saveIndex, items.get(i));
            saveProgress[saveIndex] = cookingProgress[i];
            saveTime[saveIndex] = cookingTime[i];
        }

        CompoundTag saveTag = new CompoundTag();
        ContainerHelper.saveAllItems(saveTag, saveItems, false, level.registryAccess());
        if (!saveTag.isEmpty()) {
            saveTag.putIntArray(COOKING_PROGRESS_TAG, saveProgress);
            saveTag.putIntArray(COOKING_TIME_TAG, saveTime);
            BlockItem.setBlockEntityData(stack, ModBlocks.STEAMER_BE.get(), saveTag);
        }

        return stack;
    }

    private StructureTemplate.StructureBlockInfo reduceSteamerToSingleLayer(
            StructureTemplate.StructureBlockInfo info, CompoundTag nbt, Level level) {
        CompoundTag reducedNbt = nbt.copy();
        NonNullList<ItemStack> items = readItems(reducedNbt, level);
        int[] cookingProgress = normalizeArray(reducedNbt.getIntArray(COOKING_PROGRESS_TAG));
        int[] cookingTime = normalizeArray(reducedNbt.getIntArray(COOKING_TIME_TAG));
        for (int i = 4; i < 8; i++) {
            items.set(i, ItemStack.EMPTY);
            cookingProgress[i] = 0;
            cookingTime[i] = 0;
        }
        saveItems(reducedNbt, items, level);
        reducedNbt.putIntArray(COOKING_PROGRESS_TAG, cookingProgress);
        reducedNbt.putIntArray(COOKING_TIME_TAG, cookingTime);
        BlockState reducedState = info.state().setValue(SteamerBlock.HALF, true);
        return new StructureTemplate.StructureBlockInfo(info.pos(), reducedState, reducedNbt);
    }

    private int[] normalizeArray(int[] source) {
        if (source.length >= 8) {
            return source;
        }
        int[] normalized = new int[8];
        System.arraycopy(source, 0, normalized, 0, source.length);
        return normalized;
    }

    /**
     * 读取物品列表
     */
    private NonNullList<ItemStack> readItems(CompoundTag nbt, Level level) {
        NonNullList<ItemStack> items = NonNullList.withSize(8, ItemStack.EMPTY);
        if (nbt.contains(ITEMS_TAG, Tag.TAG_LIST)) {
            ContainerHelper.loadAllItems(nbt, items, level.registryAccess());
        }
        return items;
    }

    /**
     * 保存物品列表
     */
    private void saveItems(CompoundTag nbt, NonNullList<ItemStack> items, Level level) {
        ContainerHelper.saveAllItems(nbt, items, true, level.registryAccess());
    }

}
