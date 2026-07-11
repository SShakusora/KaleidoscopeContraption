package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.TableBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.util.CarpetColor;
import com.github.ysbbbbbb.kaleidoscopecookery.util.ItemUtils;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.api.placement.ContraptionPlacementManager;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.registry.KCContraptionPlacements;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
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

        if (ContraptionPlacementManager.tryPlace(KCContraptionPlacements.TABLE_TOP,
                player, activeHand, localPos, contraptionEntity)) {
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
            tableItems.deserializeNBT(contraptionEntity.level().registryAccess(), nbt.getCompound(SHOW_ITEMS));
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
                newNbt.put(SHOW_ITEMS, tableItems.serializeNBT(contraptionEntity.level().registryAccess()));
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
                newNbt.put(SHOW_ITEMS, tableItems.serializeNBT(contraptionEntity.level().registryAccess()));
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


}
