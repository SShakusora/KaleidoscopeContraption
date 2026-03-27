package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.advancements.critereon.ModEventTriggerType;
import com.github.ysbbbbbb.kaleidoscopecookery.api.recipe.soupbase.ISoupBase;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.StockpotBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.StockpotRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.serializer.StockpotRecipeSerializer;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.soupbase.FluidSoupBase;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.soupbase.SoupBaseManager;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModSoupBases;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModTrigger;
import com.github.ysbbbbbb.kaleidoscopecookery.init.tag.TagMod;
import com.github.ysbbbbbb.kaleidoscopecookery.util.ItemUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;


public class StockpotBlockInteraction extends MovingInteractionBehaviour {

    private static final String INPUTS = "Inputs";
    private static final String RECIPE_ID = "RecipeId";
    private static final String SOUP_BASE_ID = "SoupBaseId";
    private static final String RESULT = "Result";
    private static final String STATUS = "Status";
    private static final String CURRENT_TICK = "CurrentTick";
    private static final String TAKEOUT_COUNT = "TakeoutCount";
    private static final String LID_ITEM = "LidItem";
    private static final String CARRIER = "Carrier";

    // IStockpot 状态常量
    private static final int PUT_SOUP_BASE = 0;
    private static final int PUT_INGREDIENT = 1;
    private static final int COOKING = 2;
    private static final int FINISHED = 3;

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return false;
        }

        BlockState state = info.state();
        if (!(state.getBlock() instanceof StockpotBlock)) {
            return false;
        }

        ItemStack itemInHand = player.getItemInHand(activeHand);

        // 处理按下移除键取下StockpotBlock
        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return removeStockpotBlock(player, contraptionEntity, localPos, activeHand);
        }

        CompoundTag nbt = info.nbt();
        if (nbt == null) {
            nbt = new CompoundTag();
        }

        // 读取当前状态
        int status = nbt.getInt(STATUS);
        boolean hasLid = state.getValue(StockpotBlock.HAS_LID);

        // 先处理盖子相关逻辑（放上或取下盖子）
        if (onLidClick(player, contraptionEntity, localPos, state, nbt, itemInHand, info)) {
            return true;
        }

        // 如果有盖子，大部分操作无法进行
        if (hasLid) {
            return true;
        }

        // 先检查执行原料取出逻辑
        if ((itemInHand.isEmpty() || itemInHand.is(TagMod.INGREDIENT_CONTAINER)) && removeIngredient(player, contraptionEntity, localPos, state, nbt, info)) {
            return true;
        }

        // 再检查成品取出逻辑
        if (takeOutProduct(player, contraptionEntity, localPos, state, nbt, itemInHand, info)) {
            return true;
        }

        // 检查热源
//        if (!hasHeatSource(contraptionEntity, localPos)) {
//            sendActionBarMessage(player, "need_lit_stove");
//            return true;
//        }

        // 加入汤底
        if (status == PUT_SOUP_BASE) {
            if (addSoupBase(player, contraptionEntity, localPos, state, nbt, itemInHand, info)) {
                return true;
            }
        }

        // 取出汤底
        if (status == PUT_INGREDIENT && isEmpty(nbt)) {
            if (removeSoupBase(player, contraptionEntity, localPos, state, nbt, itemInHand, info)) {
                return true;
            }
        }

        // 放入原料（仅在PUT_INGREDIENT状态）
        if (status == PUT_INGREDIENT) {
            if (addIngredient(player, contraptionEntity, localPos, state, nbt, itemInHand, info)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 处理盖子点击逻辑（放上或取下盖子）
     */
    private boolean onLidClick(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                BlockState state, CompoundTag nbt, ItemStack stack, StructureTemplate.StructureBlockInfo info) {
        boolean hasLid = state.getValue(StockpotBlock.HAS_LID);

        // 第一种情况：放上盖子
        if (!hasLid && stack.is(ModItems.STOCKPOT_LID.get())) {
            if (!contraptionEntity.level().isClientSide) {
                // 更新NBT - 保存盖子物品
                CompoundTag newNbt = nbt.copy();
                newNbt.put(LID_ITEM, stack.split(1).save(new CompoundTag()));

                // 更新BlockState
                BlockState newState = state.setValue(StockpotBlock.HAS_LID, true);

                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        info.pos(), newState, newNbt);
                updateContraptionData(contraptionEntity, localPos, newInfo);

                // 播放音效
                Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
                BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
                contraptionEntity.level().playSound(player, soundPos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 0.5F, 0.5F);

                ModTrigger.EVENT.trigger(player, ModEventTriggerType.USE_LID_ON_STOCKPOT);
            }
            return true;
        }

        // 第二种情况：取下盖子
        if (hasLid) {
            if (!contraptionEntity.level().isClientSide) {
                // 获取盖子物品
                ItemStack lidItem;
                if (nbt.contains(LID_ITEM, Tag.TAG_COMPOUND)) {
                    lidItem = ItemStack.of(nbt.getCompound(LID_ITEM));
                } else {
                    lidItem = ModItems.STOCKPOT_LID.get().getDefaultInstance();
                }

                // 更新NBT - 移除盖子物品
                CompoundTag newNbt = nbt.copy();
                newNbt.remove(LID_ITEM);

                // 更新BlockState
                BlockState newState = state.setValue(StockpotBlock.HAS_LID, false);

                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        info.pos(), newState, newNbt);
                updateContraptionData(contraptionEntity, localPos, newInfo);

                // 给予玩家盖子
                if (stack.isEmpty()) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, lidItem);
                } else {
                    ItemUtils.getItemToLivingEntity(player, lidItem);
                }

                // 播放音效
                Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
                BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
                contraptionEntity.level().playSound(player, soundPos, SoundEvents.LANTERN_BREAK, SoundSource.BLOCKS, 0.5F, 0.5F);
            }
            return true;
        }

        return false;
    }

    /**
     * 添加汤底到汤锅中
     */
    private boolean addSoupBase(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                 BlockState state, CompoundTag nbt, ItemStack bucket, StructureTemplate.StructureBlockInfo info) {
        for (var entry : SoupBaseManager.getAllSoupBases().entrySet()) {
            ResourceLocation key = entry.getKey();
            ISoupBase soupBase = entry.getValue();
            if (soupBase.isSoupBase(bucket)) {
                if (!contraptionEntity.level().isClientSide) {
                    // 更新NBT
                    CompoundTag newNbt = nbt.copy();
                    newNbt.putString(SOUP_BASE_ID, key.toString());
                    newNbt.putInt(STATUS, PUT_INGREDIENT);
                    // 初始化inputs
                    if (!newNbt.contains(INPUTS, Tag.TAG_COMPOUND)) {
                        newNbt.put(INPUTS, ContainerHelper.saveAllItems(new CompoundTag(),
                                NonNullList.withSize(StockpotRecipe.RECIPES_SIZE, ItemStack.EMPTY)));
                    }

                    StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                            info.pos(), state, newNbt);
                    updateContraptionData(contraptionEntity, localPos, newInfo);

                    // 返还容器
                    ItemStack container = soupBase.getReturnContainer(contraptionEntity.level(), player, bucket);
                    if (!player.isCreative()) {
                        bucket.shrink(1);
                    }
                    ItemUtils.getItemToLivingEntity(player, container);

                    ModTrigger.EVENT.trigger(player, ModEventTriggerType.PUT_SOUP_BASE_IN_STOCKPOT);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 移除汤锅中的汤底
     */
    private boolean removeSoupBase(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                    BlockState state, CompoundTag nbt, ItemStack bucket, StructureTemplate.StructureBlockInfo info) {
        ResourceLocation soupBaseId = ResourceLocation.tryParse(nbt.getString(SOUP_BASE_ID));
        if (soupBaseId == null) {
            soupBaseId = ModSoupBases.WATER;
        }

        ISoupBase soupBase = SoupBaseManager.getSoupBase(soupBaseId);
        if (soupBase == null || !soupBase.isContainer(bucket)) {
            return false;
        }

        if (!contraptionEntity.level().isClientSide) {
            // 更新NBT
            CompoundTag newNbt = nbt.copy();
            newNbt.putString(SOUP_BASE_ID, ModSoupBases.WATER.toString());
            newNbt.putInt(STATUS, PUT_SOUP_BASE);

            StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                    info.pos(), state, newNbt);
            updateContraptionData(contraptionEntity, localPos, newInfo);

            // 返还汤底
            ItemStack container = soupBase.getReturnSoupBase(contraptionEntity.level(), player, bucket);
            if (!player.isCreative()) {
                bucket.shrink(1);
            }
            ItemUtils.getItemToLivingEntity(player, container);
        }
        return true;
    }

    /**
     * 添加原料到汤锅中
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
                contraptionEntity.level().playSound(null, soundPos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                        ((contraptionEntity.level().random.nextFloat() - contraptionEntity.level().random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
                return true;
            }
        }
        return false;
    }

    /**
     * 移除汤锅中的原料
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
                ItemUtils.getItemToLivingEntity(player, stack.copy());

                // 更新NBT
                CompoundTag newNbt = nbt.copy();
                saveInputs(newNbt, inputs);
                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        info.pos(), state, newNbt);
                updateContraptionData(contraptionEntity, localPos, newInfo);

                // 如果是流体汤底，且温度过高，玩家会受到伤害
                ResourceLocation soupBaseId = ResourceLocation.tryParse(nbt.getString(SOUP_BASE_ID));
                if (soupBaseId != null) {
                    ISoupBase soupBase = SoupBaseManager.getSoupBase(soupBaseId);
                    if (soupBase instanceof FluidSoupBase fluidSoupBase && fluidSoupBase.getFluid().getFluidType().getTemperature() > 500) {
                        player.hurt(contraptionEntity.level().damageSources().inFire(), 1);
                        ModTrigger.EVENT.trigger(player, ModEventTriggerType.HURT_WHEN_TAKEOUT_FROM_STOCKPOT);
                    }
                }
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
        sendActionBarMessage(player, "need_container", containerItem.getDefaultInstance().getHoverName());
        return false;
    }

    /**
     * 从汤锅中取出产品
     */
    private boolean takeOutProduct(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                    BlockState state, CompoundTag nbt, ItemStack stack, StructureTemplate.StructureBlockInfo info) {
        int status = nbt.getInt(STATUS);

        // 仅在烹饪完成时可以取出
        if (status != FINISHED) {
            return false;
        }

        ItemStack result = readResult(nbt);
        if (result.isEmpty()) {
            return false;
        }

        int takeoutCount = nbt.getInt(TAKEOUT_COUNT);
        if (takeoutCount <= 0) {
            return false;
        }

        Ingredient carrier = readCarrier(nbt);

        // 检查容器是否正确
        if (!carrier.isEmpty() && !carrier.test(stack)) {
            Component carrierName = carrier.getItems()[0].getHoverName();
            sendActionBarMessage(player, "need_carrier", carrierName);
            return false;
        }

        if (!contraptionEntity.level().isClientSide) {
            // 消耗容器
            if (!carrier.isEmpty()) {
                stack.shrink(1);
            }

            // 给予玩家结果
            ItemStack resultCopy = result.copyWithCount(1);
            ItemUtils.getItemToLivingEntity(player, resultCopy);

            // 更新取出次数
            takeoutCount--;

            CompoundTag newNbt = nbt.copy();
            newNbt.putInt(TAKEOUT_COUNT, takeoutCount);

            // 如果取完了，重置状态
            if (takeoutCount <= 0) {
                newNbt.putInt(STATUS, PUT_SOUP_BASE);
                newNbt.putString(SOUP_BASE_ID, ModSoupBases.WATER.toString());
                newNbt.putString(RECIPE_ID, StockpotRecipeSerializer.EMPTY_ID.toString());
                newNbt.put(RESULT, ItemStack.EMPTY.serializeNBT());
                newNbt.putInt(CURRENT_TICK, -1);
                saveInputs(newNbt, NonNullList.withSize(StockpotRecipe.RECIPES_SIZE, ItemStack.EMPTY));
            }

            StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                    info.pos(), state, newNbt);
            updateContraptionData(contraptionEntity, localPos, newInfo);
        }
        return true;
    }

    /**
     * 移除StockpotBlock
     */
    private boolean removeStockpotBlock(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos, InteractionHand activeHand) {
        StructureTemplate.StructureBlockInfo aboveInfo = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (aboveInfo == null || !(aboveInfo.state().getBlock() instanceof StockpotBlock)) {
            return false;
        }

        CompoundTag nbt = aboveInfo.nbt();
        if (nbt == null) {
            nbt = new CompoundTag();
        }

        // 检查是否可以移除（必须没有盖子，且状态为PUT_SOUP_BASE或PUT_INGREDIENT且为空）
        boolean hasLid = aboveInfo.state().getValue(StockpotBlock.HAS_LID);
        int status = nbt.getInt(STATUS);

        if (hasLid) {
            return false;
        }

        if (status != PUT_SOUP_BASE && status != PUT_INGREDIENT) {
            return false;
        }

        if (status == PUT_INGREDIENT && !isEmpty(nbt)) {
            return false;
        }

        // 在服务端执行移除逻辑
        if (!contraptionEntity.level().isClientSide) {
            // 从Contraption中移除方块
            ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);

            // 更新Contraption的bounds
            var updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);

            // 掉落StockpotBlock物品给玩家
            ItemStack stockpotItem = new ItemStack(ModBlocks.STOCKPOT.get());
            if (!player.isCreative()) {
                player.getInventory().placeItemBackInInventory(stockpotItem);
            }

            // 通知客户端重新渲染Contraption
            ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, updatedBounds);

            // 播放破坏音效
            ContraptionInteractionUtil.playBreakSound(contraptionEntity, localPos, aboveInfo.state());
        }

        return true;
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
        // 默认返回碗
        return Ingredient.of(Items.BOWL);
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
     * 发送ActionBar消息
     */
    private void sendActionBarMessage(Player player, String type, Object... args) {
        if (player instanceof ServerPlayer serverPlayer) {
            String key = "tip.kaleidoscope_cookery.stockpot." + type;
            MutableComponent message = Component.translatable(key, args);
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
        }
    }

    /**
     * 更新Contraption中的方块数据
     */
    private void updateContraptionData(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                       StructureTemplate.StructureBlockInfo newInfo) {
        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);
    }
}
