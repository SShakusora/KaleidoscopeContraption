package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.TableBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteOneByTwoBlock;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.network.KCContraptionChangedPacket;
import com.sshakusora.kaleidoscope_contraption.network.KCPacketHandler;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

public class FoodBiteOneByTwoBlockMovingInteraction extends FoodBiteBlockMovingInteraction {

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return false;
        }

        BlockState state = info.state();
        if (!(state.getBlock() instanceof FoodBiteOneByTwoBlock)) {
            return false;
        }

        // 获取当前位置
        int position = state.getValue(FoodBiteOneByTwoBlock.POSITION);
        Direction facing = state.getValue(FoodBiteBlock.FACING);

        // 计算 LEFT 和 RIGHT 位置
        BlockPos leftPos;
        BlockPos rightPos;
        if (position == FoodBiteOneByTwoBlock.LEFT) {
            leftPos = localPos;
            rightPos = localPos.relative(facing.getCounterClockWise());
        } else {
            // 当前是 RIGHT
            rightPos = localPos;
            leftPos = localPos.relative(facing.getClockWise());
        }

        // 获取 LEFT 位置的方块信息（主控位置）
        StructureTemplate.StructureBlockInfo leftInfo = contraptionEntity.getContraption().getBlocks().get(leftPos);
        if (leftInfo == null || !(leftInfo.state().getBlock() instanceof FoodBiteOneByTwoBlock)) {
            return false;
        }

        // 获取 RIGHT 位置的方块信息
        StructureTemplate.StructureBlockInfo rightInfo = contraptionEntity.getContraption().getBlocks().get(rightPos);
        if (rightInfo == null || !(rightInfo.state().getBlock() instanceof FoodBiteOneByTwoBlock)) {
            return false;
        }

        // 获取当前咬食次数
        FoodBiteOneByTwoBlock foodBlock = (FoodBiteOneByTwoBlock) state.getBlock();
        int currentBites = leftInfo.state().getValue(foodBlock.getBites());
        int maxBites = foodBlock.getMaxBites();

        // 如果已经吃完，处理替换或移除逻辑
        if (currentBites >= maxBites) {
            return handleReplacementOrRemoval(player, activeHand, leftPos, rightPos, contraptionEntity, leftInfo, rightInfo);
        }

        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) return false;

        // 执行食用逻辑（只执行一次）
        if (!eatFood(player, foodBlock, contraptionEntity, leftPos)) {
            return false;
        }

        // 同时更新 LEFT 和 RIGHT 的咬食次数
        int newBites = currentBites + 1;
        updateBothBlocksBites(contraptionEntity, leftPos, rightPos, leftInfo, rightInfo, foodBlock, newBites, facing);

        return true;
    }

    /**
     * 处理替换或移除逻辑
     */
    private boolean handleReplacementOrRemoval(Player player, InteractionHand activeHand, BlockPos leftPos, BlockPos rightPos,
                                                AbstractContraptionEntity contraptionEntity, StructureTemplate.StructureBlockInfo leftInfo,
                                                StructureTemplate.StructureBlockInfo rightInfo) {
        // 检查玩家手持的物品
        ItemStack itemInHand = player.getItemInHand(activeHand);
        Block heldBlock = Block.byItem(itemInHand.getItem());

        if (heldBlock instanceof FoodBiteBlock newFoodBlock) {
            // 替换逻辑
            return replaceFoodBlock(player, contraptionEntity, leftPos, rightPos, leftInfo, rightInfo, newFoodBlock, itemInHand);
        } else {
            // 移除逻辑 - 检查下方是否是 TableBlock
            return removeFoodBlock(player, contraptionEntity, leftPos, rightPos, leftInfo, rightInfo);
        }
    }

    /**
     * 替换食物方块
     */
    private boolean replaceFoodBlock(Player player, AbstractContraptionEntity contraptionEntity, BlockPos leftPos, BlockPos rightPos,
                                      StructureTemplate.StructureBlockInfo leftInfo, StructureTemplate.StructureBlockInfo rightInfo,
                                      FoodBiteBlock newFoodBlock, ItemStack itemInHand) {
        if (!contraptionEntity.level().isClientSide) {
            // 掉落旧方块的战利品
            dropLootItems(leftInfo.state(), contraptionEntity, leftPos);

            Direction facing = leftInfo.state().getValue(FoodBiteBlock.FACING);

            AABB updatedBounds;
            
            if (newFoodBlock instanceof FoodBiteOneByTwoBlock) {
                // 新方块也是 1x2，替换为新的 1x2
                BlockState newRightState = newFoodBlock.defaultBlockState()
                        .setValue(newFoodBlock.getBites(), 0)
                        .setValue(FoodBiteBlock.FACING, facing)
                        .setValue(FoodBiteOneByTwoBlock.POSITION, FoodBiteOneByTwoBlock.RIGHT);
                BlockState newLeftState = newFoodBlock.defaultBlockState()
                        .setValue(newFoodBlock.getBites(), 0)
                        .setValue(FoodBiteBlock.FACING, facing)
                        .setValue(FoodBiteOneByTwoBlock.POSITION, FoodBiteOneByTwoBlock.LEFT);

                StructureTemplate.StructureBlockInfo newRightInfo = new StructureTemplate.StructureBlockInfo(rightPos, newRightState, null);
                StructureTemplate.StructureBlockInfo newLeftInfo = new StructureTemplate.StructureBlockInfo(leftPos, newLeftState, null);

                // 注册交互行为
                MovingInteractionBehaviour interactionBehaviour = MovingInteractionBehaviour.REGISTRY.get(newRightState);
                if (interactionBehaviour != null) {
                    contraptionEntity.getContraption().getInteractors().put(rightPos, interactionBehaviour);
                    contraptionEntity.getContraption().getInteractors().put(leftPos, interactionBehaviour);
                }

                // 更新 bounds
                updatedBounds = ContraptionInteractionUtil.updateBounds(contraptionEntity, rightPos);
                updatedBounds = ContraptionInteractionUtil.updateBounds(contraptionEntity, leftPos);

                ContraptionInteractionUtil.updateContraptionDataWithBound(contraptionEntity, rightPos, newRightInfo, updatedBounds);
                ContraptionInteractionUtil.updateContraptionDataWithBound(contraptionEntity, leftPos, newLeftInfo, updatedBounds);
            } else {
                // 新方块是 1x1，移除 1x2 并放置 1x1 在 LEFT 位置
                ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, rightPos);

                BlockState newState = newFoodBlock.defaultBlockState()
                        .setValue(newFoodBlock.getBites(), 0)
                        .setValue(FoodBiteBlock.FACING, facing);

                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(leftPos, newState, null);
                
                // 注册交互行为
                MovingInteractionBehaviour interactionBehaviour = MovingInteractionBehaviour.REGISTRY.get(newState);
                if (interactionBehaviour != null) {
                    contraptionEntity.getContraption().getInteractors().put(leftPos, interactionBehaviour);
                }

                ContraptionInteractionUtil.updateContraptionData(contraptionEntity, leftPos, newInfo);

                // 更新 bounds
                updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);
                ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, rightPos, updatedBounds);
                
                // 同步新方块到客户端
                KCPacketHandler.sendToTracking(
                        new KCContraptionChangedPacket(
                                contraptionEntity.getId(),
                                leftPos,
                                newInfo.state(),
                                newInfo.nbt(),
                                updatedBounds
                        ),
                        contraptionEntity
                );
            }

            // 消耗物品
            if (!player.isCreative()) {
                itemInHand.shrink(1);
            }

            // 播放音效
            ContraptionInteractionUtil.playPlaceSound(contraptionEntity, leftPos, newFoodBlock.defaultBlockState());
        }
        return true;
    }

    /**
     * 移除食物方块
     */
    private boolean removeFoodBlock(Player player, AbstractContraptionEntity contraptionEntity, BlockPos leftPos, BlockPos rightPos,
                                     StructureTemplate.StructureBlockInfo leftInfo, StructureTemplate.StructureBlockInfo rightInfo) {
        // 检查下方是否是 TableBlock
        BlockPos belowLeftPos = leftPos.below();
        BlockPos belowRightPos = rightPos.below();

        StructureTemplate.StructureBlockInfo belowLeftInfo = contraptionEntity.getContraption().getBlocks().get(belowLeftPos);
        StructureTemplate.StructureBlockInfo belowRightInfo = contraptionEntity.getContraption().getBlocks().get(belowRightPos);

        boolean isOnTable = (belowLeftInfo != null && belowLeftInfo.state().getBlock() instanceof TableBlock) ||
                           (belowRightInfo != null && belowRightInfo.state().getBlock() instanceof TableBlock);

        if (!isOnTable) {
            return false;
        }

        if (!contraptionEntity.level().isClientSide) {
            // 掉落战利品
            dropLootItems(leftInfo.state(), contraptionEntity, leftPos);

            // 移除两个方块
            ContraptionInteractionUtil.removeBlocksFromContraption(contraptionEntity, leftPos, rightPos);

            // 更新 bounds
            var updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);

            // 同步到客户端
            ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, updatedBounds, leftPos, rightPos);

            // 播放破坏音效
            ContraptionInteractionUtil.playBreakSound(contraptionEntity, leftPos, leftInfo.state());
        }
        return true;
    }

    /**
     * 更新两个方块的咬食次数
     */
    private void updateBothBlocksBites(AbstractContraptionEntity contraptionEntity, BlockPos leftPos, BlockPos rightPos,
                                        StructureTemplate.StructureBlockInfo leftInfo, StructureTemplate.StructureBlockInfo rightInfo,
                                        FoodBiteOneByTwoBlock foodBlock, int newBites, Direction facing) {
        BlockState newLeftState = leftInfo.state().setValue(foodBlock.getBites(), newBites);
        BlockState newRightState = rightInfo.state().setValue(foodBlock.getBites(), newBites);

        StructureTemplate.StructureBlockInfo newLeftInfo = new StructureTemplate.StructureBlockInfo(leftPos, newLeftState, leftInfo.nbt());
        StructureTemplate.StructureBlockInfo newRightInfo = new StructureTemplate.StructureBlockInfo(rightPos, newRightState, rightInfo.nbt());

        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, leftPos, newLeftInfo);
        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, rightPos, newRightInfo);
    }
}
