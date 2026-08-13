package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.BarCabinetBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.BottleBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.brew.BarCabinetBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.init.tag.TagMod;
import com.github.ysbbbbbb.kaleidoscopetavern.item.BottleBlockItem;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

public class TavernBarCabinetMovingInteraction extends BlockEntityDelegatingMovingInteraction {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        if (activeHand != InteractionHand.MAIN_HAND) {
            return false;
        }

        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof BarCabinetBlock)) {
            return false;
        }

        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return removeBarCabinet(player, localPos, contraptionEntity, info);
        }

        Optional<TavernContraptionInteractionSupport.Hit> hit =
                TavernContraptionInteractionSupport.findHit(player, localPos, contraptionEntity);
        if (hit.isEmpty()) {
            return false;
        }

        BarCabinetBlockEntity cabinet = loadBlockEntity(
                new BarCabinetBlockEntity(localPos, info.state()), info.nbt(), contraptionEntity);
        ItemStack held = player.getItemInHand(activeHand);
        ItemStack leftItem = cabinet.getLeftItem();
        ItemStack rightItem = cabinet.getRightItem();
        boolean isLeftSide = TavernContraptionInteractionSupport.isLeftSide(
                info.state().getValue(BlockStateProperties.HORIZONTAL_FACING), localPos, hit.get().point());
        boolean irregular = false;
        boolean single = cabinet.isSingle();
        BottleBlock bottleBlock = getBottleBlock(held);

        if (bottleBlock != null) {
            if (single) {
                return false;
            }
            if (held.is(TagMod.BAR_CABINET_IRREGULAR)) {
                if (!leftItem.isEmpty() || !rightItem.isEmpty()) {
                    return false;
                }
                isLeftSide = true;
                irregular = true;
            } else {
                if (!leftItem.isEmpty() && rightItem.isEmpty() && isLeftSide) {
                    isLeftSide = false;
                } else if (leftItem.isEmpty() && !rightItem.isEmpty() && !isLeftSide) {
                    isLeftSide = true;
                }
            }
        } else if (!held.isEmpty()) {
            return false;
        } else if (single) {
            isLeftSide = true;
            irregular = true;
        } else {
            if (leftItem.isEmpty() && !rightItem.isEmpty() && isLeftSide) {
                isLeftSide = false;
            } else if (!leftItem.isEmpty() && rightItem.isEmpty() && !isLeftSide) {
                isLeftSide = true;
            }
        }

        ItemStack selected = isLeftSide ? leftItem : rightItem;
        if (held.isEmpty() && selected.isEmpty()) {
            return false;
        }
        if (!held.isEmpty() && !selected.isEmpty()) {
            return false;
        }

        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        if (held.isEmpty()) {
            player.setItemInHand(activeHand, selected.copy());
            if (isLeftSide) {
                cabinet.setLeftItem(ItemStack.EMPTY);
            } else {
                cabinet.setRightItem(ItemStack.EMPTY);
            }
            cabinet.setSingle(false);
            saveBlockEntity(contraptionEntity, localPos, info, cabinet);
            TavernContraptionInteractionSupport.playSound(contraptionEntity, localPos,
                    SoundEvents.GLASS_PLACE);
            return true;
        }

        ItemStack placed = held.split(1);
        if (isLeftSide) {
            cabinet.setLeftItem(placed);
        } else {
            cabinet.setRightItem(placed);
        }
        cabinet.setSingle(irregular);
        saveBlockEntity(contraptionEntity, localPos, info, cabinet);
        TavernContraptionInteractionSupport.playSound(contraptionEntity, localPos,
                SoundEvents.GLASS_PLACE);
        return true;
    }

    private boolean removeBarCabinet(Player player, BlockPos localPos,
                                     AbstractContraptionEntity contraptionEntity,
                                     StructureTemplate.StructureBlockInfo info) {
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        BarCabinetBlockEntity cabinet = loadBlockEntity(
                new BarCabinetBlockEntity(localPos, info.state()), info.nbt(), contraptionEntity);
        if (!player.isCreative()) {
            if (!cabinet.getLeftItem().isEmpty()) {
                player.getInventory().placeItemBackInInventory(cabinet.getLeftItem().copy());
            }
            if (!cabinet.getRightItem().isEmpty()) {
                player.getInventory().placeItemBackInInventory(cabinet.getRightItem().copy());
            }
            player.getInventory().placeItemBackInInventory(new ItemStack(info.state().getBlock().asItem()));
        }
        TavernContraptionInteractionSupport.removeBlock(contraptionEntity, localPos, info.state());
        return true;
    }

    private BottleBlock getBottleBlock(ItemStack stack) {
        if (stack.getItem() instanceof BottleBlockItem bottleItem
                && bottleItem.getBlock() instanceof BottleBlock bottleBlock) {
            return bottleBlock;
        }
        return null;
    }
}

