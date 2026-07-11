package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.EnamelBasinBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.item.KitchenShovelItem;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

public class EnamelBasinBlockMovingInteraction extends SyncedMovingInteractionBehaviour {

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return false;
        }

        BlockState state = info.state();
        if (!(state.getBlock() instanceof EnamelBasinBlock)) {
            return false;
        }

        ItemStack mainHandItem = player.getMainHandItem();

        // 先判断棍子敲
        if (mainHandItem.is(Items.STICK)) {
            float pitch = 0.6F + (float) Math.random() * 0.2F;
            Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
            BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
            contraptionEntity.level().playSound(player, soundPos, SoundEvents.LANTERN_BREAK, SoundSource.BLOCKS, 2, pitch);
            return true;
        }

        // 再判断开盖
        boolean hasLid = state.getValue(EnamelBasinBlock.HAS_LID);
        if (hasLid) {
            // 开盖
            if (!contraptionEntity.level().isClientSide) {
                BlockState newState = state.setValue(EnamelBasinBlock.HAS_LID, false);
                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        info.pos(), newState, info.nbt());
                setContraptionBlockData(contraptionEntity, localPos, newInfo);
            }
            ContraptionInteractionUtil.playSound(player, contraptionEntity, localPos,
                    SoundEvents.LANTERN_BREAK, SoundSource.BLOCKS, 0.8f, 0.8f);
            return true;
        }

        // 没有盖子，并且是空手，那么盖上盖子
        if (mainHandItem.isEmpty()) {
            if (!contraptionEntity.level().isClientSide) {
                BlockState newState = state.setValue(EnamelBasinBlock.HAS_LID, true);
                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        info.pos(), newState, info.nbt());
                setContraptionBlockData(contraptionEntity, localPos, newInfo);
            }
            ContraptionInteractionUtil.playSound(player, contraptionEntity, localPos,
                    SoundEvents.LANTERN_BREAK, SoundSource.BLOCKS, 0.8f, 0.4f);
            return true;
        }

        // 手持油脂时，消耗油脂添加进去
        if (mainHandItem.is(ModItems.OIL.get())) {
            return handleOilPlacement(player, contraptionEntity, localPos, state, info, mainHandItem);
        }

        // 当用铲子右击时
        if (mainHandItem.is(ModItems.KITCHEN_SHOVEL.get())) {
            return onShovelClick(player, contraptionEntity, localPos, state, info, mainHandItem);
        }

        return false;
    }

    /**
     * 处理放置油脂
     */
    private boolean handleOilPlacement(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                        BlockState state, StructureTemplate.StructureBlockInfo info, ItemStack mainHandItem) {
        CompoundTag nbt = info.nbt();
        if (nbt == null) {
            nbt = new CompoundTag();
        }

        int value = state.getValue(EnamelBasinBlock.OIL_COUNT);
        // 如果油已经满了，不能再放油
        if (value >= EnamelBasinBlock.MAX_OIL_COUNT) {
            return false;
        }

        // 尝试直接放满
        int needCount = EnamelBasinBlock.MAX_OIL_COUNT - value;
        int consumeCount = Math.min(needCount, mainHandItem.getCount());

        if (!contraptionEntity.level().isClientSide) {
            mainHandItem.shrink(consumeCount);
            BlockState newState = state.setValue(EnamelBasinBlock.OIL_COUNT, value + consumeCount);
            StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                    info.pos(), newState, nbt);
            setContraptionBlockData(contraptionEntity, localPos, newInfo);
        }

        ContraptionInteractionUtil.playSound(player, contraptionEntity, localPos,
                SoundEvents.HONEY_BLOCK_BREAK, SoundSource.BLOCKS, 0.8f, 0.8f);
        return true;
    }

    /**
     * 处理铲子点击
     */
    private boolean onShovelClick(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                    BlockState state, StructureTemplate.StructureBlockInfo info, ItemStack mainHandItem) {
        CompoundTag nbt = info.nbt();
        if (nbt == null) {
            nbt = new CompoundTag();
        }

        int value = state.getValue(EnamelBasinBlock.OIL_COUNT);
        boolean shovelHasOil = KitchenShovelItem.hasOil(mainHandItem);

        // 如果铲子有油，能还回去
        if (shovelHasOil) {
            // 如果油已经满了，不能再放油
            if (value >= EnamelBasinBlock.MAX_OIL_COUNT) {
                return false;
            }
            if (!contraptionEntity.level().isClientSide) {
                KitchenShovelItem.setHasOil(mainHandItem, false);
                BlockState newState = state.setValue(EnamelBasinBlock.OIL_COUNT, value + 1);
                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        info.pos(), newState, nbt);
                setContraptionBlockData(contraptionEntity, localPos, newInfo);
            }
            ContraptionInteractionUtil.playSound(player, contraptionEntity, localPos,
                    SoundEvents.HONEY_BLOCK_BREAK, SoundSource.BLOCKS, 0.8f, 0.8f);
            return true;
        }

        // 没有油时，返回
        if (value == 0) {
            return false;
        }

        // 取油
        if (!contraptionEntity.level().isClientSide) {
            KitchenShovelItem.setHasOil(mainHandItem, true);
            BlockState newState = state.setValue(EnamelBasinBlock.OIL_COUNT, value - 1);
            StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                    info.pos(), newState, nbt);
            setContraptionBlockData(contraptionEntity, localPos, newInfo);
        }
        ContraptionInteractionUtil.playSound(player, contraptionEntity, localPos,
                SoundEvents.HONEY_BLOCK_BREAK, SoundSource.BLOCKS, 0.8f, 1.2F);
        return true;
    }
}
