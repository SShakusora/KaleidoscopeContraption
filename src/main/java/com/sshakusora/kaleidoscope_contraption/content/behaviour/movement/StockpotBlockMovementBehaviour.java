package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.StockpotBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.client.particle.StockpotParticleOptions;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.container.StockpotContainer;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.StockpotRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.StockpotVisuals;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.serializer.StockpotRecipeSerializer;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.soupbase.SoupBaseManager;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModParticles;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModSounds;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModSoupBases;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.Quality;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.QualityEvaluator;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.QualityUtils;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionDataUtil;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
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
            if (context.contraption.getBlockEntityClientSide(context.localPos)
                    instanceof StockpotBlockEntity blockEntity) {
                blockEntity.clientTick();
            }
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
        if (!ContraptionInteractionUtil.hasHeatSource(context)) {
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

        CompoundTag newNbt = nbt.copy();
        newNbt.putInt(STATUS, COOKING);

        var manager = context.world.getRecipeManager();
        var recipe = manager.getRecipeFor(ModRecipes.STOCKPOT_RECIPE, container, context.world);
        if (recipe.isPresent()) {
            ItemStack result = recipe.get().assemble(container, context.world.registryAccess());
            applyRecipe(newNbt, recipe.get().getId(), recipe.get().carrier(), result, recipe.get().time());
        } else {
            var flexRecipe = manager.getRecipeFor(ModRecipes.FLEX_STOCKPOT_RECIPE, container, context.world);
            if (flexRecipe.isPresent()) {
                ItemStack result = flexRecipe.get().assemble(container, context.world.registryAccess());
                if (context.world instanceof ServerLevel serverLevel) {
                    Quality quality = QualityEvaluator.evaluate(
                            inputs, flexRecipe.get().ingredients(), flexRecipe.get().getId(), serverLevel.getSeed());
                    QualityUtils.setQuality(result, quality);
                }
                applyRecipe(newNbt, flexRecipe.get().getId(), flexRecipe.get().carrier(), result, flexRecipe.get().time());
            } else {
                applyRecipe(newNbt, StockpotRecipeSerializer.EMPTY_ID, Ingredient.of(Items.BOWL),
                        new ItemStack(Items.SUSPICIOUS_STEW), StockpotRecipeSerializer.DEFAULT_TIME);
            }
        }

        ContraptionDataUtil.updateContraptionData(context, state, newNbt, true);
    }

    private void applyRecipe(CompoundTag nbt, ResourceLocation recipeId, Ingredient carrier,
                             ItemStack result, int time) {
        nbt.putString(RECIPE_ID, recipeId.toString());
        nbt.putString(CARRIER, carrier.toJson().toString());
        nbt.put(RESULT, result.serializeNBT());
        nbt.putInt(CURRENT_TICK, time);
        nbt.putInt(TAKEOUT_COUNT, Math.min(result.getCount(), 9));
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
     * 播放汤锅音效
     */
    private void playStockpotSound(MovementContext context, boolean hasLid) {
        if (context.contraption.entity == null) return;
        Vec3 globalPos = getGlobalPos(context);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);

        float volume = hasLid ? 0.075f : 0.2f;
        float pitch = hasLid ? 0.1f + context.world.random.nextFloat() * 0.05f : 1f + context.world.random.nextFloat() * 0.1f;

        context.world.playSound(null, soundPos,
                ModSounds.BLOCK_STOCKPOT.get(), SoundSource.BLOCKS, volume, pitch);
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

        int color = getBubbleColor(context, nbt);
        serverLevel.sendParticles(new StockpotParticleOptions(Vec3.fromRGB24(color).toVector3f(), 1.0F),
                globalPos.x - 0.25 + (random.nextFloat() * 0.5F),
                globalPos.y - 0.25,
                globalPos.z - 0.25 + (random.nextFloat() * 0.5F),
                2,
                (random.nextFloat() - 0.5) * 0.1F, 0,
                (random.nextFloat() - 0.5) * 0.1F, 0);
    }

    private int getBubbleColor(MovementContext context, CompoundTag nbt) {
        int status = nbt.getInt(STATUS);
        ResourceLocation recipeId = ResourceLocation.tryParse(nbt.getString(RECIPE_ID));
        if (recipeId != null && !recipeId.equals(StockpotRecipeSerializer.EMPTY_ID)) {
            StockpotVisuals visuals = null;
            StockpotRecipe recipe = context.world.getRecipeManager()
                    .getAllRecipesFor(ModRecipes.STOCKPOT_RECIPE).stream()
                    .filter(candidate -> candidate.getId().equals(recipeId))
                    .findFirst().orElse(null);
            if (recipe != null) {
                visuals = recipe.visuals();
            } else {
                var flexRecipe = context.world.getRecipeManager()
                        .getAllRecipesFor(ModRecipes.FLEX_STOCKPOT_RECIPE).stream()
                        .filter(candidate -> candidate.getId().equals(recipeId))
                        .findFirst().orElse(null);
                if (flexRecipe != null) {
                    visuals = flexRecipe.visuals();
                }
            }
            if (visuals != null) {
                if (status == COOKING) return visuals.cookingBubbleColor();
                if (status == FINISHED) return visuals.finishedBubbleColor();
            }
        }

        ResourceLocation soupBaseId = ResourceLocation.tryParse(nbt.getString(SOUP_BASE_ID));
        var soupBase = SoupBaseManager.getSoupBase(soupBaseId);
        return soupBase != null ? soupBase.getBubbleColor() : 0xFFFFFF;
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
