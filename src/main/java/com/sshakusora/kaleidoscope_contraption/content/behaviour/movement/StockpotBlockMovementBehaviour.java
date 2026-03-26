package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.StockpotBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.container.StockpotContainer;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.StockpotRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.serializer.StockpotRecipeSerializer;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModParticles;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModSoupBases;
import com.github.ysbbbbbb.kaleidoscopecookery.init.tag.TagMod;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionDataUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

public class StockpotBlockMovementBehaviour implements MovementBehaviour {

    // NBT键名
    private static final String INPUTS = "Inputs";
    private static final String RECIPE_ID = "RecipeId";
    private static final String SOUP_BASE_ID = "SoupBaseId";
    private static final String RESULT = "Result";
    private static final String STATUS = "Status";
    private static final String CURRENT_TICK = "CurrentTick";
    private static final String TAKEOUT_COUNT = "TakeoutCount";
    private static final String CARRIER = "Carrier";

    // IStockpot状态常量
    private static final int PUT_SOUP_BASE = 0;
    private static final int PUT_INGREDIENT = 1;
    private static final int COOKING = 2;
    private static final int FINISHED = 3;

    @Override
    public void tick(MovementContext context) {
        if (context.world.isClientSide) {
            return;
        }

        // 获取当前方块信息
        StructureTemplate.StructureBlockInfo info = context.contraption.getBlocks().get(context.localPos);
        if (info == null || !(info.state().getBlock() instanceof StockpotBlock)) {
            return;
        }

        BlockState state = info.state();
        CompoundTag nbt = info.nbt();
        if (nbt == null) {
            return;
        }

        // 没放入汤底时，不进行任何tick
        int status = nbt.getInt(STATUS);
        if (status == PUT_SOUP_BASE) {
            return;
        }

        // 检查热源
        if (!hasHeatSource(context)) {
            return;
        }

        // 检查是否有盖子
        boolean hasLid = state.getValue(StockpotBlock.HAS_LID);

        // 音效播放（每15tick）
        if (context.world.getGameTime() % 15 == 0) {
            playStockpotSound(context, hasLid);
        }

        // 没有盖子时，不进行任何tick，只生成粒子
        if (!hasLid) {
            spawnParticleWithoutLid(context, nbt);
            return;
        } else {
            // 有盖子时，生成白色粒子
            spawnParticleWithLid(context);
        }

        // 执行tick逻辑
        tickStockpot(context, state, nbt, info);
    }

    /**
     * 执行Stockpot的tick逻辑（参考StockpotBlockEntity.tick()）
     */
    private void tickStockpot(MovementContext context, BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        int status = nbt.getInt(STATUS);
        int currentTick = nbt.getInt(CURRENT_TICK);

        // 如果当前状态是放入素材，且素材不为空
        // 因为 isEmpty() 可能耗时，所以每隔 5 tick 检查一次
        if (status == PUT_INGREDIENT && context.world.getGameTime() % 5 == 0 && !isEmpty(nbt)) {
            startCooking(context, state, nbt, info);
            return;
        }

        // 如果当前状态是烹饪中，递减当前 tick
        if (status == COOKING) {
            if (currentTick > 0) {
                // 更新currentTick，不需要同步
                CompoundTag newNbt = nbt.copy();
                newNbt.putInt(CURRENT_TICK, currentTick - 1);
                ContraptionDataUtil.updateContraptionData(context, state, newNbt, false);
                return;
            }
            // 烹饪完成
            finishCooking(context, state, nbt, info);
        }
    }

    /**
     * 开始烹饪
     */
    private void startCooking(MovementContext context, BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        NonNullList<ItemStack> inputs = readInputs(nbt);
        ResourceLocation soupBaseId = ResourceLocation.tryParse(nbt.getString(SOUP_BASE_ID));
        if (soupBaseId == null) {
            soupBaseId = ModSoupBases.WATER;
        }

        StockpotContainer container = new StockpotContainer(inputs, soupBaseId);

        // 匹配配方
        var recipeOptional = context.world.getRecipeManager().getRecipeFor(ModRecipes.STOCKPOT_RECIPE, container, context.world);

        CompoundTag newNbt = nbt.copy();
        newNbt.putInt(STATUS, COOKING);

        recipeOptional.ifPresentOrElse(recipe -> {
            // 如果合成表符合
            newNbt.putString(RECIPE_ID, recipe.getId().toString());
            newNbt.putString(CARRIER, recipe.carrier().toJson().toString());
            newNbt.put(RESULT, recipe.assemble(container, context.world.registryAccess()).serializeNBT());
            newNbt.putInt(CURRENT_TICK, recipe.time());
            newNbt.putInt(TAKEOUT_COUNT, Math.min(recipe.getResultItem(context.world.registryAccess()).getCount(), StockpotRecipeSerializer.DEFAULT_TIME));
        }, () -> {
            // 不符合，进入迷之炖菜阶段
            newNbt.putString(RECIPE_ID, StockpotRecipeSerializer.EMPTY_ID.toString());
            newNbt.putString(CARRIER, Ingredient.of(Items.BOWL).toJson().toString());
            newNbt.put(RESULT, new ItemStack(Items.SUSPICIOUS_STEW).serializeNBT());
            newNbt.putInt(CURRENT_TICK, StockpotRecipeSerializer.DEFAULT_TIME);
            newNbt.putInt(TAKEOUT_COUNT, 1);
        });

        ContraptionDataUtil.updateContraptionData(context, state, newNbt, true);
    }

    /**
     * 完成烹饪
     */
    private void finishCooking(MovementContext context, BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        CompoundTag newNbt = nbt.copy();
        newNbt.putInt(STATUS, FINISHED);
        newNbt.putInt(CURRENT_TICK, -1);

        // 清空inputs
        newNbt.put(INPUTS, ContainerHelper.saveAllItems(new CompoundTag(),
                NonNullList.withSize(StockpotRecipe.RECIPES_SIZE, ItemStack.EMPTY)));

        ContraptionDataUtil.updateContraptionData(context, state, newNbt, true);
    }

    /**
     * 检查是否有热源
     */
    private boolean hasHeatSource(MovementContext context) {
        Contraption contraption = context.contraption;
        BlockPos belowLocalPos = context.localPos.below();

        // 首先检查Contraption内部下方是否有方块
        StructureTemplate.StructureBlockInfo belowInfo = contraption.getBlocks().get(belowLocalPos);
        if (belowInfo != null) {
            BlockState belowState = belowInfo.state();
            // 检查是否有LIT属性
            if (belowState.hasProperty(BlockStateProperties.LIT)) {
                return belowState.getValue(BlockStateProperties.LIT);
            }
            // 检查是否在热源标签中
            return belowState.is(TagMod.HEAT_SOURCE_BLOCKS_WITHOUT_LIT);
        }

        // Contraption内部没有下方方块，检查世界中Contraption实体下方的方块
        if (context.contraption.entity == null) {
            return false;
        }

        Vec3 globalPos = context.contraption.entity.toGlobalVector(Vec3.atCenterOf(context.localPos), 1.0f);
        BlockPos worldPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
        BlockPos worldBelowPos = worldPos.below();

        BlockState worldBelowState = context.world.getBlockState(worldBelowPos);
        if (worldBelowState.hasProperty(BlockStateProperties.LIT)) {
            return worldBelowState.getValue(BlockStateProperties.LIT);
        }
        return worldBelowState.is(TagMod.HEAT_SOURCE_BLOCKS_WITHOUT_LIT);
    }

    /**
     * 播放汤锅音效
     */
    private void playStockpotSound(MovementContext context, boolean hasLid) {
        if (context.contraption.entity == null) return;
        Vec3 globalPos = getGlobalPos(context);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);

        float volume = hasLid ? 0.075f : 0.2f;
        float pitch = hasLid ? 0.1f + context.world.random.nextFloat() * 0.05f : 1f + context.world.random.nextFloat() * 0.1f;

        context.world.playSound(null, soundPos,
                SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, SoundSource.BLOCKS, volume, pitch);
    }

    /**
     * 有盖子时生成粒子
     */
    private void spawnParticleWithLid(MovementContext context) {
        if (!(context.world instanceof ServerLevel serverLevel)) return;
        if (serverLevel.random.nextFloat() >= 0.05F) return;

        Vec3 globalPos = getGlobalPos(context);
        RandomSource random = serverLevel.random;

        serverLevel.sendParticles(ModParticles.COOKING.get(),
                globalPos.x + random.nextDouble() / 3 * (random.nextBoolean() ? 1 : -1),
                globalPos.y - 0.25 + random.nextDouble() / 3,
                globalPos.z + random.nextDouble() / 3 * (random.nextBoolean() ? 1 : -1),
                1, 0, 0, 0, 0.05);
    }

    /**
     * 无盖子时生成粒子
     */
    private void spawnParticleWithoutLid(MovementContext context, CompoundTag nbt) {
        if (!(context.world instanceof ServerLevel serverLevel)) return;
        if (serverLevel.random.nextFloat() >= 0.25F) return;

        Vec3 globalPos = getGlobalPos(context);
        RandomSource random = serverLevel.random;

        // 这里简化处理，使用普通气泡粒子
        serverLevel.sendParticles(ModParticles.COOKING.get(),
                globalPos.x - 0.25 + (random.nextFloat() * 0.5F),
                globalPos.y - 0.25,
                globalPos.z - 0.25 + (random.nextFloat() * 0.5F),
                2,
                (random.nextFloat() - 0.5) * 0.1F, 0,
                (random.nextFloat() - 0.5) * 0.1F, 0);
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
        NonNullList<ItemStack> inputs = NonNullList.withSize(StockpotRecipe.RECIPES_SIZE, ItemStack.EMPTY);
        if (nbt.contains(INPUTS, Tag.TAG_COMPOUND)) {
            ContainerHelper.loadAllItems(nbt.getCompound(INPUTS), inputs);
        }
        return inputs;
    }

    /**
     * 检查汤锅是否为空
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
