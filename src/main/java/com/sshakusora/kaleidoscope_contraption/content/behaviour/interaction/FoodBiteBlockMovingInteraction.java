package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.TableBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteOneByTwoBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteThreeByThreeBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.NinePart;
import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.FoodBiteRegistry;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.Quality;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.QualityUtils;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.placement.TableFoodPlacementRule;
import com.sshakusora.kaleidoscope_contraption.mixin.accessor.FoodBiteBlockAccessor;
import com.sshakusora.kaleidoscope_contraption.network.KCContraptionChangedPacket;
import com.sshakusora.kaleidoscope_contraption.network.KCPacketHandler;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.MutablePair;


public class FoodBiteBlockMovingInteraction extends MovingInteractionBehaviour {

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return false;
        }

        BlockState state = info.state();
        if (!(state.getBlock() instanceof FoodBiteBlock foodBlock)) {
            return false;
        }

        // 获取当前咬食次数
        IntegerProperty bitesProperty = foodBlock.getBites();
        int currentBites = state.getValue(bitesProperty);
        int maxBites = foodBlock.getMaxBites();

        // 如果已经吃完，检查玩家是否手持新的FoodBiteBlock物品进行替换
        if (currentBites >= maxBites) {
            return handleReplacement(player, activeHand, localPos, contraptionEntity, state, info);
        }

        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return new TableFoodPlacementRule().tryRemoveUnregistered(
                    player, localPos, contraptionEntity);
        }

        // 检查是否可以食用并执行食用逻辑
        if (!eatFood(player, foodBlock, state, contraptionEntity, localPos)) {
            return false;
        }

        // 更新咬食次数
        BlockState newState = state.setValue(bitesProperty, currentBites + 1);
        StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(info.pos(), newState, info.nbt());
        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);

        return true;
    }

    /**
     * 处理食物吃完后的替换逻辑
     * 当玩家手持新的FoodBiteBlock物品右键时，替换方块并掉落旧方块的LootItem
     * 当玩家没有手持FoodBiteBlock时，检查下方是否是TableBlock，如果是则移除FoodBiteBlock
     */
    private boolean handleReplacement(Player player, InteractionHand activeHand, BlockPos localPos,
                                      AbstractContraptionEntity contraptionEntity, BlockState oldState,
                                      StructureTemplate.StructureBlockInfo oldInfo) {
        ItemStack itemInHand = player.getItemInHand(activeHand);

        // 检查玩家手持的物品是否为FoodBiteBlock物品
        Block heldBlock = Block.byItem(itemInHand.getItem());
        if (!(heldBlock instanceof FoodBiteBlock newFoodBlock)) {
            // 如果不是食物方块，检查下方是否是TableBlock，如果是则移除FoodBiteBlock
            return handleRemoval(player, localPos, contraptionEntity, oldState, oldInfo);
        }

        // 在服务端执行替换和掉落逻辑
        if (!contraptionEntity.level().isClientSide) {
            // 先移除旧的多部分方块（如果适用）
            removeMultiPartBlockIfNeeded(contraptionEntity, localPos, oldState);

            // 先掉落旧方块的LootItem
            dropLootItems(oldState, contraptionEntity, localPos);

            // 根据新方块类型执行不同的放置逻辑
            if (newFoodBlock instanceof FoodBiteOneByTwoBlock oneByTwoBlock) {
                replaceWithOneByTwoBlock(player, contraptionEntity, localPos, oldState, itemInHand, oneByTwoBlock);
            } else if (newFoodBlock instanceof FoodBiteThreeByThreeBlock threeByThreeBlock) {
                replaceWithThreeByThreeBlock(player, contraptionEntity, localPos, oldState, itemInHand, threeByThreeBlock);
            } else {
                // 普通 1x1 方块替换
                BlockState newState = newFoodBlock.defaultBlockState()
                        .setValue(newFoodBlock.getBites(), 0)
                        .setValue(FoodBiteBlock.FACING, oldState.getValue(FoodBiteBlock.FACING))
                        .setValue(FoodBiteBlock.QUALITY, getQualityId(itemInHand));

                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        oldInfo.pos(), newState, oldInfo.nbt());

                MovingInteractionBehaviour interactionBehaviour = MovingInteractionBehaviour.REGISTRY.get(newState);
                if (interactionBehaviour != null) {
                    contraptionEntity.getContraption().getInteractors().put(localPos, interactionBehaviour);
                }

                // 注册MovementBehaviour到actors列表，使tick逻辑可以执行
                MovementBehaviour movementBehaviour = MovementBehaviour.REGISTRY.get(newState);
                if (movementBehaviour != null) {
                    var actors = contraptionEntity.getContraption().getActors();
                    // 检查是否已存在该位置的actor
                    boolean exists = false;
                    for (var actor : actors) {
                        if (actor.getLeft().pos().equals(localPos)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        MovementContext context = new MovementContext(
                                contraptionEntity.level(), newInfo, contraptionEntity.getContraption());
                        actors.add(MutablePair.of(newInfo, context));
                    }
                }

                // 更新contraption中的方块数据
                ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);

                // 消耗玩家手持的一个物品
                if (!player.isCreative()) {
                    itemInHand.shrink(1);
                }

                // 播放放置音效
                Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
                BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
                contraptionEntity.level().playSound(null, soundPos, newState.getSoundType().getPlaceSound(),
                        SoundSource.BLOCKS, 1.0F, 0.8F);
            }
        }

        return true;
    }

    /**
     * 如果当前方块是多部分方块，移除所有部分
     */
    protected void removeMultiPartBlockIfNeeded(AbstractContraptionEntity contraptionEntity, BlockPos localPos, BlockState state) {
        if (state.getBlock() instanceof FoodBiteOneByTwoBlock) {
            removeOneByTwoBlock(contraptionEntity, localPos, state);
        } else if (state.getBlock() instanceof FoodBiteThreeByThreeBlock) {
            removeThreeByThreeBlock(contraptionEntity, localPos, state);
        }
    }

    /**
     * 移除 1x2 方块的所有部分
     */
    private void removeOneByTwoBlock(AbstractContraptionEntity contraptionEntity, BlockPos localPos, BlockState state) {
        int position = state.getValue(FoodBiteOneByTwoBlock.POSITION);
        Direction facing = state.getValue(FoodBiteBlock.FACING);

        BlockPos leftPos;
        BlockPos rightPos;
        if (position == FoodBiteOneByTwoBlock.LEFT) {
            leftPos = localPos;
            rightPos = localPos.relative(facing.getCounterClockWise());
        } else {
            rightPos = localPos;
            leftPos = localPos.relative(facing.getClockWise());
        }

        // 移除两个位置（不播放音效和掉落，因为替换逻辑会处理）
        ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, leftPos);
        ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, rightPos);
    }

    /**
     * 移除 3x3 方块的所有部分
     */
    private void removeThreeByThreeBlock(AbstractContraptionEntity contraptionEntity, BlockPos localPos, BlockState state) {
        NinePart part = state.getValue(FoodBiteThreeByThreeBlock.PART);
        BlockPos centerPos = localPos.subtract(new Vec3i(part.getPosX(), 0, part.getPosY()));

        // 移除所有 9 个部分
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockPos pos = centerPos.offset(i, 0, j);
                ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, pos);
            }
        }
    }

    /**
     * 替换为 1x2 方块
     */
    private void replaceWithOneByTwoBlock(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                          BlockState oldState, ItemStack itemInHand, FoodBiteOneByTwoBlock newFoodBlock) {
        Direction facing = oldState.getValue(FoodBiteBlock.FACING);

        // 计算 LEFT 和 RIGHT 位置（以当前位置为 RIGHT）
        BlockPos rightPos = localPos;
        BlockPos leftPos = localPos.relative(facing.getClockWise());

        // 检查 LEFT 位置是否为空
        StructureTemplate.StructureBlockInfo leftInfo = contraptionEntity.getContraption().getBlocks().get(leftPos);
        if (leftInfo != null && !leftInfo.state().isAir()) {
            // 如果左边有方块，尝试以当前位置为 LEFT
            leftPos = localPos;
            rightPos = localPos.relative(facing.getCounterClockWise());

            StructureTemplate.StructureBlockInfo rightInfo = contraptionEntity.getContraption().getBlocks().get(rightPos);
            if (rightInfo != null && !rightInfo.state().isAir()) {
                // 两边都有方块，无法放置 1x2，改为放置 1x1 在原地
                placeSingleBlock(player, contraptionEntity, localPos, oldState, itemInHand, newFoodBlock);
                return;
            }
        }

        // RIGHT 位置的状态
        BlockState rightState = newFoodBlock.defaultBlockState()
                .setValue(newFoodBlock.getBites(), 0)
                .setValue(FoodBiteBlock.FACING, facing)
                .setValue(FoodBiteBlock.QUALITY, getQualityId(itemInHand))
                .setValue(FoodBiteOneByTwoBlock.POSITION, FoodBiteOneByTwoBlock.RIGHT);

        // LEFT 位置的状态
        BlockState leftState = newFoodBlock.defaultBlockState()
                .setValue(newFoodBlock.getBites(), 0)
                .setValue(FoodBiteBlock.FACING, facing)
                .setValue(FoodBiteBlock.QUALITY, getQualityId(itemInHand))
                .setValue(FoodBiteOneByTwoBlock.POSITION, FoodBiteOneByTwoBlock.LEFT);

        StructureTemplate.StructureBlockInfo newRightInfo = new StructureTemplate.StructureBlockInfo(rightPos, rightState, null);
        StructureTemplate.StructureBlockInfo newLeftInfo = new StructureTemplate.StructureBlockInfo(leftPos, leftState, null);

        MovingInteractionBehaviour interactionBehaviour = MovingInteractionBehaviour.REGISTRY.get(rightState);
        if (interactionBehaviour != null) {
            contraptionEntity.getContraption().getInteractors().put(rightPos, interactionBehaviour);
            contraptionEntity.getContraption().getInteractors().put(leftPos, interactionBehaviour);
        }

        // 放置两个方块
        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, rightPos, newRightInfo);
        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, leftPos, newLeftInfo);

        // 消耗物品
        if (!player.isCreative()) {
            itemInHand.shrink(1);
        }

        // 播放音效
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
        contraptionEntity.level().playSound(null, soundPos, rightState.getSoundType().getPlaceSound(),
                SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /**
     * 替换为 3x3 方块
     */
    private void replaceWithThreeByThreeBlock(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                              BlockState oldState, ItemStack itemInHand, FoodBiteThreeByThreeBlock newFoodBlock) {
        Direction facing = oldState.getValue(FoodBiteBlock.FACING);

        // 以当前位置为中心，检查周围 3x3 区域是否为空
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockPos checkPos = localPos.offset(i, 0, j);
                StructureTemplate.StructureBlockInfo checkInfo = contraptionEntity.getContraption().getBlocks().get(checkPos);
                if (checkInfo != null && !checkInfo.state().isAir()) {
                    // 有位置被占用，改为放置 1x1 在原地
                    placeSingleBlock(player, contraptionEntity, localPos, oldState, itemInHand, newFoodBlock);
                    return;
                }
            }
        }

        // 计算所有 9 个位置
        BlockPos[] positions = new BlockPos[9];
        StructureTemplate.StructureBlockInfo[] infos = new StructureTemplate.StructureBlockInfo[9];
        int idx = 0;

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockPos pos = localPos.offset(i, 0, j);
                positions[idx] = pos;

                NinePart part = NinePart.getPartByPos(i, j);
                BlockState newState = newFoodBlock.defaultBlockState()
                        .setValue(newFoodBlock.getBites(), 0)
                        .setValue(FoodBiteBlock.FACING, facing)
                        .setValue(FoodBiteBlock.QUALITY, getQualityId(itemInHand))
                        .setValue(FoodBiteThreeByThreeBlock.PART, part);

                infos[idx] = new StructureTemplate.StructureBlockInfo(pos, newState, null);
                idx++;
            }
        }

        // 放置所有方块
        AABB updatedBounds = null;
        MovingInteractionBehaviour interactionBehaviour = null;
        for (int i = 0; i < 9; i++) {
            ContraptionInteractionUtil.updateContraptionData(contraptionEntity, positions[i], infos[i]);
            updatedBounds = ContraptionInteractionUtil.updateBounds(contraptionEntity, positions[i]);

            // 注册交互行为
            if (interactionBehaviour == null) {
                interactionBehaviour = MovingInteractionBehaviour.REGISTRY.get(infos[i].state());
            }
            if (interactionBehaviour != null) {
                contraptionEntity.getContraption().getInteractors().put(positions[i], interactionBehaviour);
            }
        }

        // 同步到客户端
        for (int i = 0; i < 9; i++) {
            KCPacketHandler.sendToTracking(
                    new KCContraptionChangedPacket(
                            contraptionEntity.getId(),
                            positions[i],
                            infos[i].state(),
                            infos[i].nbt(),
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
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
        contraptionEntity.level().playSound(null, soundPos, infos[0].state().getSoundType().getPlaceSound(),
                SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /**
     * 在指定位置放置单个方块（用于空间不足时的回退）
     */
    private void placeSingleBlock(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                  BlockState oldState, ItemStack itemInHand, FoodBiteBlock newFoodBlock) {
        BlockState newState = newFoodBlock.defaultBlockState()
                .setValue(newFoodBlock.getBites(), 0)
                .setValue(FoodBiteBlock.FACING, oldState.getValue(FoodBiteBlock.FACING))
                .setValue(FoodBiteBlock.QUALITY, getQualityId(itemInHand));

        StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(localPos, newState, null);
        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);

        // 消耗物品
        if (!player.isCreative()) {
            itemInHand.shrink(1);
        }

        // 播放音效
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
        contraptionEntity.level().playSound(null, soundPos, newState.getSoundType().getPlaceSound(),
                SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /**
     * 处理FoodBiteBlock的移除逻辑
     * 当玩家没有手持FoodBiteBlock时，检查下方是否是TableBlock，如果是则移除FoodBiteBlock并掉落LootItem
     */
    private boolean handleRemoval(Player player, BlockPos localPos,
                                  AbstractContraptionEntity contraptionEntity, BlockState oldState,
                                  StructureTemplate.StructureBlockInfo oldInfo) {
        // 对于多部分方块，需要检查主控位置下方是否是TableBlock
        BlockPos checkPos = localPos;
        if (oldState.getBlock() instanceof FoodBiteOneByTwoBlock) {
            int position = oldState.getValue(FoodBiteOneByTwoBlock.POSITION);
            Direction facing = oldState.getValue(FoodBiteBlock.FACING);
            if (position == FoodBiteOneByTwoBlock.RIGHT) {
                checkPos = localPos.relative(facing.getClockWise());
            }
        } else if (oldState.getBlock() instanceof FoodBiteThreeByThreeBlock) {
            NinePart part = oldState.getValue(FoodBiteThreeByThreeBlock.PART);
            checkPos = localPos.subtract(new Vec3i(part.getPosX(), 0, part.getPosY()));
        }

        // 检查主控位置正下方是否是TableBlock
        BlockPos belowPos = checkPos.below();
        StructureTemplate.StructureBlockInfo belowInfo = contraptionEntity.getContraption().getBlocks().get(belowPos);
        if (belowInfo == null || !(belowInfo.state().getBlock() instanceof TableBlock)) {
            // 下方不是TableBlock，不处理交互
            return false;
        }

        // 在服务端执行移除逻辑
        if (!contraptionEntity.level().isClientSide) {
            // 掉落旧方块的LootItem（只在主控位置掉落一次）
            dropLootItems(oldState, contraptionEntity, checkPos);

            // 如果是多部分方块，移除所有部分
            if (oldState.getBlock() instanceof FoodBiteOneByTwoBlock) {
                removeOneByTwoBlock(contraptionEntity, localPos, oldState);
            } else if (oldState.getBlock() instanceof FoodBiteThreeByThreeBlock) {
                removeThreeByThreeBlock(contraptionEntity, localPos, oldState);
            } else {
                // 普通 1x1 方块，只移除当前位置
                ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);
            }

            // 更新Contraption的bounds - 移除方块后需要重新计算
            var updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);

            // 通知客户端重新渲染Contraption（同步bounds）
            ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, updatedBounds);

            // 播放破坏音效
            ContraptionInteractionUtil.playBreakSound(contraptionEntity, checkPos, oldState);
        }

        return true;
    }

    /**
     * 检查玩家是否可以食用该食物，并执行食用逻辑
     *
     * @return 如果成功食用返回 true，否则返回 false
     */
    protected boolean eatFood(Player player, FoodBiteBlock foodBlock, BlockState state,
                              AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        // 获取食物属性（FoodBiteBlock中的protected字段）
        FoodProperties foodProperties = ((FoodBiteBlockAccessor) foodBlock).getFoodProperties();
        if (foodProperties == null || !player.canEat(foodProperties.canAlwaysEat())) {
            return false;
        }

        double ratio = 1.0;
        int qualityId = state.getValue(FoodBiteBlock.QUALITY);
        if (qualityId != FoodBiteBlock.DEFAULT_QUALITY) {
            ratio = Quality.BY_ID.apply(qualityId).getRatio();
        }

        player.getFoodData().eat(
                (int) Math.round(foodProperties.nutrition() * ratio),
                (float) (foodProperties.saturation() * ratio));

        // 应用食物效果
        for (FoodProperties.PossibleEffect possibleEffect : foodProperties.effects()) {
            MobEffectInstance effect = possibleEffect.effect();
            if (!contraptionEntity.level().isClientSide && effect != null
                    && contraptionEntity.level().random.nextFloat() < possibleEffect.probability()) {
                player.addEffect(new MobEffectInstance(
                        effect.getEffect(),
                        (int) Math.round(effect.getDuration() * ratio),
                        effect.getAmplifier()));
            }
        }

        // 计算全局位置用于播放音效和触发事件
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);

        // 播放音效
        contraptionEntity.level().playSound(null, soundPos, SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.5F, contraptionEntity.level().getRandom().nextFloat() * 0.1F + 0.9F);

        // 触发游戏事件
        contraptionEntity.level().gameEvent(player, GameEvent.EAT, soundPos);

        return true;
    }

    public static int getQualityId(ItemStack stack) {
        return QualityUtils.hasQuality(stack)
                ? QualityUtils.getQuality(stack).getId()
                : FoodBiteBlock.DEFAULT_QUALITY;
    }

    /**
     * 掉落食物的LootTable物品（bowl和额外添加物）
     */
    protected void dropLootItems(BlockState state, AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null) {
            return;
        }

        FoodBiteRegistry.FoodData foodData = FoodBiteRegistry.FOOD_DATA_MAP.get(blockId);
        if (foodData == null) {
            // 对于不在 FoodBiteRegistry 中的方块（如 3x3 方块），使用默认掉落（碗）
            dropDefaultLootItems(state, contraptionEntity, localPos);
            return;
        }

        // 计算全局位置用于生成掉落物
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);

        // 掉落每个LootItem
        for (var lootItem : foodData.getLootItems()) {
            ItemStack stack = new ItemStack(lootItem.asItem());
            ItemEntity itemEntity = new ItemEntity(
                    contraptionEntity.level(),
                    globalPos.x,
                    globalPos.y,
                    globalPos.z,
                    stack
            );
            // 给予掉落物一个小的随机速度
            itemEntity.setDeltaMovement(
                    contraptionEntity.level().getRandom().nextGaussian() * 0.05,
                    contraptionEntity.level().getRandom().nextGaussian() * 0.05 + 0.2,
                    contraptionEntity.level().getRandom().nextGaussian() * 0.05
            );
            contraptionEntity.level().addFreshEntity(itemEntity);
        }
    }

    /**
     * 掉落默认的战利品（碗）
     * 用于不在 FoodBiteRegistry 中的方块
     */
    protected void dropDefaultLootItems(BlockState state, AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        // 计算全局位置用于生成掉落物
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);

        // 默认只掉落碗
        ItemStack stack = new ItemStack(Items.BOWL);
        ItemEntity itemEntity = new ItemEntity(
                contraptionEntity.level(),
                globalPos.x,
                globalPos.y,
                globalPos.z,
                stack
        );
        // 给予掉落物一个小的随机速度
        itemEntity.setDeltaMovement(
                contraptionEntity.level().getRandom().nextGaussian() * 0.05,
                contraptionEntity.level().getRandom().nextGaussian() * 0.05 + 0.2,
                contraptionEntity.level().getRandom().nextGaussian() * 0.05
        );
        contraptionEntity.level().addFreshEntity(itemEntity);
    }
}
