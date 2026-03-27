package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.PotBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.PotRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModParticles;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.FoodBiteRegistry;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionDataUtil;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import static com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.PotBlock.HAS_OIL;
import static com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.PotBlock.SHOW_OIL;
import static com.github.ysbbbbbb.kaleidoscopecookery.init.registry.FoodBiteRegistry.SUSPICIOUS_STIR_FRY;

public class PotBlockMovementBehaviour implements MovementBehaviour {

    // 时间常量（单位：tick）
    private static final int PUT_INGREDIENT_TIME = 60 * 20;
    private static final int TAKEOUT_TIME = 40 * 20;
    private static final int BURNT_TIME = 20 * 20;

    // NBT键名
    private static final String INPUTS = "Inputs";
    private static final String CARRIER = "Carrier";
    private static final String RESULT = "Result";
    private static final String STATUS = "Status";
    private static final String CURRENT_TICK = "CurrentTick";
    private static final String STIR_FRY_COUNT = "StirFryCount";
    private static final String SEED = "Seed";

    // IPot状态常量
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
        StructureTemplate.StructureBlockInfo info = context.contraption.getBlocks().get(context.localPos);
        if (info == null || !(info.state().getBlock() instanceof PotBlock)) {
            return;
        }

        BlockState state = info.state();
        CompoundTag nbt = info.nbt();
        if (nbt == null) {
            return;
        }

        // 检查是否有油，没有油则不执行tick
        if (!state.getValue(HAS_OIL)) {
            return;
        }

        // 检查热源
        if (!ContraptionInteractionUtil.hasHeatSource(context)) {
            return;
        }

        // 执行tick逻辑
        tickPot(context, state, nbt, info);
    }

    /**
     * 执行Pot的tick逻辑（参考PotBlockEntity.tick()）
     */
    private void tickPot(MovementContext context, BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        int currentTick = nbt.getInt(CURRENT_TICK);
        int status = nbt.getInt(STATUS);
        RandomSource random = context.world.random;

        // 递减计时器
        boolean statusChanged = false;
        if (currentTick > 0) {
            currentTick--;

            // 每5tick刷新一次（用于保存数据，不需要同步）
            if (currentTick % 5 == 0) {
                CompoundTag newNbt = nbt.copy();
                newNbt.putInt(CURRENT_TICK, currentTick);
                ContraptionDataUtil.updateContraptionData(context, state, newNbt, false);
            }

            // 模拟油炸声音（每20tick）
            if (currentTick % 20 == 0) {
                playCookingSound(context);
            }
        }

        // 根据状态执行不同逻辑，statusChanged表示状态是否发生改变
        int oldStatus = status;
        switch (status) {
            case PUT_INGREDIENT -> statusChanged = tickPutIngredient(context, state, nbt, info, currentTick, random);
            case COOKING -> statusChanged = tickCooking(context, state, nbt, info, currentTick, random);
            case FINISHED -> statusChanged = tickFinished(context, state, nbt, info, currentTick, random);
            case BURNT -> statusChanged = tickBurnt(context, state, nbt, info, currentTick, random);
        }

        // 如果状态发生改变，需要同步到客户端
        if (statusChanged && oldStatus != context.contraption.getBlocks().get(context.localPos).nbt().getInt(STATUS)) {
            StructureTemplate.StructureBlockInfo newInfo = context.contraption.getBlocks().get(context.localPos);
            ContraptionDataUtil.updateContraptionData(context, newInfo.state(), newInfo.nbt(), true);
        }
    }

    /**
     * 放食材阶段tick
     * @return 状态是否发生改变
     */
    private boolean tickPutIngredient(MovementContext context, BlockState state, CompoundTag nbt,
                                       StructureTemplate.StructureBlockInfo info, int currentTick, RandomSource random) {
        // 每10tick产生烹饪粒子效果
        if (currentTick % 10 == 0 && context.world instanceof ServerLevel serverLevel) {
            Vec3 globalPos = getGlobalPos(context);
            serverLevel.sendParticles(ModParticles.COOKING.get(),
                    globalPos.x + random.nextDouble() / 5 * (random.nextBoolean() ? 1 : -1),
                    globalPos.y - 0.4 + random.nextDouble() / 3,
                    globalPos.z + random.nextDouble() / 5 * (random.nextBoolean() ? 1 : -1),
                    1, 0, 0, 0, 0);
        }

        // 时间到，自动开始烹饪（如果有食材）
        if (currentTick == 0) {
            if (isEmpty(nbt)) {
                // 没有食材，重置状态
                resetPot(context, state, nbt, info);
                playExtinguishSound(context);
                spawnExtinguishParticles(context);
            } else {
                // 自动开始烹饪
                startCooking(context, state, nbt, info);
            }
            return true; // 状态已改变
        } else {
            // 更新currentTick，不需要同步
            CompoundTag newNbt = nbt.copy();
            newNbt.putInt(CURRENT_TICK, currentTick);
            ContraptionDataUtil.updateContraptionData(context, state, newNbt, false);
            return false; // 状态未改变
        }
    }

    /**
     * 烹饪阶段tick
     * @return 状态是否发生改变
     */
    private boolean tickCooking(MovementContext context, BlockState state, CompoundTag nbt,
                                 StructureTemplate.StructureBlockInfo info, int currentTick, RandomSource random) {
        if (currentTick == 0) {
            // 烹饪完成
            playExtinguishSound(context);

            CompoundTag newNbt = nbt.copy();
            newNbt.putInt(STATUS, FINISHED);

            // 检查翻炒次数，如果不足则变成迷之炒菜
            int stirFryCount = newNbt.getInt(STIR_FRY_COUNT);
            if (stirFryCount > 0) {
                // 翻炒不足，变成迷之炒菜
                newNbt.putString(CARRIER, Ingredient.of(Items.BOWL).toJson().toString());
                newNbt.put(RESULT, new ItemStack(FoodBiteRegistry.getItem(SUSPICIOUS_STIR_FRY)).serializeNBT());
            }

            newNbt.putInt(CURRENT_TICK, TAKEOUT_TIME);

            // 更新BlockState - 隐藏油
            BlockState newState = state.setValue(SHOW_OIL, false);

            ContraptionDataUtil.updateContraptionData(context, newState, newNbt, true);
            return true; // 状态已改变
        } else {
            // 更新currentTick，不需要同步
            CompoundTag newNbt = nbt.copy();
            newNbt.putInt(CURRENT_TICK, currentTick);
            ContraptionDataUtil.updateContraptionData(context, state, newNbt, false);
            return false; // 状态未改变
        }
    }

    /**
     * 完成阶段tick
     * @return 状态是否发生改变
     */
    private boolean tickFinished(MovementContext context, BlockState state, CompoundTag nbt,
                                  StructureTemplate.StructureBlockInfo info, int currentTick, RandomSource random) {
        // 每10tick产生完成粒子效果
        if (currentTick % 10 == 0 && context.world instanceof ServerLevel serverLevel) {
            Vec3 globalPos = getGlobalPos(context);
            serverLevel.sendParticles(ModParticles.COOKING.get(),
                    globalPos.x,
                    globalPos.y - 0.4 + random.nextDouble() / 2,
                    globalPos.z,
                    1, 0, 0, 0, 0);
        }

        if (currentTick == 0) {
            // 进入烧焦阶段
            CompoundTag newNbt = nbt.copy();
            newNbt.putInt(STATUS, BURNT);
            newNbt.putInt(CURRENT_TICK, BURNT_TIME);
            ContraptionDataUtil.updateContraptionData(context, state, newNbt, true);
            return true; // 状态已改变
        } else {
            // 更新currentTick，不需要同步
            CompoundTag newNbt = nbt.copy();
            newNbt.putInt(CURRENT_TICK, currentTick);
            ContraptionDataUtil.updateContraptionData(context, state, newNbt, false);
            return false; // 状态未改变
        }
    }

    /**
     * 烧焦阶段tick
     * @return 状态是否发生改变
     */
    private boolean tickBurnt(MovementContext context, BlockState state, CompoundTag nbt,
                               StructureTemplate.StructureBlockInfo info, int currentTick, RandomSource random) {
        int particleCount = 10 - currentTick / 5;

        // 产生烟雾粒子
        if (currentTick % 2 == 0 && context.world instanceof ServerLevel serverLevel) {
            Vec3 globalPos = getGlobalPos(context);
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    globalPos.x + random.nextDouble() / 3 * (random.nextBoolean() ? 1 : -1),
                    globalPos.y - 0.75 + random.nextDouble() / 3,
                    globalPos.z + random.nextDouble() / 3 * (random.nextBoolean() ? 1 : -1),
                    particleCount, 0, 0, 0, 0.05);
        }

        if (currentTick == 0) {
            // 完全烧焦，重置并掉落木炭
            resetPot(context, state, nbt, info);
            playExtinguishSound(context);

            if (context.world instanceof ServerLevel serverLevel) {
                Vec3 globalPos = getGlobalPos(context);
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        globalPos.x + random.nextDouble() / 3 * (random.nextBoolean() ? 1 : -1),
                        globalPos.y - 0.75 + random.nextDouble() / 3,
                        globalPos.z + random.nextDouble() / 3 * (random.nextBoolean() ? 1 : -1),
                        8, 0, 0, 0, 0.05);

                // 掉落木炭
                int count = 1 + random.nextInt(3);
                Block.popResource(context.world, new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z),
                        new ItemStack(Items.CHARCOAL, count));
            }
            return true; // 状态已改变
        } else {
            // 更新currentTick，每25tick同步一次
            CompoundTag newNbt = nbt.copy();
            newNbt.putInt(CURRENT_TICK, currentTick);
            boolean needSync = currentTick % 25 == 0;
            ContraptionDataUtil.updateContraptionData(context, state, newNbt, needSync);
            return false; // 状态未改变
        }
    }

    /**
     * 开始烹饪
     */
    private void startCooking(MovementContext context, BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        NonNullList<ItemStack> inputs = readInputs(nbt);
        SimpleContainer container = getContainer(inputs);

        // 匹配配方
        var recipeOptional = context.world.getRecipeManager().getRecipeFor(ModRecipes.POT_RECIPE, container, context.world);

        CompoundTag newNbt = nbt.copy();
        newNbt.putInt(STATUS, COOKING);

        recipeOptional.ifPresentOrElse(recipe -> {
            // 如果合成表符合
            newNbt.putString(CARRIER, recipe.carrier().toJson().toString());
            newNbt.put(RESULT, recipe.assemble(container, context.world.registryAccess()).serializeNBT());
            newNbt.putInt(CURRENT_TICK, recipe.time());
            newNbt.putInt(STIR_FRY_COUNT, recipe.stirFryCount());
        }, () -> {
            // 不符合，进入迷之炒菜阶段
            newNbt.putString(CARRIER, Ingredient.of(Items.BOWL).toJson().toString());
            newNbt.put(RESULT, new ItemStack(FoodBiteRegistry.getItem(SUSPICIOUS_STIR_FRY)).serializeNBT());
            newNbt.putInt(CURRENT_TICK, 10 * 20); // 迷之炒菜时间
            newNbt.putInt(STIR_FRY_COUNT, 0); // 迷之炒菜不计翻炒次数
        });

        ContraptionDataUtil.updateContraptionData(context, state, newNbt, true);
    }

    /**
     * 重置锅的状态
     */
    private void resetPot(MovementContext context, BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        CompoundTag newNbt = new CompoundTag();
        newNbt.put(INPUTS, ContainerHelper.saveAllItems(new CompoundTag(), NonNullList.withSize(PotRecipe.RECIPES_SIZE, ItemStack.EMPTY)));
        newNbt.putString(CARRIER, Ingredient.EMPTY.toJson().toString());
        newNbt.put(RESULT, ItemStack.EMPTY.serializeNBT());
        newNbt.putInt(STATUS, PUT_INGREDIENT);
        newNbt.putInt(CURRENT_TICK, 0);
        newNbt.putInt(STIR_FRY_COUNT, 0);
        newNbt.putLong(SEED, System.currentTimeMillis());

        BlockState newState = state.setValue(HAS_OIL, false);

        ContraptionDataUtil.updateContraptionData(context, newState, newNbt, true);
    }

    /**
     * 播放烹饪音效
     */
    private void playCookingSound(MovementContext context) {
        if (context.contraption.entity == null) return;
        Vec3 globalPos = getGlobalPos(context);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
        context.world.playSound(null, soundPos,
                SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
                0.5f + context.world.random.nextFloat() / 0.5f,
                0.8f + context.world.random.nextFloat() / 0.5f);
    }

    /**
     * 播放熄火音效
     */
    private void playExtinguishSound(MovementContext context) {
        if (context.contraption.entity == null) return;
        Vec3 globalPos = getGlobalPos(context);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
        context.world.playSound(null, soundPos,
                SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1F,
                (context.world.random.nextFloat() - context.world.random.nextFloat()) * 0.8F);
    }

    /**
     * 产生熄火粒子效果
     */
    private void spawnExtinguishParticles(MovementContext context) {
        if (!(context.world instanceof ServerLevel serverLevel)) return;
        Vec3 globalPos = getGlobalPos(context);
        RandomSource random = context.world.random;
        serverLevel.sendParticles(ModParticles.COOKING.get(),
                globalPos.x + random.nextDouble() / 3 * (random.nextBoolean() ? 1 : -1),
                globalPos.y - 0.9 + random.nextDouble() / 3,
                globalPos.z + random.nextDouble() / 3 * (random.nextBoolean() ? 1 : -1),
                8, 0, 0, 0, 0.05);
    }

    /**
     * 获取全局位置
     */
    private Vec3 getGlobalPos(MovementContext context) {
        if (context.contraption.entity == null) {
            return Vec3.atCenterOf(context.localPos);
        }
        return context.contraption.entity.toGlobalVector(Vec3.atCenterOf(context.localPos), 1.0f);
    }

    /**
     * 读取原料列表
     */
    private NonNullList<ItemStack> readInputs(CompoundTag nbt) {
        NonNullList<ItemStack> inputs = NonNullList.withSize(PotRecipe.RECIPES_SIZE, ItemStack.EMPTY);
        if (nbt.contains(INPUTS, Tag.TAG_COMPOUND)) {
            ContainerHelper.loadAllItems(nbt.getCompound(INPUTS), inputs);
        }
        return inputs;
    }

    /**
     * 获取容器
     */
    private SimpleContainer getContainer(NonNullList<ItemStack> inputs) {
        SimpleContainer container = new SimpleContainer(PotRecipe.RECIPES_SIZE);
        for (int i = 0; i < inputs.size(); i++) {
            ItemStack stack = inputs.get(i);
            if (!stack.isEmpty()) {
                container.setItem(i, stack);
            }
        }
        return container;
    }

    /**
     * 检查锅是否为空
     */
    private boolean isEmpty(CompoundTag nbt) {
        NonNullList<ItemStack> inputs = readInputs(nbt);
        for (ItemStack stack : inputs) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
