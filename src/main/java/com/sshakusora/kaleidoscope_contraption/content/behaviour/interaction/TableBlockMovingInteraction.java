package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.TableBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteOneByTwoBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteThreeByThreeBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.ChoppingBoardBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.NinePart;
import com.github.ysbbbbbb.kaleidoscopecookery.util.CarpetColor;
import com.github.ysbbbbbb.kaleidoscopecookery.util.ItemUtils;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.network.KCContraptionChangedPacket;
import com.sshakusora.kaleidoscope_contraption.network.KCPacketHandler;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
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
import org.apache.commons.lang3.tuple.Pair;

public class TableBlockMovingInteraction extends SyncedMovingInteractionBehaviour {

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
                ContraptionInteractionUtil.dropItemToPlayer(contraptionEntity, localPos, player, tableItem.copy());
                tableItems.setStackInSlot(tableIndex, ItemStack.EMPTY);

                // 更新NBT
                CompoundTag newNbt = nbt.copy();
                newNbt.put(SHOW_ITEMS, tableItems.serializeNBT());
                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        info.pos(), state, newNbt);
                ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);
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
                ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);
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
                ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);

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
                ContraptionInteractionUtil.dropItemToPlayer(contraptionEntity, localPos, player, carpetItem);

                // 更新NBT中的颜色
                CompoundTag newNbt = nbt.copy();
                newNbt.putInt(COLOR_TAG, dyeColor.getId());

                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        info.pos(), state, newNbt);
                ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos, newInfo);

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

        // 获取手持的FoodBiteBlock
        Block heldBlock = Block.byItem(itemInHand.getItem());
        if (!(heldBlock instanceof FoodBiteBlock foodBlock)) {
            return false;
        }

        Direction facing = player.getDirection().getOpposite();

        // 检查上方空间是否足够
        if (!canPlaceFoodBlock(contraptionEntity, abovePos, foodBlock, facing)) {
            return false;
        }

        // 在服务端执行放置逻辑
        if (!contraptionEntity.level().isClientSide) {
            if (foodBlock instanceof FoodBiteOneByTwoBlock oneByTwoBlock) {
                // 1x2 方块需要同时放置 LEFT 和 RIGHT
                placeOneByTwoFoodBlock(player, contraptionEntity, abovePos, oneByTwoBlock, itemInHand, facing);
            } else if (foodBlock instanceof FoodBiteThreeByThreeBlock threeByThreeBlock) {
                // 3x3 方块需要同时放置 9 个部分
                placeThreeByThreeFoodBlock(player, contraptionEntity, abovePos, threeByThreeBlock, itemInHand);
            } else {
                // 普通 1x1 方块
                placeSingleFoodBlock(player, contraptionEntity, abovePos, foodBlock, itemInHand);
            }
        }

        return true;
    }

    /**
     * 检查是否可以放置食物方块
     */
    private boolean canPlaceFoodBlock(AbstractContraptionEntity contraptionEntity, BlockPos abovePos, FoodBiteBlock foodBlock, Direction facing) {
        if (foodBlock instanceof FoodBiteOneByTwoBlock) {
            // 1x2 需要检查两个位置（根据朝向计算 LEFT 和 RIGHT 位置）
            BlockPos rightPos = abovePos;
            BlockPos leftPos = abovePos.relative(facing.getClockWise());
            StructureTemplate.StructureBlockInfo rightInfo = contraptionEntity.getContraption().getBlocks().get(rightPos);
            StructureTemplate.StructureBlockInfo leftInfo = contraptionEntity.getContraption().getBlocks().get(leftPos);
            // 两个位置都必须是空的（没有方块或只有空气）
            boolean rightEmpty = rightInfo == null || rightInfo.state().isAir();
            boolean leftEmpty = leftInfo == null || leftInfo.state().isAir();
            return rightEmpty && leftEmpty;
        } else if (foodBlock instanceof FoodBiteThreeByThreeBlock) {
            // 3x3 需要检查 9 个位置
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    BlockPos checkPos = abovePos.offset(i, 0, j);
                    StructureTemplate.StructureBlockInfo checkInfo = contraptionEntity.getContraption().getBlocks().get(checkPos);
                    if (checkInfo != null && !checkInfo.state().isAir()) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            // 普通 1x1 方块
            StructureTemplate.StructureBlockInfo aboveInfo = contraptionEntity.getContraption().getBlocks().get(abovePos);
            return aboveInfo == null || aboveInfo.state().isAir();
        }
    }

    /**
     * 放置普通 1x1 食物方块
     */
    private void placeSingleFoodBlock(Player player, AbstractContraptionEntity contraptionEntity, BlockPos abovePos,
                                      FoodBiteBlock foodBlock, ItemStack itemInHand) {
        BlockState newState = foodBlock.defaultBlockState()
                .setValue(foodBlock.getBites(), 0)
                .setValue(FoodBiteBlock.FACING, player.getDirection().getOpposite())
                .setValue(FoodBiteBlock.QUALITY, FoodBiteBlockMovingInteraction.getQualityId(itemInHand));

        StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                abovePos, newState, null);

        setContraptionBlockDataLocally(contraptionEntity, abovePos, newInfo);

        // 注册交互行为
        MovingInteractionBehaviour interactionBehaviour = MovingInteractionBehaviour.REGISTRY.get(newState);
        if (interactionBehaviour != null) {
            contraptionEntity.getContraption().getInteractors().put(abovePos, interactionBehaviour);
        }

        // 更新 bounds 并同步
        var updatedBounds = ContraptionInteractionUtil.updateBounds(contraptionEntity, abovePos);
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

        // 消耗物品
        if (!player.isCreative()) {
            itemInHand.shrink(1);
        }

        // 播放音效
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(abovePos), 1.0f);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
        contraptionEntity.level().playSound(null, soundPos, newState.getSoundType().getPlaceSound(),
                SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /**
     * 放置 1x2 食物方块（LEFT 和 RIGHT）
     */
    private void placeOneByTwoFoodBlock(Player player, AbstractContraptionEntity contraptionEntity, BlockPos abovePos,
                                        FoodBiteOneByTwoBlock foodBlock, ItemStack itemInHand, Direction facing) {
        BlockPos rightPos = abovePos;
        BlockPos leftPos = abovePos.relative(facing.getClockWise());

        // RIGHT 位置的状态
        BlockState rightState = foodBlock.defaultBlockState()
                .setValue(foodBlock.getBites(), 0)
                .setValue(FoodBiteBlock.FACING, facing)
                .setValue(FoodBiteBlock.QUALITY, FoodBiteBlockMovingInteraction.getQualityId(itemInHand))
                .setValue(FoodBiteOneByTwoBlock.POSITION, FoodBiteOneByTwoBlock.RIGHT);

        // LEFT 位置的状态
        BlockState leftState = foodBlock.defaultBlockState()
                .setValue(foodBlock.getBites(), 0)
                .setValue(FoodBiteBlock.FACING, facing)
                .setValue(FoodBiteBlock.QUALITY, FoodBiteBlockMovingInteraction.getQualityId(itemInHand))
                .setValue(FoodBiteOneByTwoBlock.POSITION, FoodBiteOneByTwoBlock.LEFT);

        StructureTemplate.StructureBlockInfo rightInfo = new StructureTemplate.StructureBlockInfo(rightPos, rightState, null);
        StructureTemplate.StructureBlockInfo leftInfo = new StructureTemplate.StructureBlockInfo(leftPos, leftState, null);

        // 放置两个方块
        setContraptionBlockDataLocally(contraptionEntity, rightPos, rightInfo);
        setContraptionBlockDataLocally(contraptionEntity, leftPos, leftInfo);

        // 注册交互行为（只在 RIGHT 位置注册，因为交互会转发到 LEFT）
        MovingInteractionBehaviour interactionBehaviour = MovingInteractionBehaviour.REGISTRY.get(rightState);
        if (interactionBehaviour != null) {
            contraptionEntity.getContraption().getInteractors().put(rightPos, interactionBehaviour);
            contraptionEntity.getContraption().getInteractors().put(leftPos, interactionBehaviour);
        }

        // 更新 bounds（包含两个位置）
        var updatedBounds = ContraptionInteractionUtil.updateBounds(contraptionEntity, rightPos);
        updatedBounds = ContraptionInteractionUtil.updateBounds(contraptionEntity, leftPos);

        // 同步到客户端
        KCPacketHandler.sendToTracking(
                new KCContraptionChangedPacket(
                        contraptionEntity.getId(),
                        rightPos,
                        rightInfo.state(),
                        rightInfo.nbt(),
                        updatedBounds
                ),
                contraptionEntity
        );
        KCPacketHandler.sendToTracking(
                new KCContraptionChangedPacket(
                        contraptionEntity.getId(),
                        leftPos,
                        leftInfo.state(),
                        leftInfo.nbt(),
                        updatedBounds
                ),
                contraptionEntity
        );

        // 消耗物品
        if (!player.isCreative()) {
            itemInHand.shrink(1);
        }

        // 播放音效
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(abovePos), 1.0f);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
        contraptionEntity.level().playSound(null, soundPos, rightState.getSoundType().getPlaceSound(),
                SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /**
     * 放置 3x3 食物方块（9 个部分）
     */
    private void placeThreeByThreeFoodBlock(Player player, AbstractContraptionEntity contraptionEntity, BlockPos centerPos,
                                            FoodBiteThreeByThreeBlock foodBlock, ItemStack itemInHand) {
        Direction facing = player.getDirection().getOpposite();

        // 计算所有 9 个位置
        BlockPos[] positions = new BlockPos[9];
        StructureTemplate.StructureBlockInfo[] infos = new StructureTemplate.StructureBlockInfo[9];
        int idx = 0;

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockPos pos = centerPos.offset(i, 0, j);
                positions[idx] = pos;

                NinePart part = NinePart.getPartByPos(i, j);
                BlockState state = foodBlock.defaultBlockState()
                        .setValue(foodBlock.getBites(), 0)
                        .setValue(FoodBiteBlock.FACING, facing)
                        .setValue(FoodBiteBlock.QUALITY, FoodBiteBlockMovingInteraction.getQualityId(itemInHand))
                        .setValue(FoodBiteThreeByThreeBlock.PART, part);

                infos[idx] = new StructureTemplate.StructureBlockInfo(pos, state, null);
                idx++;
            }
        }

        // 放置所有方块
        AABB updatedBounds = null;
        MovingInteractionBehaviour interactionBehaviour = null;
        for (int i = 0; i < 9; i++) {
            setContraptionBlockDataLocally(contraptionEntity, positions[i], infos[i]);
            updatedBounds = ContraptionInteractionUtil.updateBounds(contraptionEntity, positions[i]);

            // 注册交互行为
            if (interactionBehaviour == null) {
                interactionBehaviour = MovingInteractionBehaviour.REGISTRY.get(infos[i].state());
            }
            if (interactionBehaviour != null) {
                contraptionEntity.getContraption().getInteractors().put(positions[i], interactionBehaviour);
            }
        }

        // 同步到客户端
        for (int i = 0; i < 9; i++) {
            KCPacketHandler.sendToTracking(
                    new KCContraptionChangedPacket(
                            contraptionEntity.getId(),
                            positions[i],
                            infos[i].state(),
                            infos[i].nbt(),
                            updatedBounds
                    ),
                    contraptionEntity
            );
        }

        // 消耗物品
        if (!player.isCreative()) {
            itemInHand.shrink(1);
        }

        // 播放音效
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(centerPos), 1.0f);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
        contraptionEntity.level().playSound(null, soundPos, infos[0].state().getSoundType().getPlaceSound(),
                SoundSource.BLOCKS, 1.0F, 0.8F);
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
            setContraptionBlockDataLocally(contraptionEntity, abovePos, newInfo);

            // 注册交互行为到interactors地图，使新放置的方块可以被交互
            MovingInteractionBehaviour interactionBehaviour = MovingInteractionBehaviour.REGISTRY.get(newState);
            if (interactionBehaviour != null) {
                contraptionEntity.getContraption().getInteractors().put(abovePos, interactionBehaviour);
            }

            // 更新Contraption的bounds
            var updatedBounds = ContraptionInteractionUtil.updateBounds(contraptionEntity, abovePos);

            // 通知客户端重新渲染Contraption（同步bounds）
            if (!contraptionEntity.level().isClientSide) {
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
            }

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

}
