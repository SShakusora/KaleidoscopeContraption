package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.BottleBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.api.placement.ContraptionRemovalManager;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Restores BottleBlock's empty-hand pickup behavior inside a moving contraption. */
public final class TavernBottleBlockMovingInteraction extends MovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info =
                contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof BottleBlock)) {
            return false;
        }

        boolean removeKeyPressed = KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID());
        if (!removeKeyPressed && !player.getItemInHand(activeHand).isEmpty()) {
            return false;
        }
        if (removeKeyPressed) {
            ContraptionRemovalManager.Result result = ContraptionRemovalManager.tryRemove(
                    player, localPos, contraptionEntity);
            if (result != ContraptionRemovalManager.Result.NOT_REGISTERED) {
                return result == ContraptionRemovalManager.Result.REMOVED;
            }
        }
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        if (!removeKeyPressed || !player.isCreative()) {
            ItemStack returned = getReturnedBottle(info.state().getBlock());
            if (!returned.isEmpty()) {
                ItemHandlerHelper.giveItemToPlayer(player, returned);
            }
        }
        TavernContraptionInteractionSupport.removeBlock(
                contraptionEntity, localPos,
                removeKeyPressed ? info.state().getSoundType().getBreakSound() : SoundEvents.STONE_PLACE);
        return true;
    }

    /** Mirrors Tavern's loot tables for BottleBlocks backed by vanilla bottle items. */
    public static ItemStack getReturnedBottle(Block block) {
        if (block == ModBlocks.WATER_BOTTLE.get()) {
            ItemStack water = new ItemStack(Items.POTION);
            water.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
            return water;
        }
        if (block == ModBlocks.HONEY_BOTTLE.get()) {
            return new ItemStack(Items.HONEY_BOTTLE);
        }
        if (block == ModBlocks.DRAGON_BREATH_BOTTLE.get()) {
            return new ItemStack(Items.DRAGON_BREATH);
        }
        if (block == ModBlocks.XP_BOTTLE.get()) {
            return new ItemStack(Items.EXPERIENCE_BOTTLE);
        }
        return new ItemStack(block.asItem());
    }
}
