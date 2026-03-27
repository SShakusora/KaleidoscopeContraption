package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.ShawarmaSpitBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.util.ItemUtils;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.mixin.accessor.ContraptionAccessor;
import com.sshakusora.kaleidoscope_contraption.network.KCContraptionChangedPacket;
import com.sshakusora.kaleidoscope_contraption.network.KCPacketHandler;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionBoundsUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ShawarmaSpitBlockMovingInteraction extends MovingInteractionBehaviour {

    // NBT键名（与ShawarmaSpitBlockEntity保持一致）
    private static final String COOKING_ITEM = "CookingItem";
    private static final String COOKED_ITEM = "CookedItem";
    private static final String COOK_TIME = "CookTime";

    // 最大可放入物品数量（与ShawarmaSpitBlockEntity保持一致）
    private static final int MAX_ITEMS = 8;

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return false;
        }

        BlockState state = info.state();
        if (!(state.getBlock() instanceof ShawarmaSpitBlock)) {
            return false;
        }

        ItemStack itemInHand = player.getItemInHand(activeHand);

        // 处理按下移除键取下ShawarmaSpitBlock
        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return removeShawarmaSpitBlock(player, contraptionEntity, localPos, activeHand);
        }

        CompoundTag nbt = info.nbt();
        if (nbt == null) {
            nbt = new CompoundTag();
        }

        // 尝试放入食材
        if (onPutCookingItem(player, contraptionEntity, localPos, state, nbt, itemInHand, info)) {
            return true;
        }

        // 尝试取出成品
        if (onTakeCookedItem(player, contraptionEntity, localPos, state, nbt, itemInHand, info)) {
            return true;
        }

        return false;
    }

    /**
     * 放入烹饪食材
     */
    private boolean onPutCookingItem(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                      BlockState state, CompoundTag nbt, ItemStack itemStack, StructureTemplate.StructureBlockInfo info) {
        // 检查是否已有物品在烹饪或已完成
        ItemStack cookingItem = readCookingItem(nbt);
        ItemStack cookedItem = readCookedItem(nbt);
        if (!cookingItem.isEmpty() || !cookedItem.isEmpty()) {
            return false;
        }

        // 检查物品是否为空
        if (itemStack.isEmpty()) {
            return false;
        }

        // 尝试匹配营火配方
        SimpleContainer container = new SimpleContainer(itemStack.copy());
        var recipeOptional = contraptionEntity.level().getRecipeManager()
                .getRecipeFor(RecipeType.CAMPFIRE_COOKING, container, contraptionEntity.level());

        return recipeOptional.map(recipe -> {
            if (contraptionEntity.level().isClientSide) {
                return true;
            }

            // 设置烹饪物品和结果 - 一次最多放入MAX_ITEMS个
            int countToPut = Math.min(itemStack.getCount(), MAX_ITEMS);
            ItemStack cookingItemStack = itemStack.split(countToPut);
            ItemStack resultItem = recipe.assemble(container, contraptionEntity.level().registryAccess());
            resultItem.setCount(cookingItemStack.getCount());

            // 更新NBT
            CompoundTag newNbt = nbt.copy();
            newNbt.put(COOKING_ITEM, cookingItemStack.save(new CompoundTag()));
            newNbt.put(COOKED_ITEM, resultItem.save(new CompoundTag()));
            newNbt.putInt(COOK_TIME, recipe.getCookingTime());

            StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                    info.pos(), state, newNbt);
            updateContraptionData(contraptionEntity, localPos, newInfo);

            // 播放放入音效
            Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
            contraptionEntity.level().playSound(null,
                    globalPos.x + 0.5,
                    globalPos.y + 0.5,
                    globalPos.z + 0.5,
                    SoundEvents.ITEM_FRAME_ADD_ITEM,
                    SoundSource.BLOCKS,
                    0.5F + contraptionEntity.level().random.nextFloat(),
                    contraptionEntity.level().random.nextFloat() * 0.7F + 0.6F);

            return true;
        }).orElse(false);
    }

    /**
     * 取出烹饪完成的物品
     */
    private boolean onTakeCookedItem(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                      BlockState state, CompoundTag nbt, ItemStack mainHandItem, StructureTemplate.StructureBlockInfo info) {
        int cookTime = nbt.getInt(COOK_TIME);
        ItemStack cookingItem = readCookingItem(nbt);
        ItemStack cookedItem = readCookedItem(nbt);

        // 如果有烹饪完成的物品（cookTime <= 0 且 cookedItem 不为空）
        if (cookTime <= 0 && !cookedItem.isEmpty()) {
            if (contraptionEntity.level().isClientSide) {
                return true;
            }

            giveItem(player, contraptionEntity, localPos, state, nbt, cookedItem.copy(), info);
            return true;
        }

        // 如果没有烹饪完成，但有正在烹饪的物品，返还原材料
        if (cookTime > 0 && !cookingItem.isEmpty()) {
            if (contraptionEntity.level().isClientSide) {
                return true;
            }

            giveItem(player, contraptionEntity, localPos, state, nbt, cookingItem.copy(), info);
            return true;
        }

        return false;
    }

    /**
     * 给予玩家物品并重置状态
     */
    private void giveItem(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                          BlockState state, CompoundTag nbt, ItemStack itemStack, StructureTemplate.StructureBlockInfo info) {
        // 如果通电，玩家会受到伤害
        if (state.getValue(BlockStateProperties.POWERED)) {
            player.hurt(contraptionEntity.level().damageSources().inFire(), 1);
        }

        // 给予玩家物品
        ItemUtils.getItemToLivingEntity(player, itemStack);

        // 重置NBT
        CompoundTag newNbt = nbt.copy();
        newNbt.put(COOKING_ITEM, ItemStack.EMPTY.save(new CompoundTag()));
        newNbt.put(COOKED_ITEM, ItemStack.EMPTY.save(new CompoundTag()));
        newNbt.putInt(COOK_TIME, 0);

        StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                info.pos(), state, newNbt);
        updateContraptionData(contraptionEntity, localPos, newInfo);

        // 播放取出音效
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
        contraptionEntity.level().playSound(null,
                globalPos.x + 0.5,
                globalPos.y + 0.5,
                globalPos.z + 0.5,
                SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                SoundSource.BLOCKS,
                0.5F + contraptionEntity.level().random.nextFloat(),
                contraptionEntity.level().random.nextFloat() * 0.7F + 0.6F);
    }

    /**
     * 移除ShawarmaSpitBlock（空手+Shift+右键）
     */
    private boolean removeShawarmaSpitBlock(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos, InteractionHand activeHand) {
        StructureTemplate.StructureBlockInfo blockInfo = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (blockInfo == null || !(blockInfo.state().getBlock() instanceof ShawarmaSpitBlock)) {
            return false;
        }

        // 检查是否正在烹饪或有成品
        CompoundTag nbt = blockInfo.nbt();
        if (nbt != null) {
            ItemStack cookingItem = readCookingItem(nbt);
            ItemStack cookedItem = readCookedItem(nbt);
            if (!cookingItem.isEmpty() || !cookedItem.isEmpty()) {
                // 有物品时不能移除
                return false;
            }
        }

        // 在服务端执行移除逻辑
        if (!contraptionEntity.level().isClientSide) {
            // 检查是否是双层方块，如果是上层需要找到下层
            BlockPos lowerPos;
            BlockState state = blockInfo.state();
            if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
                lowerPos = localPos.below();
            } else {
                lowerPos = localPos;
            }
            BlockPos upperPos = lowerPos.above();

            // 先掉落物品（如果有）
            dropCookItems(contraptionEntity, lowerPos);

            // 从blocks中移除下层和上层
            contraptionEntity.getContraption().getBlocks().remove(lowerPos);
            contraptionEntity.getContraption().getBlocks().remove(upperPos);

            // 从interactors中移除
            contraptionEntity.getContraption().getInteractors().remove(lowerPos);
            contraptionEntity.getContraption().getInteractors().remove(upperPos);

            // 从actors中移除
            contraptionEntity.getContraption().getActors().removeIf(actor ->
                    actor.getLeft().pos().equals(lowerPos) || actor.getLeft().pos().equals(upperPos));

            // 更新Contraption的bounds
            AABB updatedBounds = ContraptionBoundsUtil.recalculateBounds(contraptionEntity.getContraption());

            // 掉落ShawarmaSpitBlock物品给玩家
            ItemStack spitItem = new ItemStack(ModBlocks.SHAWARMA_SPIT.get());
            if (!player.isCreative()) {
                player.getInventory().placeItemBackInInventory(spitItem);
            }

            // 通知客户端重新渲染Contraption
            BlockState airState = Blocks.AIR.defaultBlockState();
            StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                    lowerPos, airState, null);

            setContraptionBlockData(contraptionEntity, lowerPos, newInfo);
            ((ContraptionAccessor) contraptionEntity.getContraption()).getUpdateTags().put(lowerPos, newInfo.nbt());

            // 同样处理上层
            StructureTemplate.StructureBlockInfo upperInfo = new StructureTemplate.StructureBlockInfo(
                    upperPos, airState, null);
            setContraptionBlockData(contraptionEntity, upperPos, upperInfo);
            ((ContraptionAccessor) contraptionEntity.getContraption()).getUpdateTags().put(upperPos, upperInfo.nbt());

            KCPacketHandler.sendToTracking(
                    new KCContraptionChangedPacket(
                            contraptionEntity.getId(),
                            lowerPos,
                            airState,
                            null,
                            updatedBounds
                    ),
                    contraptionEntity
            );

            // 播放破坏音效
            Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
            BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
            contraptionEntity.level().playSound(null, soundPos, blockInfo.state().getSoundType().getBreakSound(),
                    SoundSource.BLOCKS, 1.0F, 0.8F);
        }

        return true;
    }

    /**
     * 掉落烹饪物品
     */
    private void dropCookItems(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || info.nbt() == null) {
            return;
        }

        CompoundTag nbt = info.nbt();
        ItemStack cookingItem = readCookingItem(nbt);
        ItemStack cookedItem = readCookedItem(nbt);

        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
        BlockPos dropPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);

        if (!cookingItem.isEmpty()) {
            Block.popResource(contraptionEntity.level(), dropPos, cookingItem.copy());
        } else if (!cookedItem.isEmpty()) {
            Block.popResource(contraptionEntity.level(), dropPos, cookedItem.copy());
        }
    }

    /**
     * 读取正在烹饪的物品
     */
    private ItemStack readCookingItem(CompoundTag nbt) {
        if (nbt.contains(COOKING_ITEM)) {
            return ItemStack.of(nbt.getCompound(COOKING_ITEM));
        }
        return ItemStack.EMPTY;
    }

    /**
     * 读取烹饪完成的物品
     */
    private ItemStack readCookedItem(CompoundTag nbt) {
        if (nbt.contains(COOKED_ITEM)) {
            return ItemStack.of(nbt.getCompound(COOKED_ITEM));
        }
        return ItemStack.EMPTY;
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
            var actor = actors.get(i);
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
