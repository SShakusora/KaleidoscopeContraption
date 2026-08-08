package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.DrinkBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.brew.DrinkBlockEntity;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Adapts Tavern's stackable drink blocks to Create contraptions.
 *
 * <p>The block state count and the DrinkBlockEntity inventory are updated
 * together because the Tavern renderer uses both pieces of data.</p>
 */
public class DrinkBlockMovingInteraction extends BlockEntityDelegatingMovingInteraction {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof DrinkBlock drinkBlock)) {
            return false;
        }

        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return removeDrinkBlock(player, localPos, contraptionEntity, info, drinkBlock);
        }

        ItemStack held = player.getItemInHand(activeHand);
        BlockState state = info.state();
        int count = state.getValue(drinkBlock.getCountProperty());

        // Match DrinkBlockItem.useOn: only the same drink can increase this block.
        if (held.is(drinkBlock.asItem()) && count < drinkBlock.getMaxCount()) {
            if (contraptionEntity.level().isClientSide) {
                return true;
            }

            DrinkBlockEntity blockEntity = loadBlockEntity(
                    new DrinkBlockEntity(localPos, state), info.nbt(), contraptionEntity);
            if (!blockEntity.addItem(held)) {
                return false;
            }
            if (!player.isCreative()) {
                held.shrink(1);
            }

            saveBlockEntity(contraptionEntity, localPos, info,
                    state.setValue(drinkBlock.getCountProperty(), count + 1), blockEntity);
            playPlaceSound(contraptionEntity, localPos, state, player);
            return true;
        }

        if (!held.isEmpty()) {
            return false;
        }

        // DrinkBlock.use removes the last stored bottle and then reduces count.
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        DrinkBlockEntity blockEntity = loadBlockEntity(
                new DrinkBlockEntity(localPos, state), info.nbt(), contraptionEntity);
        ItemStack extracted = blockEntity.removeItem();
        if (extracted.isEmpty()) {
            return false;
        }
        ItemHandlerHelper.giveItemToPlayer(player, extracted);
        ContraptionInteractionUtil.playSound(player, contraptionEntity, localPos,
                SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (count > 1) {
            saveBlockEntity(contraptionEntity, localPos, info,
                    state.setValue(drinkBlock.getCountProperty(), count - 1), blockEntity);
        } else {
            removeBlock(contraptionEntity, localPos, info.state(), false);
        }
        return true;
    }

    private void saveBlockEntity(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                 StructureTemplate.StructureBlockInfo info, BlockState state,
                                 DrinkBlockEntity blockEntity) {
        CompoundTag tag = blockEntity.saveWithFullMetadata();
        tag.remove("x");
        tag.remove("y");
        tag.remove("z");
        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos,
                new StructureTemplate.StructureBlockInfo(info.pos(), state, tag));
    }

    private boolean removeDrinkBlock(Player player, BlockPos localPos,
                                     AbstractContraptionEntity contraptionEntity,
                                     StructureTemplate.StructureBlockInfo info,
                                     DrinkBlock drinkBlock) {
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        DrinkBlockEntity blockEntity = loadBlockEntity(
                new DrinkBlockEntity(localPos, info.state()), info.nbt(), contraptionEntity);
        if (!player.isCreative()) {
            for (ItemStack stack : blockEntity.getItems()) {
                if (!stack.isEmpty()) {
                    ItemHandlerHelper.giveItemToPlayer(player, stack.copy());
                }
            }
            ItemStack blockItem = new ItemStack(drinkBlock.asItem());
            ItemHandlerHelper.giveItemToPlayer(player, blockItem);
        }

        removeBlock(contraptionEntity, localPos, info.state(), true);
        return true;
    }

    private void removeBlock(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                             BlockState state, boolean playBreakSound) {
        ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);
        var updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);
        contraptionEntity.getContraption().invalidateColliders();
        ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, updatedBounds);
        if (playBreakSound) {
            ContraptionInteractionUtil.playBreakSound(contraptionEntity, localPos, state);
        }
    }

    private void playPlaceSound(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                BlockState state, Player player) {
        var sound = state.getSoundType(contraptionEntity.level(), localPos, player);
        ContraptionInteractionUtil.playSound(contraptionEntity, localPos, sound.getPlaceSound(),
                SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
    }
}
