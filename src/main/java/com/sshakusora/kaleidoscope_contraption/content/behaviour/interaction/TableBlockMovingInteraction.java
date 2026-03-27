package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.TableBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.ChoppingBoardBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.util.CarpetColor;
import com.github.ysbbbbbb.kaleidoscopecookery.util.ItemUtils;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.mixin.accessor.ContraptionAccessor;
import com.sshakusora.kaleidoscope_contraption.network.KCContraptionChangedPacket;
import com.sshakusora.kaleidoscope_contraption.network.KCPacketHandler;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class TableBlockMovingInteraction extends MovingInteractionBehaviour {

    private static final String COLOR_TAG = "CarpetColor";
    private static final String SHOW_ITEMS = "ShowItems";

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) return false;

        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return true;
        }

        BlockState state = info.state();
        if (!(state.getBlock() instanceof TableBlock)) {
            return true;
        }

        ItemStack itemInHand = player.getItemInHand(activeHand);

        // 处理地毯交互
        if (itemInHand.is(ItemTags.WOOL_CARPETS)) {
            return handleCarpetInteraction(player, activeHand, localPos, contraptionEntity, state, info, itemInHand);
        }

        //处理食物方块的交互
        if (handleFoodBlockPlacement(player, activeHand, localPos, contraptionEntity, info, itemInHand)) {
            return true;
        }

        //处理切菜板的放置交互
        if (handleChoppingBoardPlacement(player, activeHand, localPos, contraptionEntity, info, itemInHand)) {
            return true;
        }

        // 处理物品放置/取出交互
        return handleItemInteraction(player, activeHand, localPos, contraptionEntity, state, info, itemInHand);
    }

    /**
     * 处理物品放置和取出逻辑
     */
    private boolean handleItemInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                          AbstractContraptionEntity contraptionEntity, BlockState state,
                                          StructureTemplate.StructureBlockInfo info, ItemStack itemInHand) {
        CompoundTag nbt = info.nbt();
        if (nbt == null) {
            nbt = new CompoundTag();
        }

        // 读取当前物品数据
        ItemStackHandler tableItems = new ItemStackHandler(4);
        if (nbt.contains(SHOW_ITEMS)) {
            tableItems.deserializeNBT(nbt.getCompound(SHOW_ITEMS));
        }

        Pair<Integer, ItemStack> lastStack = ItemUtils.getLastStack(tableItems);
        int tableIndex = lastStack.getLeft();
        ItemStack tableItem = lastStack.getRight();

        boolean handEmpty = itemInHand.isEmpty();

        // 玩家手为空，桌子有物品：取出桌子物品
        if (handEmpty && !tableItem.isEmpty()) {
            if (!contraptionEntity.level().isClientSide) {
                // 掉落物品给玩家
                dropItemToPlayer(contraptionEntity, localPos, player, tableItem.copy());
                tableItems.setStackInSlot(tableIndex, ItemStack.EMPTY);

                // 更新NBT
                CompoundTag newNbt = nbt.copy();
                newNbt.put(SHOW_ITEMS, tableItems.serializeNBT());
                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        info.pos(), state, newNbt);
                updateContraptionData(contraptionEntity, localPos, newInfo);
            }
            // 播放音效
            Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
            BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
            contraptionEntity.level().playSound(null, soundPos, SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                    player.getSoundSource(), 1.0F, 1.0F);
            return true;
        }

        // 玩家手有物品，并且可以放入物品时（桌子还有空位）
        if (!handEmpty && tableIndex < (tableItems.getSlots() - 1)) {
            if (!contraptionEntity.level().isClientSide) {
                ItemStack split = itemInHand.split(1);
                if (tableItem.isEmpty()) {
                    tableItems.setStackInSlot(tableIndex, split);
                } else {
                    tableItems.setStackInSlot(tableIndex + 1, split);
                }

                // 更新NBT
                CompoundTag newNbt = nbt.copy();
                newNbt.put(SHOW_ITEMS, tableItems.serializeNBT());
                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        info.pos(), state, newNbt);
                updateContraptionData(contraptionEntity, localPos, newInfo);
            }
            // 播放音效
            Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
            BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
            contraptionEntity.level().playSound(null, soundPos, SoundEvents.ITEM_FRAME_ADD_ITEM,
                    player.getSoundSource(), 1.0F, 1.0F);
            return true;
        }

        return true;
    }

    /**
     * 处理地毯放置和更换逻辑
     */
    private boolean handleCarpetInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                            AbstractContraptionEntity contraptionEntity, BlockState state,
                                            StructureTemplate.StructureBlockInfo info, ItemStack itemInHand) {
        DyeColor dyeColor = CarpetColor.getColorByCarpet(itemInHand.getItem());
        if (dyeColor == null) {
            return true;
        }

        boolean hasCarpet = state.getValue(TableBlock.HAS_CARPET);
        CompoundTag nbt = info.nbt();
        if (nbt == null) {
            nbt = new CompoundTag();
        }

        DyeColor currentColor = DyeColor.WHITE;
        if (nbt.contains(COLOR_TAG)) {
            currentColor = DyeColor.byId(nbt.getInt(COLOR_TAG));
        }

        // 第一种情况：桌子上没有地毯，放置地毯
        if (!hasCarpet) {
            if (!contraptionEntity.level().isClientSide) {
                // 更新BlockState
                BlockState newState = state.setValue(TableBlock.HAS_CARPET, true);

                // 更新NBT
                CompoundTag newNbt = nbt.copy();
                newNbt.putInt(COLOR_TAG, dyeColor.getId());

                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        info.pos(), newState, newNbt);
                updateContraptionData(contraptionEntity, localPos, newInfo);

                // 消耗物品
                if (!player.isCreative()) {
                    itemInHand.shrink(1);
                }
            }
            // 播放音效
            Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
            BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
            contraptionEntity.level().playSound(null, soundPos, SoundType.WOOL.getPlaceSound(),
                    player.getSoundSource(), 1.0F, 1.0F);
            return true;
        }

        // 第二种情况：有地毯，但是颜色不一致，更换地毯
        if (hasCarpet && currentColor != dyeColor) {
            if (!contraptionEntity.level().isClientSide) {
                // 掉落原地毯
                ItemStack carpetItem = new ItemStack(CarpetColor.getCarpetByColor(currentColor));
                dropItemToPlayer(contraptionEntity, localPos, player, carpetItem);

                // 更新NBT中的颜色
                CompoundTag newNbt = nbt.copy();
                newNbt.putInt(COLOR_TAG, dyeColor.getId());

                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        info.pos(), state, newNbt);
                updateContraptionData(contraptionEntity, localPos, newInfo);

                // 消耗物品
                if (!player.isCreative()) {
                    itemInHand.shrink(1);
                }
            }
            // 播放音效
            Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
            BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
            contraptionEntity.level().playSound(null, soundPos, SoundType.WOOL.getPlaceSound(),
                    player.getSoundSource(), 1.0F, 1.0F);
            return true;
        }

        return true;
    }

    /**
     * 处理手持FoodBiteBlock放置到桌子上方
     */
    private boolean handleFoodBlockPlacement(Player player, InteractionHand activeHand, BlockPos localPos,
                                             AbstractContraptionEntity contraptionEntity, StructureTemplate.StructureBlockInfo info,
                                             ItemStack itemInHand) {
        CompoundTag nbt = info.nbt();
        if (nbt == null) {
            nbt = new CompoundTag();
        }

        // 读取当前物品数据
        ItemStackHandler tableItems = new ItemStackHandler(4);
        if (nbt.contains(SHOW_ITEMS)) {
            tableItems.deserializeNBT(nbt.getCompound(SHOW_ITEMS));
        }

        Pair<Integer, ItemStack> lastStack = ItemUtils.getLastStack(tableItems);
        int tableIndex = lastStack.getLeft();

        if (!player.isShiftKeyDown() && tableIndex < (tableItems.getSlots() - 1)) return false;

        BlockPos abovePos = localPos.above();

        // 检查正上方是否已经有方块
        StructureTemplate.StructureBlockInfo aboveInfo = contraptionEntity.getContraption().getBlocks().get(abovePos);
        if (aboveInfo != null && !aboveInfo.state().isAir()) {
            // 上方有方块，无法放置
            return false;
        }

        // 获取手持的FoodBiteBlock
        Block heldBlock = Block.byItem(itemInHand.getItem());
        if (!(heldBlock instanceof FoodBiteBlock foodBlock)) {
            return false;
        }

        // 在服务端执行放置逻辑
        if (!contraptionEntity.level().isClientSide) {
            // 创建新的FoodBiteBlock状态（重置咬食次数为0，朝向玩家）
            BlockState newState = foodBlock.defaultBlockState()
                    .setValue(foodBlock.getBites(), 0)
                    .setValue(FoodBiteBlock.FACING, player.getDirection().getOpposite());

            StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                    abovePos, newState, null);

            // 使用setContraptionBlockData来更新方块数据
            setContraptionBlockData(contraptionEntity, abovePos, newInfo);

            // 注册交互行为到interactors地图，使新放置的方块可以被交互
            MovingInteractionBehaviour interactionBehaviour = MovingInteractionBehaviour.REGISTRY.get(newState);
            if (interactionBehaviour != null) {
                contraptionEntity.getContraption().getInteractors().put(abovePos, interactionBehaviour);
            }

            // 更新Contraption的bounds - 参考Contraption.addBlock()的实现
            AABB updatedBounds = contraptionEntity.getContraption().bounds.minmax(new AABB(abovePos));
            contraptionEntity.getContraption().bounds = updatedBounds;

            // 通知客户端重新渲染Contraption（同步bounds）
            KCPacketHandler.sendToTracking(
                    new KCContraptionChangedPacket(
                            contraptionEntity.getId(),
                            abovePos,
                            newInfo.state(),
                            newInfo.nbt(),
                            updatedBounds
                    ),
                    contraptionEntity
            );

            // 消耗玩家手持的一个物品
            if (!player.isCreative()) {
                itemInHand.shrink(1);
            }

            // 播放放置音效
            Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(abovePos), 1.0f);
            BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
            contraptionEntity.level().playSound(null, soundPos, newState.getSoundType().getPlaceSound(),
                    SoundSource.BLOCKS, 1.0F, 0.8F);
        }

        return true;
    }

    /**
     * 处理手持ChoppingBoardBlock放置到桌子上方
     */
    private boolean handleChoppingBoardPlacement(Player player, InteractionHand activeHand, BlockPos localPos,
                                                  AbstractContraptionEntity contraptionEntity, StructureTemplate.StructureBlockInfo info,
                                                  ItemStack itemInHand) {
        // 需要Shift+右键才能放置切菜板
        if (!player.isShiftKeyDown()) {
            return false;
        }

        BlockPos abovePos = localPos.above();

        // 检查正上方是否已经有方块
        StructureTemplate.StructureBlockInfo aboveInfo = contraptionEntity.getContraption().getBlocks().get(abovePos);
        if (aboveInfo != null && !aboveInfo.state().isAir()) {
            // 上方有方块，无法放置
            return false;
        }

        // 获取手持的ChoppingBoardBlock
        Block heldBlock = Block.byItem(itemInHand.getItem());
        if (!(heldBlock instanceof ChoppingBoardBlock choppingBoardBlock)) {
            return false;
        }

        // 在服务端执行放置逻辑
        if (!contraptionEntity.level().isClientSide) {
            // 创建新的ChoppingBoardBlock状态，朝向玩家
            BlockState newState = choppingBoardBlock.defaultBlockState()
                    .setValue(ChoppingBoardBlock.FACING, player.getDirection().getOpposite());

            CompoundTag nbt = new CompoundTag();
            nbt.putInt("MaxCutCount", 0);
            nbt.putInt("CurrentCutCount", 0);
            nbt.put("CurrentCutStack", ItemStack.EMPTY.serializeNBT());
            nbt.put("ResultItem", ItemStack.EMPTY.serializeNBT());
            // 写入id，防止重进存档时渲染的物品消失
            nbt.putString("id", "kaleidoscope_cookery:chopping_board");

            StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                    abovePos, newState, nbt);

            // 使用setContraptionBlockData来更新方块数据
            setContraptionBlockData(contraptionEntity, abovePos, newInfo);

            // 注册交互行为到interactors地图，使新放置的方块可以被交互
            MovingInteractionBehaviour interactionBehaviour = MovingInteractionBehaviour.REGISTRY.get(newState);
            if (interactionBehaviour != null) {
                contraptionEntity.getContraption().getInteractors().put(abovePos, interactionBehaviour);
            }

            // 更新Contraption的bounds
            AABB updatedBounds = contraptionEntity.getContraption().bounds.minmax(new AABB(abovePos));
            contraptionEntity.getContraption().bounds = updatedBounds;

            // 通知客户端重新渲染Contraption（同步bounds）
            KCPacketHandler.sendToTracking(
                    new KCContraptionChangedPacket(
                            contraptionEntity.getId(),
                            abovePos,
                            newInfo.state(),
                            newInfo.nbt(),
                            updatedBounds
                    ),
                    contraptionEntity
            );

            // 消耗玩家手持的一个物品
            if (!player.isCreative()) {
                itemInHand.shrink(1);
            }

            // 播放放置音效
            Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(abovePos), 1.0f);
            BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
            contraptionEntity.level().playSound(null, soundPos, newState.getSoundType().getPlaceSound(),
                    SoundSource.BLOCKS, 1.0F, 0.8F);
        }

        return true;
    }

    /**
     * 在指定位置生成掉落物（模拟popResource）
     */
    private void dropItemToPlayer(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                  Player player, ItemStack stack) {
        if (contraptionEntity.level().isClientSide || stack.isEmpty()) {
            return;
        }

        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
        ItemEntity itemEntity = new ItemEntity(
                contraptionEntity.level(),
                globalPos.x,
                globalPos.y + 0.75,
                globalPos.z,
                stack
        );
        itemEntity.setDefaultPickUpDelay();
        contraptionEntity.level().addFreshEntity(itemEntity);
    }

    /**
     * 更新Contraption中的方块数据
     */
    private void updateContraptionData(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                       StructureTemplate.StructureBlockInfo newInfo) {
        setContraptionBlockData(contraptionEntity, localPos, newInfo);
        // 标记为更新，避免重进存档后NBT消失
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
