package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.PotionBottleBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.brew.PotionBottleBlockEntity;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Preserves the PotionBottleBlockEntity's serialized potion ItemStack in a contraption. */
public class PotionBottleBlockMovingInteraction extends BlockEntityDelegatingMovingInteraction {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof PotionBottleBlock)) {
            return false;
        }

        boolean removeKeyPressed = KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID());
        if (!removeKeyPressed && !player.getItemInHand(activeHand).isEmpty()) {
            return false;
        }
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        PotionBottleBlockEntity blockEntity = loadBlockEntity(
                new PotionBottleBlockEntity(localPos, info.state()), info.nbt(), contraptionEntity);
        if (!player.isCreative() || !removeKeyPressed) {
            ItemStack potion = blockEntity.getPotionStack().copyWithCount(1);
            if (!potion.isEmpty()) {
                ItemHandlerHelper.giveItemToPlayer(player, potion);
            }

            // PotionBottleBlock has no registered custom block item in the Tavern item registry,
            // but keep the fallback for packs that provide one through a registry extension.
            ItemStack blockItem = new ItemStack(info.state().getBlock().asItem());
            if (!blockItem.isEmpty() && (potion.isEmpty() || !blockItem.is(potion.getItem()))) {
                ItemHandlerHelper.giveItemToPlayer(player, blockItem);
            }
        }

        TavernContraptionInteractionSupport.removeBlock(
                contraptionEntity, localPos, SoundEvents.STONE_PLACE);
        return true;
    }
}

