package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.TableBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.FoodBiteRegistry;
import com.mojang.datafixers.util.Pair;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.mixin.accessor.FoodBiteBlockAccessor;
import com.sshakusora.kaleidoscope_contraption.network.KCContraptionChangedPacket;
import com.sshakusora.kaleidoscope_contraption.network.KCPacketHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionBoundsUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
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

        // 获取食物属性（FoodBiteBlock中的protected字段）
        FoodProperties foodProperties = ((FoodBiteBlockAccessor) foodBlock).getFoodProperties();
        if (foodProperties == null || !player.canEat(foodProperties.canAlwaysEat())) {
            return false;
        }

        // 执行食用逻辑
        player.getFoodData().eat(foodProperties.getNutrition(), foodProperties.getSaturationModifier());

        // 应用食物效果
        for (Pair<MobEffectInstance, Float> pair : foodProperties.getEffects()) {
            if (!contraptionEntity.level().isClientSide && pair.getFirst() != null && contraptionEntity.level().random.nextFloat() < pair.getSecond()) {
                player.addEffect(new MobEffectInstance(pair.getFirst()));
            }
        }

        // 计算全局位置用于播放音效和触发事件
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);

        // 播放音效
        contraptionEntity.level().playSound(null, soundPos, SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.5F, contraptionEntity.level().getRandom().nextFloat() * 0.1F + 0.9F);

        // 触发游戏事件
        contraptionEntity.level().gameEvent(player, GameEvent.EAT, soundPos);

        // 更新咬食次数
        BlockState newState = state.setValue(bitesProperty, currentBites + 1);
        StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(info.pos(), newState, info.nbt());
        setContraptionBlockData(contraptionEntity, localPos, newInfo);

        // 查找并更新actor数据
        var actors = contraptionEntity.getContraption().getActors();
        for (int i = 0; i < actors.size(); i++) {
            MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor = actors.get(i);
            if (actor.getLeft().pos().equals(localPos)) {
                setContraptionActorData(contraptionEntity, i, newInfo, actor.getRight());
                break;
            }
        }

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
            // 先掉落旧方块的LootItem
            dropLootItems(oldState, contraptionEntity, localPos);

            // 创建新方块的BlockState（重置咬食次数为0）
            BlockState newState = newFoodBlock.defaultBlockState()
                    .setValue(newFoodBlock.getBites(), 0)
                    .setValue(FoodBiteBlock.FACING, oldState.getValue(FoodBiteBlock.FACING));

            StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                    oldInfo.pos(), newState, oldInfo.nbt());

            // 更新contraption中的方块数据
            setContraptionBlockData(contraptionEntity, localPos, newInfo);

            // 查找并更新actor数据
            var actors = contraptionEntity.getContraption().getActors();
            for (int i = 0; i < actors.size(); i++) {
                MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor = actors.get(i);
                if (actor.getLeft().pos().equals(localPos)) {
                    setContraptionActorData(contraptionEntity, i, newInfo, actor.getRight());
                    break;
                }
            }

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

        return true;
    }

    /**
     * 处理FoodBiteBlock的移除逻辑
     * 当玩家没有手持FoodBiteBlock时，检查下方是否是TableBlock，如果是则移除FoodBiteBlock并掉落LootItem
     */
    private boolean handleRemoval(Player player, BlockPos localPos,
                                  AbstractContraptionEntity contraptionEntity, BlockState oldState,
                                  StructureTemplate.StructureBlockInfo oldInfo) {
        // 检查正下方是否是TableBlock
        BlockPos belowPos = localPos.below();
        StructureTemplate.StructureBlockInfo belowInfo = contraptionEntity.getContraption().getBlocks().get(belowPos);
        if (belowInfo == null || !(belowInfo.state().getBlock() instanceof TableBlock)) {
            // 下方不是TableBlock，不处理交互
            return false;
        }

        // 在服务端执行移除逻辑
        if (!contraptionEntity.level().isClientSide) {
            // 掉落旧方块的LootItem
            dropLootItems(oldState, contraptionEntity, localPos);

            // 从blocks中真正移除该位置
            contraptionEntity.getContraption().getBlocks().remove(localPos);

            // 从interactors中移除
            contraptionEntity.getContraption().getInteractors().remove(localPos);

            // 从actors中移除
            contraptionEntity.getContraption().getActors().removeIf(actor -> actor.getLeft().pos().equals(localPos));

            // 更新Contraption的bounds - 移除方块后需要重新计算
            AABB updatedBounds = ContraptionBoundsUtil.recalculateBounds(contraptionEntity.getContraption());

            // 通知客户端重新渲染Contraption（同步bounds）
            // 使用空气状态表示该位置已被移除
            BlockState airState = Blocks.AIR.defaultBlockState();
            KCPacketHandler.sendToTracking(
                    new KCContraptionChangedPacket(
                            contraptionEntity.getId(),
                            localPos,
                            airState,
                            null,
                            updatedBounds
                    ),
                    contraptionEntity
            );

            // 播放破坏音效
            Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
            BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
            contraptionEntity.level().playSound(null, soundPos, oldState.getSoundType().getBreakSound(),
                    SoundSource.BLOCKS, 1.0F, 0.8F);
        }

        return true;
    }

    /**
     * 掉落食物的LootTable物品（bowl和额外添加物）
     */
    private void dropLootItems(BlockState state, AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId == null) {
            return;
        }

        FoodBiteRegistry.FoodData foodData = FoodBiteRegistry.FOOD_DATA_MAP.get(blockId);
        if (foodData == null) {
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
}
