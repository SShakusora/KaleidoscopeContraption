package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.OilPotBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.decoration.OilPotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.item.OilPotItem;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import static com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.OilPotBlock.HAS_OIL;

public class OilPotBlockMovingInteraction extends MovingInteractionBehaviour {

    private static final String OIL_COUNT = "OilCount";

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return false;
        }

        BlockState state = info.state();
        if (!(state.getBlock() instanceof OilPotBlock)) {
            return false;
        }

        // 处理按下移除键取下OilPotBlock
        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return removeOilPotBlock(player, contraptionEntity, localPos, activeHand);
        }

        ItemStack mainHandItem = player.getMainHandItem();

        CompoundTag nbt = info.nbt();
        if (nbt == null) {
            nbt = new CompoundTag();
        }

        int currentOilCount = nbt.getInt(OIL_COUNT);

        // 如果是空手，那么取出油
        if (mainHandItem.isEmpty()) {
            if (currentOilCount <= 0) {
                return false;
            }
            int needOilCount = Math.min(currentOilCount, 64);
            ItemStack oilStack = new ItemStack(ModItems.OIL.get(), needOilCount);
            player.setItemInHand(activeHand, oilStack);

            // 更新NBT
            if (!contraptionEntity.level().isClientSide) {
                CompoundTag newNbt = nbt.copy();
                newNbt.putInt(OIL_COUNT, currentOilCount - needOilCount);
                updateOilPotState(contraptionEntity, localPos, state, newNbt, info);
            }

            // 播放音效
            ContraptionInteractionUtil.playSound(contraptionEntity, localPos, SoundEvents.LANTERN_HIT, SoundSource.BLOCKS, 1.0F, player.getRandom().nextFloat() * 0.2F + 0.8F);
            return true;
        }

        // 如果是油，那么添加油
        if (mainHandItem.is(ModItems.OIL.get())) {
            int needOilCount = OilPotBlockEntity.MAX_OIL_COUNT - currentOilCount;
            if (needOilCount <= 0) {
                return false;
            }
            int addOilCount = Math.min(needOilCount, mainHandItem.getCount());

            if (!contraptionEntity.level().isClientSide) {
                CompoundTag newNbt = nbt.copy();
                newNbt.putInt(OIL_COUNT, currentOilCount + addOilCount);
                updateOilPotState(contraptionEntity, localPos, state, newNbt, info);

                if (!player.isCreative()) {
                    mainHandItem.shrink(addOilCount);
                }
            }

            // 播放音效
            ContraptionInteractionUtil.playSound(contraptionEntity, localPos, SoundEvents.LANTERN_HIT, SoundSource.BLOCKS, 1.0F, player.getRandom().nextFloat() * 0.2F + 0.4F);
            return true;
        }

        return false;
    }

    /**
     * 更新油壶的状态（HAS_OIL属性）
     */
    private void updateOilPotState(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                    BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        int oilCount = nbt.getInt(OIL_COUNT);
        boolean hasOil = oilCount > 0;
        BlockState newState = state.setValue(HAS_OIL, hasOil);

        StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                info.pos(), newState, nbt);
        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);
    }

    /**
     * 移除OilPotBlock
     */
    private boolean removeOilPotBlock(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos, InteractionHand activeHand) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof OilPotBlock)) {
            return false;
        }

        CompoundTag nbt = info.nbt();
        if (nbt == null) {
            nbt = new CompoundTag();
        }
        int oilCount = nbt.getInt(OIL_COUNT);

        // 在服务端执行移除逻辑
        if (!contraptionEntity.level().isClientSide) {
            // 从Contraption中移除方块
            ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);

            // 更新Contraption的bounds - 移除方块后需要重新计算
            var updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);

            // 创建带有油量的OilPot物品
            ItemStack oilPotItem = new ItemStack(ModBlocks.OIL_POT.get());
            OilPotItem.setOilCount(oilPotItem, oilCount);

            if (!player.isCreative()) {
                player.getInventory().placeItemBackInInventory(oilPotItem);
            }

            // 通知客户端重新渲染Contraption（同步bounds）
            ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, updatedBounds);

            // 播放破坏音效
            ContraptionInteractionUtil.playBreakSound(contraptionEntity, localPos, info.state());
        }

        return true;
    }
}