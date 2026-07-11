package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.KitchenwareRacksBlock;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

public class KitchenwareRacksBlockMovingInteraction extends MovingInteractionBehaviour {

    private static final String LEFT_ITEM = "LeftItem";
    private static final String RIGHT_ITEM = "RightItem";

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return true;
        }

        BlockState state = info.state();
        if (!(state.getBlock() instanceof KitchenwareRacksBlock)) {
            return true;
        }

        ItemStack itemInHand = player.getItemInHand(activeHand);

        // 根据方块朝向和玩家视线方向判断是左边还是右边
        double yRotDeg = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite().toYRot();
        float yRotRad = (float) Math.toRadians(yRotDeg);

        // 获取玩家的视线方向
        Vec3 eyePosition = player.getEyePosition(1.0f);
        Vec3 lookVector = player.getViewVector(1.0f);
        double reachDistance = player.blockInteractionRange();
        Vec3 endPosition = eyePosition.add(lookVector.x * reachDistance, lookVector.y * reachDistance, lookVector.z * reachDistance);

        // 将视线转换到Contraption的本地坐标系
        Vec3 localEyePos = contraptionEntity.toLocalVector(eyePosition, 1.0f);
        Vec3 localEndPos = contraptionEntity.toLocalVector(endPosition, 1.0f);

        // 在本地坐标系中进行射线检测，计算与方块的交点
        // 创建方块的AABB（本地坐标系）
        AABB blockAABB = new AABB(localPos);
        var intersection = blockAABB.clip(localEyePos, localEndPos);

        boolean isLeft;
        if (intersection.isPresent()) {
            // 获取射线与方块的交点（本地坐标系）
            Vec3 hitPoint = intersection.get();
            // 将交点转换到以方块中心为原点的局部坐标系
            Vec3 relativeHit = hitPoint.subtract(Vec3.atCenterOf(localPos));
            // 根据方块朝向旋转到标准方向（参考 KitchenwareRacksBlock.use()）
            Vec3 localHit = relativeHit.yRot(yRotRad);
            // 判断是左边还是右边（x > 0 为左边，与 KitchenwareRacksBlock.use() 逻辑一致）
            isLeft = localHit.x > 0;
        } else {
            // 如果射线没有命中方块，使用玩家位置作为备选方案
            Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
            Vec3 playerPos = player.position();
            Vec3 relativePos = playerPos.subtract(globalPos);
            Vec3 localHit = relativePos.yRot(-yRotRad);
            isLeft = localHit.x > 0;
        }

        CompoundTag nbt = info.nbt();
        if (nbt == null) {
            nbt = new CompoundTag();
        }

        // 读取当前槽位的物品
        ItemStack stackInRacks = isLeft
                ? (nbt.contains(LEFT_ITEM) ? ItemStack.parseOptional(contraptionEntity.level().registryAccess(), nbt.getCompound(LEFT_ITEM)) : ItemStack.EMPTY)
                : (nbt.contains(RIGHT_ITEM) ? ItemStack.parseOptional(contraptionEntity.level().registryAccess(), nbt.getCompound(RIGHT_ITEM)) : ItemStack.EMPTY);

        // 取出物品：手为空且架子上有物品
        if (itemInHand.isEmpty() && !stackInRacks.isEmpty()) {
            if (!contraptionEntity.level().isClientSide) {
                // 掉落物品给玩家
                dropItemToPlayer(contraptionEntity, localPos, player, stackInRacks.copy());

                // 更新NBT，清空对应槽位
                CompoundTag newNbt = nbt.copy();
                if (isLeft) {
                    newNbt.put(LEFT_ITEM, ItemStack.EMPTY.saveOptional(contraptionEntity.level().registryAccess()));
                } else {
                    newNbt.put(RIGHT_ITEM, ItemStack.EMPTY.saveOptional(contraptionEntity.level().registryAccess()));
                }

                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        info.pos(), state, newNbt);
                ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);
            }
            // 播放音效
            playSound(contraptionEntity, localPos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, player.getSoundSource());
            return true;
        }

        // 放入物品：手上有工具类物品且架子为空
        if (!itemInHand.isEmpty() && itemInHand.is(Tags.Items.TOOLS) && stackInRacks.isEmpty()) {
            if (!contraptionEntity.level().isClientSide) {
                // 分割出一个物品
                ItemStack toPlace = itemInHand.split(1);

                // 更新NBT
                CompoundTag newNbt = nbt.copy();
                if (isLeft) {
                    newNbt.put(LEFT_ITEM, toPlace.save(contraptionEntity.level().registryAccess(), new CompoundTag()));
                } else {
                    newNbt.put(RIGHT_ITEM, toPlace.save(contraptionEntity.level().registryAccess(), new CompoundTag()));
                }

                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        info.pos(), state, newNbt);
                ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);
            }
            // 播放音效
            playSound(contraptionEntity, localPos, SoundEvents.ITEM_FRAME_ADD_ITEM, player.getSoundSource());
            return true;
        }

        return true;
    }

    /**
     * 在指定位置生成掉落物给玩家
     */
    private void dropItemToPlayer(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                  Player player, ItemStack stack) {
        if (contraptionEntity.level().isClientSide || stack.isEmpty()) {
            return;
        }

        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
        ItemEntity itemEntity = new ItemEntity(
                contraptionEntity.level(),
                globalPos.x,
                globalPos.y + 0.5,
                globalPos.z,
                stack
        );
        itemEntity.setDefaultPickUpDelay();
        contraptionEntity.level().addFreshEntity(itemEntity);
    }

    /**
     * 播放音效
     */
    private void playSound(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                           SoundEvent soundEvent, SoundSource source) {
        ContraptionInteractionUtil.playSound(contraptionEntity, localPos, soundEvent, source, 1.0f, 1.0f);
    }

}
