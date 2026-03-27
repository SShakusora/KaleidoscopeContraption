package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.advancements.critereon.ModEventTriggerType;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.PotBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.PotRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModTrigger;
import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.FoodBiteRegistry;
import com.github.ysbbbbbb.kaleidoscopecookery.init.tag.TagCommon;
import com.github.ysbbbbbb.kaleidoscopecookery.init.tag.TagMod;
import com.github.ysbbbbbb.kaleidoscopecookery.item.KitchenShovelItem;
import com.github.ysbbbbbb.kaleidoscopecookery.item.OilPotItem;
import com.github.ysbbbbbb.kaleidoscopecookery.util.ItemUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.mixin.accessor.ContraptionAccessor;
import com.sshakusora.kaleidoscope_contraption.network.KCContraptionChangedPacket;
import com.sshakusora.kaleidoscope_contraption.network.KCPacketHandler;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionBoundsUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.MutablePair;

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
            if (removePotBlock(player, contraptionEntity, localPos, activeHand)) {
                return true;
            }
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
        if (!hasHeatSource(contraptionEntity, localPos)) {
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
                itemInHand.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
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
            ModTrigger.EVENT.trigger(player, ModEventTriggerType.PUT_OIL_IN_POT);
            return true;
        } else if (stack.is(ModItems.KITCHEN_SHOVEL.get()) && KitchenShovelItem.hasOil(stack)) {
            // 带油锅铲特判
            placeOil(contraptionEntity, localPos, state, nbt, player, info);
            KitchenShovelItem.setHasOil(stack, false);
            ModTrigger.EVENT.trigger(player, ModEventTriggerType.PUT_OIL_IN_POT);
            return true;
        } else if (stack.is(ModItems.OIL_POT.get()) && OilPotItem.hasOil(stack)) {
            // 油壶特判
            placeOil(contraptionEntity, localPos, state, nbt, player, info);
            OilPotItem.shrinkOilCount(stack);
            ModTrigger.EVENT.trigger(player, ModEventTriggerType.PUT_OIL_IN_POT);
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
        updateContraptionData(contraptionEntity, localPos, newInfo);

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
        NonNullList<ItemStack> inputs = readInputs(nbt);

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
                    saveInputs(newNbt, inputs);
                    StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                            info.pos(), state, newNbt);
                    updateContraptionData(contraptionEntity, localPos, newInfo);
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
        NonNullList<ItemStack> inputs = readInputs(nbt);

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
                saveInputs(newNbt, inputs);
                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        info.pos(), state, newNbt);
                updateContraptionData(contraptionEntity, localPos, newInfo);
            }

            if (hasHeatSource(contraptionEntity, localPos)) {
                player.hurt(contraptionEntity.level().damageSources().inFire(), 1);
                ModTrigger.EVENT.trigger(player, ModEventTriggerType.HURT_WHEN_TAKEOUT_FROM_POT);
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
                if (!isEmpty(newNbt)) {
                    startCooking(contraptionEntity, localPos, state, newNbt, info);
                    ModTrigger.EVENT.trigger(user, ModEventTriggerType.STIR_FRY_IN_POT);
                }
            }

            // 炒菜阶段
            if (status == COOKING) {
                int stirFryCount = newNbt.getInt(STIR_FRY_COUNT);
                if (stirFryCount > 0) {
                    newNbt.putInt(STIR_FRY_COUNT, stirFryCount - 1);
                }
                updateContraptionData(contraptionEntity, localPos, new StructureTemplate.StructureBlockInfo(
                        info.pos(), state, newNbt));
                ModTrigger.EVENT.trigger(user, ModEventTriggerType.STIR_FRY_IN_POT);
            }
        }
    }

    /**
     * 开始炒菜
     */
    private void startCooking(AbstractContraptionEntity contraptionEntity, BlockPos localPos, BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        NonNullList<ItemStack> inputs = readInputs(nbt);
        SimpleContainer container = getContainer(inputs);

        // 匹配配方
        var recipeOptional = contraptionEntity.level().getRecipeManager().getRecipeFor(ModRecipes.POT_RECIPE, container, contraptionEntity.level());

        CompoundTag newNbt = nbt.copy();
        newNbt.putInt(STATUS, COOKING);

        recipeOptional.ifPresentOrElse(recipe -> {
            // 如果合成表符合
            newNbt.putString(CARRIER, recipe.carrier().toJson().toString());
            newNbt.put(RESULT, recipe.assemble(container, contraptionEntity.level().registryAccess()).serializeNBT());
            newNbt.putInt(CURRENT_TICK, recipe.time());
            newNbt.putInt(STIR_FRY_COUNT, recipe.stirFryCount());
        }, () -> {
            // 不符合，进入迷之炒菜阶段
            newNbt.putString(CARRIER, Ingredient.of(Items.BOWL).toJson().toString());
            newNbt.put(RESULT, new ItemStack(FoodBiteRegistry.getItem(SUSPICIOUS_STIR_FRY)).serializeNBT());
            newNbt.putInt(CURRENT_TICK, 10 * 20); // 迷之炒菜时间
            newNbt.putInt(STIR_FRY_COUNT, 0); // 迷之炒菜不计翻炒次数
        });

        StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                info.pos(), state, newNbt);
        updateContraptionData(contraptionEntity, localPos, newInfo);
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
        ItemStack finallyResult = status == FINISHED ? readResult(nbt) : new ItemStack(FoodBiteRegistry.getItem(DARK_CUISINE));

        // 迷之炒菜盖饭特判逻辑
        if (finallyResult.is(FoodBiteRegistry.getItem(SUSPICIOUS_STIR_FRY)) && stack.is(TagCommon.COOKED_RICE)) {
            if (!contraptionEntity.level().isClientSide) {
                stack.shrink(1);
                ItemUtils.getItemToLivingEntity(player, ModItems.SUSPICIOUS_STIR_FRY_RICE_BOWL.get().getDefaultInstance());
                reset(contraptionEntity, localPos, state, nbt, info);
            }
            return true;
        }

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
            if (hasHeatSource(contraptionEntity, localPos)) {
                player.hurt(contraptionEntity.level().damageSources().inFire(), 1);
                ModTrigger.EVENT.trigger(player, ModEventTriggerType.HURT_WHEN_TAKEOUT_FROM_POT);
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
            if (hasHeatSource(contraptionEntity, localPos)) {
                player.hurt(contraptionEntity.level().damageSources().inFire(), 1);
                ModTrigger.EVENT.trigger(player, ModEventTriggerType.HURT_WHEN_TAKEOUT_FROM_POT);
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
        newNbt.put(INPUTS, ContainerHelper.saveAllItems(new CompoundTag(), NonNullList.withSize(PotRecipe.RECIPES_SIZE, ItemStack.EMPTY)));
        newNbt.putString(CARRIER, Ingredient.EMPTY.toJson().toString());
        newNbt.put(RESULT, ItemStack.EMPTY.serializeNBT());
        newNbt.putInt(STATUS, PUT_INGREDIENT);
        newNbt.putInt(CURRENT_TICK, 0);
        newNbt.putInt(STIR_FRY_COUNT, 0);
        newNbt.putLong(SEED, System.currentTimeMillis());

        BlockState newState = state.setValue(HAS_OIL, false).setValue(SHOW_OIL, false);

        StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                info.pos(), newState, newNbt);
        updateContraptionData(contraptionEntity, localPos, newInfo);
    }

    /**
     * 检查是否有热源
     * 参考 PotBlockEntity.hasHeatSource 实现
     */
    public static boolean hasHeatSource(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        Contraption contraption = contraptionEntity.getContraption();
        BlockPos belowLocalPos = localPos.below();

        // 首先检查 Contraption 内部下方是否有方块
        StructureTemplate.StructureBlockInfo belowInfo = contraption.getBlocks().get(belowLocalPos);
        if (belowInfo != null) {
            BlockState belowState = belowInfo.state();
            // 检查是否有 LIT 属性
            if (belowState.hasProperty(BlockStateProperties.LIT)) {
                return belowState.getValue(BlockStateProperties.LIT);
            }
            // 检查是否在热源标签中
            return belowState.is(TagMod.HEAT_SOURCE_BLOCKS_WITHOUT_LIT);
        }

        // Contraption 内部没有下方方块，检查世界中 Contraption 实体下方的方块
        // 计算世界坐标
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
        BlockPos worldPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
        BlockPos worldBelowPos = worldPos.below();

        BlockState worldBelowState = contraptionEntity.level().getBlockState(worldBelowPos);
        if (worldBelowState.hasProperty(BlockStateProperties.LIT)) {
            return worldBelowState.getValue(BlockStateProperties.LIT);
        }
        return worldBelowState.is(TagMod.HEAT_SOURCE_BLOCKS_WITHOUT_LIT);
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
            // 从blocks中真正移除该位置（而不是替换为空气）
            contraptionEntity.getContraption().getBlocks().remove(localPos);

            // 从interactors中移除
            contraptionEntity.getContraption().getInteractors().remove(localPos);

            // 从actors中移除
            contraptionEntity.getContraption().getActors().removeIf(actor -> actor.getLeft().pos().equals(localPos));

            // 更新Contraption的bounds - 移除方块后需要重新计算
            AABB updatedBounds = ContraptionBoundsUtil.recalculateBounds(contraptionEntity.getContraption());

            // 掉落PotBlock物品给玩家
            ItemStack potItem = new ItemStack(ModBlocks.POT.get());
            if (!player.isCreative())
                player.getInventory().placeItemBackInInventory(potItem);

            // 通知客户端重新渲染Contraption（同步bounds）
            // 使用空气状态表示该位置已被移除
            BlockState airState = Blocks.AIR.defaultBlockState();
            StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                    localPos, airState, null);

            setContraptionBlockData(contraptionEntity, localPos, newInfo);
            ((ContraptionAccessor) contraptionEntity.getContraption()).getUpdateTags().put(localPos, newInfo.nbt());

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
            contraptionEntity.level().playSound(null, soundPos, aboveInfo.state().getSoundType().getBreakSound(),
                    SoundSource.BLOCKS, 1.0F, 0.8F);
        }

        return true;
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
     * 保存原料列表
     */
    private void saveInputs(CompoundTag nbt, NonNullList<ItemStack> inputs) {
        nbt.put(INPUTS, ContainerHelper.saveAllItems(new CompoundTag(), inputs));
    }

    /**
     * 读取载体
     */
    private Ingredient readCarrier(CompoundTag nbt) {
        if (nbt.contains(CARRIER, Tag.TAG_STRING)) {
            JsonElement element = JsonParser.parseString(nbt.getString(CARRIER));
            return Ingredient.fromJson(element);
        }
        return Ingredient.EMPTY;
    }

    /**
     * 读取结果
     */
    private ItemStack readResult(CompoundTag nbt) {
        if (nbt.contains(RESULT, Tag.TAG_COMPOUND)) {
            return ItemStack.of(nbt.getCompound(RESULT));
        }
        return ItemStack.EMPTY;
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
     * 发送ActionBar消息
     */
    private void sendActionBarMessage(Player player, String type, Object... args) {
        if (player instanceof ServerPlayer serverPlayer) {
            String key = "tip.kaleidoscope_cookery.pot." + type;
            MutableComponent message = Component.translatable(key, args);
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
        }
    }

    /**
     * 更新Contraption中的方块数据
     */
    private void updateContraptionData(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                       StructureTemplate.StructureBlockInfo newInfo) {
        setContraptionBlockData(contraptionEntity, localPos, newInfo);
        ((ContraptionAccessor) contraptionEntity.getContraption()).getUpdateTags().put(localPos, newInfo.nbt());

        // 查找并更新actor数据
        var actors = contraptionEntity.getContraption().getActors();
        for (int i = 0; i < actors.size(); i++) {
            MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor = actors.get(i);
            if (actor.getLeft().pos().equals(localPos)) {
                setContraptionActorData(contraptionEntity, i, newInfo, actor.getRight());
                break;
            }
        }

        // 发送自定义数据包同步NBT数据到客户端
        if (!contraptionEntity.level().isClientSide) {
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
    }
}
