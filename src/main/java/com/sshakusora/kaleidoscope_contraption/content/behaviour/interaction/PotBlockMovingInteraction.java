package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.advancements.critereon.ModEventTriggerType;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.PotBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.container.SimpleInput;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.PotRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModTrigger;
import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.FoodBiteRegistry;
import com.github.ysbbbbbb.kaleidoscopecookery.init.tag.TagMod;
import com.github.ysbbbbbb.kaleidoscopecookery.item.KitchenShovelItem;
import com.github.ysbbbbbb.kaleidoscopecookery.item.OilPotItem;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.Quality;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.QualityEvaluator;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.QualityUtils;
import com.github.ysbbbbbb.kaleidoscopecookery.util.ItemUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.api.placement.ContraptionRemovalManager;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import static com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.PotBlock.HAS_OIL;
import static com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.PotBlock.SHOW_OIL;
import static com.github.ysbbbbbb.kaleidoscopecookery.init.registry.FoodBiteRegistry.DARK_CUISINE;
import static com.github.ysbbbbbb.kaleidoscopecookery.init.registry.FoodBiteRegistry.SUSPICIOUS_STIR_FRY;

public class PotBlockMovingInteraction extends MovingInteractionBehaviour {

    private static final int PUT_INGREDIENT_TIME = 60 * 20;
    private static final int TAKEOUT_TIME = 40 * 20;
    private static final int BURNT_TIME = 20 * 20;
    private static final double DURABILITY_COST_PROBABILITY = 0.25;

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
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return false;
        }

        BlockState state = info.state();
        if (!(state.getBlock() instanceof PotBlock)) {
            return false;
        }

        ItemStack itemInHand = player.getItemInHand(activeHand);

        // 处理按下移除键取下PotBlock
        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return ContraptionRemovalManager.tryRemove(player, localPos, contraptionEntity)
                    == ContraptionRemovalManager.Result.REMOVED;
        }

        CompoundTag nbt = info.nbt();
        if (nbt == null) {
            nbt = new CompoundTag();
        }

        // 读取当前状态
        int status = nbt.getInt(STATUS);

        // 先检查执行配菜取出逻辑
        if ((itemInHand.isEmpty() || itemInHand.is(TagMod.INGREDIENT_CONTAINER)) && removeIngredient(player, contraptionEntity, localPos, state, nbt, info)) {
            return true;
        }

        // 再检查成品取出逻辑
        if (takeOutProduct(player, contraptionEntity, localPos, state, nbt, itemInHand, info)) {
            return true;
        }

        // 检查热源
        if (!ContraptionInteractionUtil.hasHeatSource(contraptionEntity, localPos)) {
            sendActionBarMessage(player, "need_lit_stove");
            return true;
        }

        // 检查油
        if (!state.getValue(HAS_OIL)) {
            if (onPlaceOil(player, contraptionEntity, localPos, state, nbt, itemInHand, info)) {
                return true;
            } else {
                sendActionBarMessage(player, "need_oil");
                return true;
            }
        }

        // 如果拿着锅铲，那么开始执行锅铲逻辑
        if (itemInHand.is(TagMod.KITCHEN_SHOVEL)) {
            if (contraptionEntity.level().random.nextDouble() < DURABILITY_COST_PROBABILITY) {
                itemInHand.hurtAndBreak(1, player,
                        activeHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
            onShovelHit(player, contraptionEntity, localPos, state, nbt, info);
            Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
            BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
            contraptionEntity.level().playSound(player, soundPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F,
                    1F + (contraptionEntity.level().random.nextFloat() - contraptionEntity.level().random.nextFloat()) * 0.8F);
            return true;
        }

        // 放入配菜
        if (addIngredient(player, contraptionEntity, localPos, state, nbt, itemInHand, info)) {
            return true;
        }

        return false;
    }

    /**
     * 执行放油逻辑
     */
    private boolean onPlaceOil(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                               BlockState state, CompoundTag nbt, ItemStack stack, StructureTemplate.StructureBlockInfo info) {
        if (stack.is(TagMod.OIL)) {
            // 普通情况油脂
            placeOil(contraptionEntity, localPos, state, nbt, player, info);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            ModTrigger.EVENT.get().trigger(player, ModEventTriggerType.PUT_OIL_IN_POT);
            return true;
        } else if (stack.is(ModItems.KITCHEN_SHOVEL.get()) && KitchenShovelItem.hasOil(stack)) {
            // 带油锅铲特判
            placeOil(contraptionEntity, localPos, state, nbt, player, info);
            KitchenShovelItem.setHasOil(stack, false);
            ModTrigger.EVENT.get().trigger(player, ModEventTriggerType.PUT_OIL_IN_POT);
            return true;
        } else if (stack.is(ModItems.OIL_POT.get()) && OilPotItem.hasOil(stack)) {
            // 油壶特判
            placeOil(contraptionEntity, localPos, state, nbt, player, info);
            OilPotItem.shrinkOilCount(stack);
            ModTrigger.EVENT.get().trigger(player, ModEventTriggerType.PUT_OIL_IN_POT);
            return true;
        }
        return false;
    }

    private void placeOil(AbstractContraptionEntity contraptionEntity, BlockPos localPos, BlockState state,
                          CompoundTag nbt, Player player, StructureTemplate.StructureBlockInfo info) {
        if (contraptionEntity.level().isClientSide) {
            return;
        }

        // 更新NBT
        CompoundTag newNbt = nbt.copy();
        newNbt.putInt(CURRENT_TICK, PUT_INGREDIENT_TIME);
        newNbt.putInt(STATUS, PUT_INGREDIENT);

        // 更新BlockState
        BlockState newState = state.setValue(HAS_OIL, true).setValue(SHOW_OIL, true);

        StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                info.pos(), newState, newNbt);
        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);

        // 播放音效
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
        contraptionEntity.level().playSound(player, soundPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1F,
                (contraptionEntity.level().random.nextFloat() - contraptionEntity.level().random.nextFloat()) * 0.8F);
    }

    /**
     * 添加原料到锅中
     */
    private boolean addIngredient(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                  BlockState state, CompoundTag nbt, ItemStack itemStack, StructureTemplate.StructureBlockInfo info) {
        int status = nbt.getInt(STATUS);
        if (status != PUT_INGREDIENT) {
            return false;
        }

        // 黑名单物品不可放入
        if (itemStack.is(TagMod.INGREDIENT_BLOCKLIST)) {
            return false;
        }

        // 读取当前原料
        NonNullList<ItemStack> inputs = readInputs(nbt, contraptionEntity.level());

        for (int i = 0; i < inputs.size(); i++) {
            ItemStack item = inputs.get(i);
            if (item.isEmpty()) {
                // 如果带有容器，此时返还容器
                Item containerItem = ItemUtils.getContainerItem(itemStack);
                if (containerItem != Items.AIR) {
                    ItemUtils.getItemToLivingEntity(player, containerItem.getDefaultInstance());
                }
                inputs.set(i, itemStack.split(1));

                // 更新NBT
                if (!contraptionEntity.level().isClientSide) {
                    CompoundTag newNbt = nbt.copy();
                    saveInputs(newNbt, inputs, contraptionEntity.level());
                    StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                            info.pos(), state, newNbt);
                    ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);
                }

                // 播放音效
                Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
                BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
                contraptionEntity.level().playSound(null, soundPos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 1.0F, 0.5F);
                return true;
            }
        }
        return false;
    }

    /**
     * 移除锅中的原料
     */
    private boolean removeIngredient(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                     BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        int status = nbt.getInt(STATUS);
        if (status != PUT_INGREDIENT) {
            return false;
        }

        // 读取当前原料
        NonNullList<ItemStack> inputs = readInputs(nbt, contraptionEntity.level());

        for (int i = inputs.size() - 1; i >= 0; i--) {
            ItemStack stack = inputs.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            // 检查容器是否符合取出条件
            if (!containerIsMatch(player, stack)) {
                return false;
            }
            inputs.set(i, ItemStack.EMPTY);

            // 掉落物品给玩家
            if (!contraptionEntity.level().isClientSide) {
                ItemUtils.getItemToLivingEntity(player, stack);

                // 更新NBT
                CompoundTag newNbt = nbt.copy();
                saveInputs(newNbt, inputs, contraptionEntity.level());
                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        info.pos(), state, newNbt);
                ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);
            }

            if (ContraptionInteractionUtil.hasHeatSource(contraptionEntity, localPos)) {
                player.hurt(contraptionEntity.level().damageSources().inFire(), 1);
                ModTrigger.EVENT.get().trigger(player, ModEventTriggerType.HURT_WHEN_TAKEOUT_FROM_POT);
            }

            return true;
        }
        return false;
    }

    private boolean containerIsMatch(Player player, ItemStack stack) {
        Item containerItem = ItemUtils.getContainerItem(stack);
        if (containerItem == Items.AIR) {
            return true;
        }
        if (player.getMainHandItem().is(containerItem)) {
            player.getMainHandItem().shrink(1);
            return true;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable("tip.kaleidoscope_cookery.kitchen.remove_ingredient.need_container",
                    containerItem.getDefaultInstance().getHoverName()));
        }
        return false;
    }

    /**
     * 锅铲点击锅时的逻辑
     */
    private void onShovelHit(Player user, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                             BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        int status = nbt.getInt(STATUS);

        // 每次翻炒给点粒子效果
        if (!contraptionEntity.level().isClientSide) {
            // 更新seed用于动画
            CompoundTag newNbt = nbt.copy();
            newNbt.putLong(SEED, System.currentTimeMillis());

            // 起锅烧油，放入食材阶段
            if (status == PUT_INGREDIENT) {
                if (!isEmpty(newNbt, contraptionEntity.level())) {
                    startCooking(contraptionEntity, localPos, state, newNbt, info);
                    ModTrigger.EVENT.get().trigger(user, ModEventTriggerType.STIR_FRY_IN_POT);
                }
            }

            // 炒菜阶段
            if (status == COOKING) {
                int stirFryCount = newNbt.getInt(STIR_FRY_COUNT);
                if (stirFryCount > 0) {
                    newNbt.putInt(STIR_FRY_COUNT, stirFryCount - 1);
                }
                ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, new StructureTemplate.StructureBlockInfo(
                        info.pos(), state, newNbt));
                ModTrigger.EVENT.get().trigger(user, ModEventTriggerType.STIR_FRY_IN_POT);
            }
        }
    }

    /**
     * 开始炒菜
     */
    private void startCooking(AbstractContraptionEntity contraptionEntity, BlockPos localPos, BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        NonNullList<ItemStack> inputs = readInputs(nbt, contraptionEntity.level());
        SimpleInput input = new SimpleInput(inputs);

        CompoundTag newNbt = nbt.copy();
        newNbt.putInt(STATUS, COOKING);

        var manager = contraptionEntity.level().getRecipeManager();
        var recipe = manager.getRecipeFor(ModRecipes.POT_RECIPE, input, contraptionEntity.level());
        if (recipe.isPresent()) {
            var value = recipe.get().value();
            applyRecipe(newNbt, value.carrier(),
                    value.assemble(input, contraptionEntity.level().registryAccess()),
                    value.time(), value.stirFryCount(), contraptionEntity.level());
        } else {
            var flexRecipe = manager.getRecipeFor(ModRecipes.FLEX_POT_RECIPE, input, contraptionEntity.level());
            if (flexRecipe.isPresent()) {
                var value = flexRecipe.get().value();
                ItemStack result = value.assemble(input, contraptionEntity.level().registryAccess());
                if (contraptionEntity.level() instanceof ServerLevel serverLevel) {
                    Quality quality = QualityEvaluator.evaluate(
                            inputs, value.ingredients(), flexRecipe.get().id(), serverLevel.getSeed());
                    QualityUtils.setQuality(result, quality);
                }
                applyRecipe(newNbt, value.carrier(), result,
                        value.time(), value.stirFryCount(), contraptionEntity.level());
            } else {
                applyRecipe(newNbt, Ingredient.of(Items.BOWL),
                        new ItemStack(FoodBiteRegistry.getItem(SUSPICIOUS_STIR_FRY)), 10 * 20, 0,
                        contraptionEntity.level());
            }
        }

        StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                info.pos(), state, newNbt);
        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);
    }

    private void applyRecipe(CompoundTag nbt, Ingredient carrier, ItemStack result, int time, int stirFryCount, Level level) {
        nbt.putString(CARRIER, Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, carrier).getOrThrow().toString());
        nbt.put(RESULT, result.save(level.registryAccess(), new CompoundTag()));
        nbt.putInt(CURRENT_TICK, time);
        nbt.putInt(STIR_FRY_COUNT, stirFryCount);
    }

    /**
     * 从锅中取出产品
     */
    private boolean takeOutProduct(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                   BlockState state, CompoundTag nbt, ItemStack stack, StructureTemplate.StructureBlockInfo info) {
        int status = nbt.getInt(STATUS);

        // 仅在炒菜完成或炒糊阶段可以取出
        if (status != FINISHED && status != BURNT) {
            return false;
        }

        // 烧焦时取出的是黑暗料理
        ItemStack finallyResult = status == FINISHED ? readResult(nbt, contraptionEntity.level()) : new ItemStack(FoodBiteRegistry.getItem(DARK_CUISINE));

        Ingredient carrier = readCarrier(nbt);
        if (!carrier.isEmpty()) {
            return takeOutWithCarrier(player, contraptionEntity, localPos, state, nbt, stack, finallyResult, carrier, info);
        } else {
            return takeOutWithoutCarrier(player, contraptionEntity, localPos, state, nbt, stack, finallyResult, info);
        }
    }

    private boolean takeOutWithoutCarrier(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                          BlockState state, CompoundTag nbt, ItemStack stack, ItemStack finallyResult, StructureTemplate.StructureBlockInfo info) {
        if (stack.is(TagMod.KITCHEN_SHOVEL)) {
            // 如果是玩家，则需要判断是否潜行才能取出
            if (!player.isSecondaryUseActive()) {
                return false;
            }
            if (!contraptionEntity.level().isClientSide) {
                ItemUtils.getItemToLivingEntity(player, finallyResult);
                reset(contraptionEntity, localPos, state, nbt, info);
            }
            return true;
        } else {
            if (ContraptionInteractionUtil.hasHeatSource(contraptionEntity, localPos)) {
                player.hurt(contraptionEntity.level().damageSources().inFire(), 1);
                ModTrigger.EVENT.get().trigger(player, ModEventTriggerType.HURT_WHEN_TAKEOUT_FROM_POT);
            }
            sendActionBarMessage(player, "need_kitchen_shovel");
            // 选择返回true，以便触发C2S，伤害得以生效
            return true;
        }
    }

    private boolean takeOutWithCarrier(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                       BlockState state, CompoundTag nbt, ItemStack mainHandItem,
                                       ItemStack finallyResult, Ingredient carrier, StructureTemplate.StructureBlockInfo info) {
        Component carrierName = carrier.getItems()[0].getHoverName();
        if (carrier.test(mainHandItem)) {
            if (mainHandItem.getCount() < finallyResult.getCount()) {
                sendActionBarMessage(player, "carrier_count_not_enough", finallyResult.getCount(), carrierName);
                return false;
            } else {
                if (!contraptionEntity.level().isClientSide) {
                    mainHandItem.shrink(finallyResult.getCount());
                    ItemUtils.getItemToLivingEntity(player, finallyResult);
                    reset(contraptionEntity, localPos, state, nbt, info);
                }
                return true;
            }
        }
        // 没有锅铲时才会触发提示
        if (!mainHandItem.is(TagMod.KITCHEN_SHOVEL)) {
            if (ContraptionInteractionUtil.hasHeatSource(contraptionEntity, localPos)) {
                player.hurt(contraptionEntity.level().damageSources().inFire(), 1);
                ModTrigger.EVENT.get().trigger(player, ModEventTriggerType.HURT_WHEN_TAKEOUT_FROM_POT);
            }
            sendActionBarMessage(player, "need_carrier", carrierName);
            // 选择返回true，以便触发C2S，伤害得以生效
            return true;
        }
        return false;
    }

    /**
     * 重置锅的状态
     */
    private void reset(AbstractContraptionEntity contraptionEntity, BlockPos localPos, BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        CompoundTag newNbt = new CompoundTag();
        newNbt.put(INPUTS, ContainerHelper.saveAllItems(new CompoundTag(), NonNullList.withSize(PotRecipe.RECIPES_SIZE, ItemStack.EMPTY), contraptionEntity.level().registryAccess()));
        newNbt.putString(CARRIER, Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, Ingredient.EMPTY).getOrThrow().toString());
        newNbt.put(RESULT, ItemStack.EMPTY.saveOptional(contraptionEntity.level().registryAccess()));
        newNbt.putInt(STATUS, PUT_INGREDIENT);
        newNbt.putInt(CURRENT_TICK, 0);
        newNbt.putInt(STIR_FRY_COUNT, 0);
        newNbt.putLong(SEED, System.currentTimeMillis());

        BlockState newState = state.setValue(HAS_OIL, false).setValue(SHOW_OIL, false);

        StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                info.pos(), newState, newNbt);
        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);
    }

    /**
     * 移除PotBlock
     */
    private boolean removePotBlock(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos, InteractionHand activeHand) {
        StructureTemplate.StructureBlockInfo aboveInfo = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (aboveInfo == null || !(aboveInfo.state().getBlock() instanceof PotBlock)) {
            return false;
        }

        // 在服务端执行移除逻辑
        if (!contraptionEntity.level().isClientSide) {
            // 从Contraption中移除方块
            ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);

            // 更新Contraption的bounds - 移除方块后需要重新计算
            var updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);

            // 掉落PotBlock物品给玩家
            ItemStack potItem = new ItemStack(ModBlocks.POT.get());
            if (!player.isCreative())
                player.getInventory().placeItemBackInInventory(potItem);

            // 通知客户端重新渲染Contraption（同步bounds）
            ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, updatedBounds);

            // 播放破坏音效
            ContraptionInteractionUtil.playBreakSound(contraptionEntity, localPos, aboveInfo.state());
        }

        return true;
    }

    /**
     * 检查锅是否为空
     */
    private boolean isEmpty(CompoundTag nbt, Level level) {
        NonNullList<ItemStack> inputs = readInputs(nbt, level);
        for (ItemStack stack : inputs) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 读取原料列表
     */
    private NonNullList<ItemStack> readInputs(CompoundTag nbt, Level level) {
        NonNullList<ItemStack> inputs = NonNullList.withSize(PotRecipe.RECIPES_SIZE, ItemStack.EMPTY);
        if (nbt.contains(INPUTS, Tag.TAG_COMPOUND)) {
            ContainerHelper.loadAllItems(nbt.getCompound(INPUTS), inputs, level.registryAccess());
        }
        return inputs;
    }

    /**
     * 保存原料列表
     */
    private void saveInputs(CompoundTag nbt, NonNullList<ItemStack> inputs, Level level) {
        nbt.put(INPUTS, ContainerHelper.saveAllItems(new CompoundTag(), inputs, level.registryAccess()));
    }

    /**
     * 读取载体
     */
    private Ingredient readCarrier(CompoundTag nbt) {
        if (nbt.contains(CARRIER, Tag.TAG_STRING)) {
            JsonElement element = JsonParser.parseString(nbt.getString(CARRIER));
            return Ingredient.CODEC.parse(JsonOps.INSTANCE, element).result().orElse(Ingredient.EMPTY);
        }
        return Ingredient.EMPTY;
    }

    /**
     * 读取结果
     */
    private ItemStack readResult(CompoundTag nbt, Level level) {
        if (nbt.contains(RESULT, Tag.TAG_COMPOUND)) {
            return ItemStack.parseOptional(level.registryAccess(), nbt.getCompound(RESULT));
        }
        return ItemStack.EMPTY;
    }

    /**
     * 获取容器
     */
    /**
     * 发送ActionBar消息
     */
    private void sendActionBarMessage(Player player, String type, Object... args) {
        if (player instanceof ServerPlayer serverPlayer) {
            String key = "tip.kaleidoscope_cookery.pot." + type;
            MutableComponent message = Component.translatable(key, args);
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
        }
    }

}
