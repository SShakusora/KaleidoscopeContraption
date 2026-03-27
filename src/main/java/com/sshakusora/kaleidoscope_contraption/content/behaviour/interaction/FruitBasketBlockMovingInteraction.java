package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.FruitBasketBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.util.ItemUtils;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

public class FruitBasketBlockMovingInteraction extends MovingInteractionBehaviour {

    private static final String ITEMS_TAG = "BasketItems";
    private static final int MAX_SLOTS = 8;

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return false;
        }

        BlockState state = info.state();
        if (!(state.getBlock() instanceof FruitBasketBlock)) {
            return false;
        }

        ItemStack itemInHand = player.getItemInHand(activeHand);

        // 处理按下移除键取下FruitBasketBlock
        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return removeFruitBasketBlock(player, contraptionEntity, localPos, activeHand);
        }

        CompoundTag nbt = info.nbt();
        if (nbt == null) {
            nbt = new CompoundTag();
        }

        // Shift + 右键：取出物品
        if (player.isSecondaryUseActive()) {
            return takeOutItem(player, contraptionEntity, localPos, state, nbt, info);
        }

        // 右键：放入物品
        if (!itemInHand.isEmpty() && !itemInHand.is(ModItems.TRANSMUTATION_LUNCH_BAG.get())) {
            return putOnItem(player, contraptionEntity, localPos, state, nbt, itemInHand, info);
        }

        return false;
    }

    /**
     * 放入物品到果篮
     */
    private boolean putOnItem(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                               BlockState state, CompoundTag nbt, ItemStack stack, StructureTemplate.StructureBlockInfo info) {
        // 检查物品是否可以放入容器
        if (!stack.getItem().canFitInsideContainerItems()) {
            return false;
        }

        // 读取当前物品
        ItemStackHandler items = readItems(nbt);

        // 尝试插入物品
        ItemStack reminder = ItemHandlerHelper.insertItemStacked(items, stack.copy(), false);
        int insertedCount = stack.getCount() - reminder.getCount();

        if (insertedCount <= 0) {
            return false;
        }

        if (!contraptionEntity.level().isClientSide) {
            // 消耗玩家手中的物品
            stack.shrink(insertedCount);

            // 更新NBT
            CompoundTag newNbt = nbt.copy();
            saveItems(newNbt, items);

            StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                    info.pos(), state, newNbt);
            updateContraptionData(contraptionEntity, localPos, newInfo);

            // 播放音效
            playSound(contraptionEntity, localPos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS);
        }

        return true;
    }

    /**
     * 从果篮取出物品
     */
    private boolean takeOutItem(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                 BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        // 读取当前物品
        ItemStackHandler items = readItems(nbt);

        // 找到第一个非空槽位并取出
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (!contraptionEntity.level().isClientSide) {
                    // 提取物品
                    ItemStack extractItem = items.extractItem(i, items.getSlotLimit(i), false);

                    // 给予玩家物品
                    ItemUtils.getItemToLivingEntity(player, extractItem);

                    // 更新NBT
                    CompoundTag newNbt = nbt.copy();
                    saveItems(newNbt, items);

                    StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                            info.pos(), state, newNbt);
                    updateContraptionData(contraptionEntity, localPos, newInfo);

                    // 播放音效
                    playSound(contraptionEntity, localPos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS);
                }
                return true;
            }
        }

        return false;
    }

    /**
     * 移除FruitBasketBlock
     */
    private boolean removeFruitBasketBlock(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos, InteractionHand activeHand) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof FruitBasketBlock)) {
            return false;
        }

        // 在服务端执行移除逻辑
        if (!contraptionEntity.level().isClientSide) {
            // 创建果篮物品并保存NBT数据
            ItemStack fruitBasketItem = new ItemStack(ModBlocks.FRUIT_BASKET.get());

            // 如果有NBT数据，保存到物品中
            CompoundTag nbt = info.nbt();
            if (nbt != null && nbt.contains(ITEMS_TAG, Tag.TAG_COMPOUND)) {
                CompoundTag beTag = new CompoundTag();
                beTag.put(ITEMS_TAG, nbt.getCompound(ITEMS_TAG));
                BlockItem.setBlockEntityData(fruitBasketItem, ModBlocks.FRUIT_BASKET_BE.get(), beTag);
            }

            // 从Contraption中移除方块
            ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);

            // 更新Contraption的bounds - 移除方块后需要重新计算
            var updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);

            // 掉落FruitBasketBlock物品给玩家
            if (!player.isCreative()) {
                player.getInventory().placeItemBackInInventory(fruitBasketItem);
            }

            // 通知客户端重新渲染Contraption（同步bounds）
            ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, updatedBounds);

            // 播放破坏音效
            ContraptionInteractionUtil.playBreakSound(contraptionEntity, localPos, info.state());
        }

        return true;
    }

    /**
     * 读取物品列表
     */
    private ItemStackHandler readItems(CompoundTag nbt) {
        ItemStackHandler items = new ItemStackHandler(MAX_SLOTS);
        if (nbt != null && nbt.contains(ITEMS_TAG, Tag.TAG_COMPOUND)) {
            items.deserializeNBT(nbt.getCompound(ITEMS_TAG));
        }
        return items;
    }

    /**
     * 保存物品列表
     */
    private void saveItems(CompoundTag nbt, ItemStackHandler items) {
        nbt.put(ITEMS_TAG, items.serializeNBT());
    }

    /**
     * 播放音效
     */
    private void playSound(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                           SoundEvent soundEvent, SoundSource source) {
        ContraptionInteractionUtil.playSound(contraptionEntity, localPos, soundEvent, source, 1.0f, 1.0f);
    }

    /**
     * 更新Contraption中的方块数据
     */
    private void updateContraptionData(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                       StructureTemplate.StructureBlockInfo newInfo) {
        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);
    }
}
