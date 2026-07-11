package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.SteamerBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModParticles;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionDataUtil;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

public class SteamerBlockMovementBehaviour implements MovementBehaviour {

    // NBT键名
    private static final String ITEMS_TAG = "Items";
    private static final String COOKING_PROGRESS_TAG = "CookingProgress";
    private static final String COOKING_TIME_TAG = "CookingTime";

    // 最大火力等级
    private static final int MAX_LIT_LEVEL = 4;

    @Override
    public void tick(MovementContext context) {
        if (context.world.isClientSide) {
            return;
        }

        // 获取当前方块信息
        StructureTemplate.StructureBlockInfo info = context.contraption.getBlocks().get(context.localPos);
        if (info == null || !(info.state().getBlock() instanceof SteamerBlock)) {
            return;
        }

        BlockState state = info.state();
        CompoundTag nbt = info.nbt();
        if (nbt == null) {
            return;
        }

        // 蒸笼每火力每 5 tick 更新一次
        if (context.world.getGameTime() % 5 == 0) {
            nbt = updateLitLevel(context, state, nbt);

            // 如果有蒸熟的，且进度为 -1，那么释放蒸汽粒子
            int[] cookingTime = nbt.getIntArray(COOKING_TIME_TAG);
            for (int time : cookingTime) {
                if (time == -1) {
                    makeRipeParticles(context, state);
                    break;
                }
            }
        }

        // 获取更新后的火力等级（从NBT计算或重新检测）
        int litLevel = getLitLevel(context, state, nbt);

        if (litLevel > 0) {
            cookingTick(context, state, nbt, info);
        } else {
            cooldownTick(context, state, nbt, info);
        }
    }

    /**
     * 更新火力等级
     */
    private CompoundTag updateLitLevel(MovementContext context, BlockState state, CompoundTag nbt) {
        int litLevel = 0;

        if (ContraptionInteractionUtil.hasHeatSource(context)) {
            litLevel = MAX_LIT_LEVEL;
        } else {
            // 检查下层是否是蒸笼
            BlockPos belowLocalPos = context.localPos.below();
            StructureTemplate.StructureBlockInfo belowInfo = context.contraption.getBlocks().get(belowLocalPos);
            if (belowInfo != null && belowInfo.state().is(ModBlocks.STEAMER.get())) {
                BlockState belowState = belowInfo.state();
                CompoundTag belowNbt = belowInfo.nbt();
                // 下层是蒸笼，且需要是双层的才能传递火力
                if (belowState.getValue(SteamerBlock.HALF)) {
                    litLevel = 0;
                } else if (belowNbt != null) {
                    // 从下层获取火力等级
                    int belowLitLevel = getLitLevelFromNbt(belowNbt);
                    litLevel = Math.max(belowLitLevel - 1, 0);
                }
            }
        }

        // 保存火力等级到NBT
        if (nbt.getInt("LitLevel") != litLevel) {
            CompoundTag newNbt = nbt.copy();
            newNbt.putInt("LitLevel", litLevel);
            ContraptionDataUtil.updateContraptionData(context, state, newNbt, false);
            return newNbt;
        }
        return nbt;
    }

    /**
     * 从NBT获取火力等级
     */
    private int getLitLevelFromNbt(CompoundTag nbt) {
        return nbt.getInt("LitLevel");
    }

    /**
     * 获取当前火力等级
     */
    private int getLitLevel(MovementContext context, BlockState state, CompoundTag nbt) {
        return nbt.getInt("LitLevel");
    }

    /**
     * 烹饪阶段tick
     */
    private void cookingTick(MovementContext context, BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        // 先检查上面是否是蒸笼
        BlockPos aboveLocalPos = context.localPos.above();
        StructureTemplate.StructureBlockInfo aboveInfo = context.contraption.getBlocks().get(aboveLocalPos);
        boolean aboveIsSteamer = aboveInfo != null && aboveInfo.state().is(ModBlocks.STEAMER.get());

        if (!aboveIsSteamer) {
            // 上面不是蒸笼，释放蒸汽粒子
            makeCookingParticles(context, state);
            // 既没有上层蒸笼，也没有盖子，不能蒸
            if (!state.getValue(SteamerBlock.HAS_LID)) {
                return;
            }
        }

        // 读取物品和进度
        NonNullList<ItemStack> items = readItems(nbt);
        int[] cookingProgress = nbt.getIntArray(COOKING_PROGRESS_TAG);
        int[] cookingTime = nbt.getIntArray(COOKING_TIME_TAG);

        if (cookingProgress.length < 8) {
            cookingProgress = new int[8];
        }
        if (cookingTime.length < 8) {
            cookingTime = new int[8];
        }

        boolean hasCooking = false;
        boolean changed = false;

        // 开始蒸
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty() || cookingTime[i] == -1) {
                continue;
            }
            // 已蒸熟的物品（cookingTime[i] == -1）不计入烹饪中
            if (cookingTime[i] != -1) {
                hasCooking = true;
            }
            int progress = cookingProgress[i]++;
            if (progress < cookingTime[i]) {
                continue;
            }
            Container container = new SimpleContainer(stack);
            ItemStack resultStack = context.world.getRecipeManager()
                    .getRecipeFor(ModRecipes.STEAMER_RECIPE, container, context.world)
                    .map(r -> r.assemble(container, context.world.registryAccess()))
                    .orElse(stack);
            if (!resultStack.isEmpty()) {
                items.set(i, resultStack);
                // 设置为 -1 代表已经蒸熟
                cookingTime[i] = -1;
                changed = true;
            }
        }

        // 只有实际发生变化时才同步到客户端
        // 烹饪进度推进不需要频繁同步，仅在物品完成烹饪时同步
        if (changed) {
            CompoundTag newNbt = nbt.copy();
            saveItems(newNbt, items);
            newNbt.putIntArray(COOKING_PROGRESS_TAG, cookingProgress);
            newNbt.putIntArray(COOKING_TIME_TAG, cookingTime);
            ContraptionDataUtil.updateContraptionData(context, state, newNbt, true);
        } else if (hasCooking) {
            // Keep server-side progress in the contraption every tick. This updates
            // only local data; the client is synchronized when an item finishes.
            CompoundTag newNbt = nbt.copy();
            saveItems(newNbt, items);
            newNbt.putIntArray(COOKING_PROGRESS_TAG, cookingProgress);
            newNbt.putIntArray(COOKING_TIME_TAG, cookingTime);
            ContraptionDataUtil.updateContraptionData(context, state, newNbt, false);
        }
    }

    /**
     * 冷却阶段tick
     */
    private void cooldownTick(MovementContext context, BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        int[] cookingProgress = nbt.getIntArray(COOKING_PROGRESS_TAG);
        int[] cookingTime = nbt.getIntArray(COOKING_TIME_TAG);

        if (cookingProgress.length < 8) {
            cookingProgress = new int[8];
        }
        if (cookingTime.length < 8) {
            cookingTime = new int[8];
        }

        boolean hasCooking = false;

        for (int i = 0; i < 8; i++) {
            if (cookingProgress[i] > 0) {
                hasCooking = true;
                cookingProgress[i] = Math.max(cookingProgress[i] - 2, 0);
            }
        }

        if (hasCooking) {
            CompoundTag newNbt = nbt.copy();
            newNbt.putIntArray(COOKING_PROGRESS_TAG, cookingProgress);
            ContraptionDataUtil.updateContraptionData(context, state, newNbt, false);
        }
    }

    /**
     * 产生烹饪粒子效果
     */
    private void makeCookingParticles(MovementContext context, BlockState state) {
        if (!(context.world instanceof ServerLevel serverLevel)) return;
        if (context.world.random.nextFloat() >= 0.1F) return;

        RandomSource random = serverLevel.random;
        boolean half = state.getValue(SteamerBlock.HALF);
        double yOffset = half ? 0.5 : 1;

        Vec3 globalPos = getGlobalPos(context);
        serverLevel.sendParticles(ModParticles.COOKING.get(),
                globalPos.x + random.nextDouble() / 2 * (random.nextBoolean() ? 1 : -1),
                globalPos.y + yOffset + random.nextDouble() / 2,
                globalPos.z + random.nextDouble() / 2 * (random.nextBoolean() ? 1 : -1),
                1, 0, 0, 0, 0.05);
    }

    /**
     * 产生蒸熟粒子效果
     */
    private void makeRipeParticles(MovementContext context, BlockState state) {
        if (!(context.world instanceof ServerLevel serverLevel)) return;
        if (context.world.random.nextFloat() >= 0.5F) return;

        RandomSource random = serverLevel.random;
        boolean half = state.getValue(SteamerBlock.HALF);
        double yOffset = half ? 0.25 : 0.75;

        Vec3 globalPos = getGlobalPos(context);
        serverLevel.sendParticles(ModParticles.COOKING.get(),
                globalPos.x + random.nextDouble() / 1.25 * (random.nextBoolean() ? 1 : -1),
                globalPos.y + yOffset + random.nextDouble() / 2,
                globalPos.z + random.nextDouble() / 1.25 * (random.nextBoolean() ? 1 : -1),
                1, 0, 0, 0, 0.05);
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
     * 读取物品列表
     */
    private NonNullList<ItemStack> readItems(CompoundTag nbt) {
        NonNullList<ItemStack> items = NonNullList.withSize(8, ItemStack.EMPTY);
        if (nbt.contains(ITEMS_TAG, Tag.TAG_LIST)) {
            ContainerHelper.loadAllItems(nbt, items);
        }
        return items;
    }

    /**
     * 保存物品列表
     */
    private void saveItems(CompoundTag nbt, NonNullList<ItemStack> items) {
        ContainerHelper.saveAllItems(nbt, items, true);
    }
}
