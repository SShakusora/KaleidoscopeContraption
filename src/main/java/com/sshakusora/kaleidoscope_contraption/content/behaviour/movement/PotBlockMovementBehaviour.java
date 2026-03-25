package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.PotBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.PotRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModParticles;
import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.FoodBiteRegistry;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.PotBlockMovingInteraction;
import com.sshakusora.kaleidoscope_contraption.mixin.accessor.ContraptionAccessor;
import com.sshakusora.kaleidoscope_contraption.network.KCContraptionChangedPacket;
import com.sshakusora.kaleidoscope_contraption.network.KCPacketHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionDataUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.Random;

import static com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.PotBlock.HAS_OIL;
import static com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.PotBlock.SHOW_OIL;
import static com.github.ysbbbbbb.kaleidoscopecookery.init.registry.FoodBiteRegistry.SUSPICIOUS_STIR_FRY;

public class PotBlockMovementBehaviour implements MovementBehaviour {

    private static final int PUT_INGREDIENT_TIME = 60 * 20;
    private static final int TAKEOUT_TIME = 40 * 20;
    private static final int BURNT_TIME = 20 * 20;

    private static final String INPUTS = "Inputs";
    private static final String CARRIER = "Carrier";
    private static final String RESULT = "Result";
    private static final String STATUS = "Status";
    private static final String CURRENT_TICK = "CurrentTick";
    private static final String STIR_FRY_COUNT = "StirFryCount";
    private static final String SEED = "Seed";

    // IPot 状态常量
    private static final int PUT_INGREDIENT = 0;
    private static final int COOKING = 1;
    private static final int FINISHED = 2;
    private static final int BURNT = 3;

    @Override
    public void tick(MovementContext context) {
        if (context.world.isClientSide) {
            return;
        }

        // 获取当前方块信息
        BlockPos localPos = context.localPos;
        AbstractContraptionEntity contraptionEntity = context.contraption.entity;
        if (contraptionEntity == null) {
            return;
        }

        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return;
        }

        BlockState state = info.state();
        if (!(state.getBlock() instanceof PotBlock)) {
            return;
        }

        CompoundTag nbt = info.nbt();
        if (nbt == null) {
            return;
        }

        // 检查是否有油，没有油不执行tick
        if (!state.getValue(HAS_OIL)) {
            return;
        }

        // 检查热源
        if (!PotBlockMovingInteraction.hasHeatSource(contraptionEntity, localPos)) {
            return;
        }

        int status = nbt.getInt(STATUS);
        int currentTick = nbt.getInt(CURRENT_TICK);

        // 倒计时逻辑
        if (currentTick > 0) {
            currentTick--;
        }

        // 模拟油炸声音（每20tick）
        if (currentTick % 20 == 0) {
            playFryingSound(contraptionEntity, localPos);
        }

        // 更新NBT中的currentTick，确保状态处理方法使用的是最新的值
        CompoundTag workingNbt = nbt.copy();
        workingNbt.putInt(CURRENT_TICK, currentTick);

        // 状态处理 - 使用workingNbt确保状态变化基于最新的currentTick
        boolean statusChanged = false;
        if (status == PUT_INGREDIENT) {
            statusChanged = tickPutIngredient(contraptionEntity, localPos, state, info, workingNbt, currentTick);
        } else if (status == COOKING) {
            statusChanged = tickCooking(contraptionEntity, localPos, state, info, workingNbt, currentTick);
        } else if (status == FINISHED) {
            statusChanged = tickFinished(contraptionEntity, localPos, state, info, workingNbt, currentTick);
        } else if (status == BURNT) {
            statusChanged = tickBurnt(contraptionEntity, localPos, state, info, workingNbt, currentTick);
        }

        // 如果状态发生了变化，重新获取最新的state和info（因为状态处理方法可能已经修改了它们）
        BlockState finalState = state;
        CompoundTag finalNbt = workingNbt;
        if (statusChanged) {
            StructureTemplate.StructureBlockInfo updatedInfo = contraptionEntity.getContraption().getBlocks().get(localPos);
            if (updatedInfo != null) {
                finalState = updatedInfo.state();
                finalNbt = updatedInfo.nbt() != null ? updatedInfo.nbt() : workingNbt;
            }
        }

        // 同步到客户端：状态变化时同步
        StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                info.pos(), finalState, finalNbt);
        updateContraptionData(contraptionEntity, localPos, newInfo, statusChanged);
    }

    /**
     * 放素材阶段tick
     * @return 是否发生了状态变化
     */
    private boolean tickPutIngredient(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                       BlockState state, StructureTemplate.StructureBlockInfo info,
                                       CompoundTag nbt, int currentTick) {
        // 播放粒子效果（每10tick）
        if (currentTick % 10 == 0 && contraptionEntity.level() instanceof ServerLevel serverLevel) {
            spawnCookingParticles(serverLevel, contraptionEntity, localPos);
        }

        // 时间到了，检查是否有食材
        if (currentTick == 0) {
            if (isEmpty(nbt)) {
                // 清空锅
                resetPot(contraptionEntity, localPos, state, info, nbt);
                playExtinguishSound(contraptionEntity, localPos);
            } else {
                // 自动切换到炒菜阶段
                startCooking(contraptionEntity, localPos, state, info, nbt);
            }
            return true; // 状态发生了变化
        }
        return false; // 状态未变化
    }

    /**
     * 炒菜阶段tick
     * @return 是否发生了状态变化
     */
    private boolean tickCooking(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                 BlockState state, StructureTemplate.StructureBlockInfo info,
                                 CompoundTag nbt, int currentTick) {
        if (currentTick == 0) {
            // 炒菜完成，进入FINISHED状态
            nbt.putInt(STATUS, FINISHED);
            nbt.putInt(CURRENT_TICK, TAKEOUT_TIME);

            // 检查翻炒次数，如果还有剩余翻炒次数，变成迷之炒菜
            int stirFryCount = nbt.getInt(STIR_FRY_COUNT);
            if (stirFryCount > 0) {
                nbt.put(RESULT, new ItemStack(FoodBiteRegistry.getItem(SUSPICIOUS_STIR_FRY)).serializeNBT());
                nbt.putString(CARRIER, Ingredient.of(Items.BOWL).toJson().toString());
            }

            // 隐藏油的显示 - 修改BlockState
            BlockState newState = state.setValue(SHOW_OIL, false);
            // 更新info以反映新的state
            StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                    info.pos(), newState, nbt);
            contraptionEntity.getContraption().getBlocks().put(localPos, newInfo);

            playExtinguishSound(contraptionEntity, localPos);
            return true; // 状态发生了变化
        }
        return false; // 状态未变化
    }

    /**
     * 完成阶段tick
     * @return 是否发生了状态变化
     */
    private boolean tickFinished(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                  BlockState state, StructureTemplate.StructureBlockInfo info,
                                  CompoundTag nbt, int currentTick) {
        // 播放粒子效果（每10tick）
        if (currentTick % 10 == 0 && contraptionEntity.level() instanceof ServerLevel serverLevel) {
            spawnFinishedParticles(serverLevel, contraptionEntity, localPos);
        }

        // 时间到了，进入烧焦阶段
        if (currentTick == 0) {
            nbt.putInt(STATUS, BURNT);
            nbt.putInt(CURRENT_TICK, BURNT_TIME);

            // 更新info以反映新的NBT状态，确保修改能持久化到下一个tick
            StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                    info.pos(), state, nbt);
            contraptionEntity.getContraption().getBlocks().put(localPos, newInfo);

            return true; // 状态发生了变化
        }
        return false; // 状态未变化
    }

    /**
     * 烧焦阶段tick
     * @return 是否发生了状态变化
     */
    private boolean tickBurnt(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                               BlockState state, StructureTemplate.StructureBlockInfo info,
                               CompoundTag nbt, int currentTick) {
        // 播放烟雾粒子
        if (currentTick % 2 == 0 && contraptionEntity.level() instanceof ServerLevel serverLevel) {
            int particleCount = 10 - currentTick / 5;
            spawnSmokeParticles(serverLevel, contraptionEntity, localPos, particleCount);
        }


        nbt.putInt(CURRENT_TICK, currentTick);
        StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                info.pos(), state, nbt);

        // 时间到了，重置锅并掉落木炭
        if (currentTick == 0) {
            resetPot(contraptionEntity, localPos, state, info, nbt);
            playExtinguishSound(contraptionEntity, localPos);

            // 掉落木炭
            if (contraptionEntity.level() instanceof ServerLevel serverLevel) {
                spawnSmokeParticles(serverLevel, contraptionEntity, localPos, 8);
                dropCharcoal(contraptionEntity, localPos);
            }

            contraptionEntity.getContraption().getBlocks().put(localPos, newInfo);
            return true; // 状态发生了变化
        }

        // 客户端更新烧糊动画
        if (currentTick % 25 == 0) {
            contraptionEntity.getContraption().getBlocks().put(localPos, newInfo);
            return true;
        }
        return false; // 状态未变化
    }

    /**
     * 开始炒菜
     */
    private void startCooking(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                               BlockState state, StructureTemplate.StructureBlockInfo info, CompoundTag nbt) {
        nbt.putInt(STATUS, COOKING);

        StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                info.pos(), state, nbt);
        updateContraptionData(contraptionEntity, localPos, newInfo, true);
    }

    /**
     * 重置锅
     */
    private void resetPot(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                           BlockState state, StructureTemplate.StructureBlockInfo info, CompoundTag nbt) {
        // 清空NBT数据
        nbt.put(INPUTS, ContainerHelper.saveAllItems(new CompoundTag(), NonNullList.withSize(PotRecipe.RECIPES_SIZE, ItemStack.EMPTY)));
        nbt.putString(CARRIER, Ingredient.EMPTY.toJson().toString());
        nbt.put(RESULT, ItemStack.EMPTY.serializeNBT());
        nbt.putInt(STATUS, PUT_INGREDIENT);
        nbt.putInt(CURRENT_TICK, 0);
        nbt.putInt(STIR_FRY_COUNT, 0);
        nbt.putLong(SEED, System.currentTimeMillis());

        // 更新BlockState
        BlockState newState = state.setValue(HAS_OIL, false).setValue(SHOW_OIL, false);
        StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                info.pos(), newState, nbt);
        updateContraptionData(contraptionEntity, localPos, newInfo, true);
    }

    /**
     * 检查锅是否为空
     */
    private boolean isEmpty(CompoundTag nbt) {
        NonNullList<ItemStack> inputs = NonNullList.withSize(PotRecipe.RECIPES_SIZE, ItemStack.EMPTY);
        if (nbt.contains(INPUTS, Tag.TAG_COMPOUND)) {
            ContainerHelper.loadAllItems(nbt.getCompound(INPUTS), inputs);
        }
        for (ItemStack stack : inputs) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 更新Contraption数据并同步到客户端
     */
    private void updateContraptionData(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                        StructureTemplate.StructureBlockInfo newInfo, boolean needSync) {
        // 更新方块数据
        contraptionEntity.getContraption().getBlocks().put(localPos, newInfo);
        ((ContraptionAccessor) contraptionEntity.getContraption()).getUpdateTags().put(localPos, newInfo.nbt());

        // 更新actor数据
        var actors = contraptionEntity.getContraption().getActors();
        for (int i = 0; i < actors.size(); i++) {
            MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor = actors.get(i);
            if (actor.getLeft().pos().equals(localPos)) {
                ContraptionDataUtil.setContraptionActorData(contraptionEntity, i, newInfo, actor.getRight());
                break;
            }
        }

        // 发送网络包同步到客户端
        if (needSync)
            KCPacketHandler.sendToTracking(
                    new KCContraptionChangedPacket(
                            contraptionEntity.getId(),
                            localPos,
                            newInfo.state(),
                            newInfo.nbt()
                    ),
                    contraptionEntity
            );
    }

    /**
     * 播放油炸声音
     */
    private void playFryingSound(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        Vec3 globalPos = contraptionEntity.toGlobalVector(
                Vec3.atCenterOf(localPos), 1.0f);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
        contraptionEntity.level().playSound(null, soundPos,
                SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
                0.5f + contraptionEntity.level().random.nextFloat() / 0.5f,
                0.8f + contraptionEntity.level().random.nextFloat() / 0.5f);
    }

    /**
     * 播放熄灭声音
     */
    private void playExtinguishSound(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        Vec3 globalPos = contraptionEntity.toGlobalVector(
                Vec3.atCenterOf(localPos), 1.0f);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
        contraptionEntity.level().playSound(null, soundPos,
                SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1F,
                (contraptionEntity.level().random.nextFloat() - contraptionEntity.level().random.nextFloat()) * 0.8F);
    }

    /**
     * 生成烹饪粒子
     */
    private void spawnCookingParticles(ServerLevel serverLevel, AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        Vec3 globalPos = contraptionEntity.toGlobalVector(
                Vec3.atCenterOf(localPos), 1.0f);
        Random random = new Random();
        serverLevel.sendParticles(ModParticles.COOKING.get(),
                globalPos.x + random.nextDouble() / 5 * (random.nextBoolean() ? 1 : -1),
                globalPos.y + 0.1 + random.nextDouble() / 3,
                globalPos.z + random.nextDouble() / 5 * (random.nextBoolean() ? 1 : -1),
                1, 0, 0, 0, 0);
    }

    /**
     * 生成完成阶段粒子
     */
    private void spawnFinishedParticles(ServerLevel serverLevel, AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        Vec3 globalPos = contraptionEntity.toGlobalVector(
                Vec3.atCenterOf(localPos), 1.0f);
        Random random = new Random();
        serverLevel.sendParticles(ModParticles.COOKING.get(),
                globalPos.x,
                globalPos.y + 0.1 + random.nextDouble() / 2,
                globalPos.z,
                1, 0, 0, 0, 0);
    }

    /**
     * 生成烟雾粒子
     */
    private void spawnSmokeParticles(ServerLevel serverLevel, AbstractContraptionEntity contraptionEntity,
                                      BlockPos localPos, int count) {
        Vec3 globalPos = contraptionEntity.toGlobalVector(
                Vec3.atCenterOf(localPos), 1.0f);
        Random random = new Random();
        serverLevel.sendParticles(ParticleTypes.SMOKE,
                globalPos.x + random.nextDouble() / 3 * (random.nextBoolean() ? 1 : -1),
                globalPos.y + 0.25 + random.nextDouble() / 3,
                globalPos.z + random.nextDouble() / 3 * (random.nextBoolean() ? 1 : -1),
                count, 0, 0, 0, 0.05);
    }

    /**
     * 掉落木炭
     */
    private void dropCharcoal(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        Vec3 globalPos = contraptionEntity.toGlobalVector(
                Vec3.atCenterOf(localPos), 1.0f);
        int count = 1 + contraptionEntity.level().random.nextInt(3);
        ItemStack charcoal = new ItemStack(Items.CHARCOAL, count);
        ItemEntity itemEntity = new ItemEntity(
                contraptionEntity.level(), globalPos.x, globalPos.y + 0.5, globalPos.z, charcoal);
        itemEntity.setDefaultPickUpDelay();
        contraptionEntity.level().addFreshEntity(itemEntity);
    }
}
