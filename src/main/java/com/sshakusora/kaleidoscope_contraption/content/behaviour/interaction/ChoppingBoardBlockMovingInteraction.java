package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.advancements.critereon.ModEventTriggerType;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.ChoppingBoardBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.ChoppingBoardRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModEnchantments;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModTrigger;
import com.github.ysbbbbbb.kaleidoscopecookery.init.tag.TagMod;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.List;
import java.util.stream.IntStream;

public class ChoppingBoardBlockMovingInteraction extends MovingInteractionBehaviour {

    private static final double DURABILITY_COST_PROBABILITY = 0.25;

    private static final String MODEL_ID = "ModelId";
    private static final String CURRENT_CUT_STACK = "CurrentCutStack";
    private static final String RESULT_ITEM = "ResultItem";
    private static final String MAX_CUT_COUNT = "MaxCutCount";
    private static final String CURRENT_CUT_COUNT = "CurrentCutCount";

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return false;
        }

        BlockState state = info.state();
        if (!(state.getBlock() instanceof ChoppingBoardBlock)) {
            return false;
        }

        ItemStack itemInHand = player.getItemInHand(activeHand);

        // 处理按下移除键取下ChoppingBoardBlock
        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            if (removeChoppingBoardBlock(player, contraptionEntity, localPos, activeHand)) {
                return true;
            }
        }

        CompoundTag nbt = info.nbt();
        if (nbt == null) {
            nbt = new CompoundTag();
        }

        // 读取当前状态
        ItemStack currentCutStack = nbt.contains(CURRENT_CUT_STACK) ? ItemStack.parseOptional(contraptionEntity.level().registryAccess(), nbt.getCompound(CURRENT_CUT_STACK)) : ItemStack.EMPTY;
        List<ItemStack> results = readResults(nbt, contraptionEntity);
        int currentCutCount = nbt.getInt(CURRENT_CUT_COUNT);
        int maxCutCount = nbt.getInt(MAX_CUT_COUNT);

        // 主手交互逻辑
        if (activeHand == InteractionHand.MAIN_HAND) {
            // 1. 尝试放置物品到切菜板
            if (results.isEmpty() && !itemInHand.isEmpty()) {
                if (tryPutItem(player, contraptionEntity, localPos, state, nbt, info, itemInHand)) {
                    return true;
                }
            }

            // 2. 尝试切菜或取出成品
            if (!results.isEmpty()) {
                // 如果已经切完，执行取出逻辑
                if (currentCutCount >= maxCutCount) {
                    if (tryTakeOutResult(player, contraptionEntity, localPos, state, nbt, info)) {
                        return true;
                    }
                } else if (itemInHand.is(TagMod.KITCHEN_KNIFE)) {
                    // 否则，检测是否是刀具，进行切菜逻辑
                    if (tryCutItem(player, contraptionEntity, localPos, state, nbt, info, itemInHand)) {
                        return true;
                    }
                }
            }
        }

        // 3. Shift+右键取出未切的物品
        if (player.isSecondaryUseActive() && currentCutCount == 0 && !currentCutStack.isEmpty()) {
            if (tryTakeOutRawItem(player, contraptionEntity, localPos, state, nbt, info)) {
                return true;
            }
        }

        // 4. 主手是刀且副手为空时，播放粒子效果（空切）
        if (itemInHand.is(TagMod.KITCHEN_KNIFE) && player.getOffhandItem().isEmpty()) {
            playCutEffect(contraptionEntity, localPos);
            return true;
        }

        return false;
    }

    /**
     * 尝试放置物品到切菜板
     */
    private boolean tryPutItem(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info,
                                ItemStack putOnItem) {
        SingleRecipeInput input = new SingleRecipeInput(putOnItem);
        var recipeOptional = contraptionEntity.level().getRecipeManager()
                .getRecipeFor(ModRecipes.CHOPPING_BOARD_RECIPE, input, contraptionEntity.level());

        if (recipeOptional.isPresent()) {
            ChoppingBoardRecipe recipe = recipeOptional.get().value();

            if (!contraptionEntity.level().isClientSide) {
                CompoundTag newNbt = nbt.copy();
                newNbt.putString(MODEL_ID, recipe.getModelId().toString());
                newNbt.putInt(MAX_CUT_COUNT, recipe.getCutCount());
                newNbt.putInt(CURRENT_CUT_COUNT, 0);
                newNbt.put(CURRENT_CUT_STACK, putOnItem.split(1).save(contraptionEntity.level().registryAccess(), new CompoundTag()));
                newNbt.put(RESULT_ITEM, saveResults(recipe.getResults(), contraptionEntity));

                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        info.pos(), state, newNbt);
                ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);
            }

            // 播放音效
            Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
            BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
            contraptionEntity.level().playSound(null, soundPos,
                    SoundEvents.WOOD_PLACE,
                    SoundSource.BLOCKS,
                    1, 1.2F);
            return true;
        }
        return false;
    }

    /**
     * 尝试切菜
     */
    private boolean tryCutItem(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info,
                                ItemStack cutterItem) {
        int currentCutCount = nbt.getInt(CURRENT_CUT_COUNT);
        int maxCutCount = nbt.getInt(MAX_CUT_COUNT);

        if (currentCutCount >= maxCutCount) {
            return false;
        }

        if (!contraptionEntity.level().isClientSide) {
            CompoundTag newNbt = nbt.copy();
            int enchantmentLevel = contraptionEntity.level().registryAccess()
                    .registryOrThrow(Registries.ENCHANTMENT)
                    .getHolder(ModEnchantments.QUICK_KNIFE)
                    .map(cutterItem::getEnchantmentLevel)
                    .orElse(0);
            enchantmentLevel = Mth.clamp(enchantmentLevel, 0, 2);
            newNbt.putInt(CURRENT_CUT_COUNT,
                    Math.min(maxCutCount, currentCutCount + (1 << enchantmentLevel)));

            StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                    info.pos(), state, newNbt);
            ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);

            // 切菜成功时，有 25% 的概率消耗耐久度
            if (contraptionEntity.level().random.nextDouble() < DURABILITY_COST_PROBABILITY) {
                cutterItem.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }

            ModTrigger.EVENT.get().trigger(player, ModEventTriggerType.USE_CHOPPING_BOARD);
        }

        // 播放音效和粒子
        playCutEffect(contraptionEntity, localPos);
        return true;
    }

    /**
     * 尝试取出成品
     */
    private boolean tryTakeOutResult(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                      BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        List<ItemStack> results = readResults(nbt, contraptionEntity);

        if (results.isEmpty()) {
            return false;
        }

        if (!contraptionEntity.level().isClientSide) {
            // 掉落成品
            for (ItemStack result : results) {
                ContraptionInteractionUtil.popResource(contraptionEntity, localPos, result.copy());
            }

            // 重置切菜板数据
            resetChoppingBoard(contraptionEntity, localPos, state, nbt, info);
        }

        // 播放音效
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
        contraptionEntity.level().playSound(null, soundPos,
                SoundEvents.WOOD_PLACE,
                SoundSource.BLOCKS,
                1, 2 + contraptionEntity.level().random.nextFloat() * 0.2f);
        return true;
    }

    /**
     * 尝试取出未切的原料
     */
    private boolean tryTakeOutRawItem(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                       BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        int currentCutCount = nbt.getInt(CURRENT_CUT_COUNT);
        ItemStack currentCutStack = nbt.contains(CURRENT_CUT_STACK) ? ItemStack.parseOptional(contraptionEntity.level().registryAccess(), nbt.getCompound(CURRENT_CUT_STACK)) : ItemStack.EMPTY;

        if (currentCutCount != 0 || currentCutStack.isEmpty()) {
            return false;
        }

        if (!contraptionEntity.level().isClientSide) {
            // 返还物品给玩家
            ItemHandlerHelper.giveItemToPlayer(player, currentCutStack);

            // 重置切菜板数据
            resetChoppingBoard(contraptionEntity, localPos, state, nbt, info);
        }

        // 播放音效
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
        contraptionEntity.level().playSound(null, soundPos,
                SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                SoundSource.BLOCKS,
                1, 1.2f + contraptionEntity.level().random.nextFloat() * 0.2f);
        return true;
    }

    /**
     * 重置切菜板数据
     */
    private void resetChoppingBoard(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                     BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        CompoundTag newNbt = new CompoundTag();
        newNbt.putInt(MAX_CUT_COUNT, 0);
        newNbt.putInt(CURRENT_CUT_COUNT, 0);
        newNbt.put(CURRENT_CUT_STACK, ItemStack.EMPTY.saveOptional(contraptionEntity.level().registryAccess()));
        newNbt.put(RESULT_ITEM, new ListTag());

        StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                info.pos(), state, newNbt);
        ContraptionInteractionUtil.updateContraptionDataWithResetRenderer(contraptionEntity, localPos, newInfo);
    }

    private List<ItemStack> readResults(CompoundTag nbt, AbstractContraptionEntity contraptionEntity) {
        if (nbt.contains(RESULT_ITEM, Tag.TAG_LIST)) {
            ListTag resultTag = nbt.getList(RESULT_ITEM, Tag.TAG_COMPOUND);
            return IntStream.range(0, resultTag.size())
                    .mapToObj(index -> ItemStack.parseOptional(
                            contraptionEntity.level().registryAccess(), resultTag.getCompound(index)))
                    .filter(stack -> !stack.isEmpty())
                    .toList();
        }
        if (nbt.contains(RESULT_ITEM, Tag.TAG_COMPOUND)) {
            ItemStack legacyResult = ItemStack.parseOptional(
                    contraptionEntity.level().registryAccess(), nbt.getCompound(RESULT_ITEM));
            return legacyResult.isEmpty() ? List.of() : List.of(legacyResult);
        }
        return List.of();
    }

    private ListTag saveResults(List<ItemStack> results, AbstractContraptionEntity contraptionEntity) {
        ListTag resultTag = new ListTag();
        for (ItemStack result : results) {
            if (!result.isEmpty()) {
                resultTag.add(result.saveOptional(contraptionEntity.level().registryAccess()));
            }
        }
        return resultTag;
    }

    /**
     * 播放切菜音效和粒子效果
     */
    private void playCutEffect(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);

        if (contraptionEntity.level() instanceof ServerLevel level) {
            RandomSource random = level.getRandom();
            level.sendParticles(ParticleTypes.CRIT,
                    globalPos.x + 0.25 + random.nextDouble() / 2,
                    globalPos.y + 0.25,
                    globalPos.z + 0.25 + random.nextDouble() / 2,
                    2, 0, 0, 0, 0.1);
        }

        contraptionEntity.level().playSound(null, soundPos,
                SoundEvents.WOOD_PLACE,
                SoundSource.BLOCKS,
                1, 1.5f + contraptionEntity.level().random.nextFloat() * 0.4f);
    }

    /**
     * 移除ChoppingBoardBlock
     */
    private boolean removeChoppingBoardBlock(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos, InteractionHand activeHand) {
        StructureTemplate.StructureBlockInfo aboveInfo = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (aboveInfo == null || !(aboveInfo.state().getBlock() instanceof ChoppingBoardBlock)) {
            return false;
        }

        // 检查切菜板上是否有物品，如果有则先取出
        CompoundTag nbt = aboveInfo.nbt();
        if (nbt != null) {
            ItemStack currentCutStack = nbt.contains(CURRENT_CUT_STACK) ? ItemStack.parseOptional(contraptionEntity.level().registryAccess(), nbt.getCompound(CURRENT_CUT_STACK)) : ItemStack.EMPTY;
            if (!currentCutStack.isEmpty()) {
                // 有物品时不能取下，提示先取出
                return false;
            }
        }

        // 在服务端执行移除逻辑
        if (!contraptionEntity.level().isClientSide) {
            // 从Contraption中移除方块
            ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);

            // 更新Contraption的bounds - 移除方块后需要重新计算
            var updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);

            // 掉落ChoppingBoardBlock物品给玩家
            ItemStack boardItem = new ItemStack(ModBlocks.CHOPPING_BOARD.get());
            if (!player.isCreative()) {
                player.getInventory().placeItemBackInInventory(boardItem);
            }

            // 通知客户端重新渲染Contraption（同步bounds）
            ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, updatedBounds);

            // 播放破坏音效
            ContraptionInteractionUtil.playBreakSound(contraptionEntity, localPos, aboveInfo.state());
        }

        return true;
    }

}
