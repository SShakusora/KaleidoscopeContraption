package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.MolotovBlock;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.api.placement.ContraptionRemovalManager;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.items.ItemHandlerHelper;

/** Handles Tavern's Molotov block inside a contraption. */
public class TavernMolotovBlockMovingInteraction extends MovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof MolotovBlock)) {
            return false;
        }

        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            ContraptionRemovalManager.Result result = ContraptionRemovalManager.tryRemove(
                    player, localPos, contraptionEntity);
            if (result == ContraptionRemovalManager.Result.NOT_REGISTERED) {
                removeUnregistered(player, localPos, contraptionEntity, info);
                return true;
            }
            return result == ContraptionRemovalManager.Result.REMOVED;
        }

        if (!player.getItemInHand(activeHand).isEmpty()) {
            return false;
        }
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(info.state().getBlock().asItem()));
        TavernContraptionInteractionSupport.removeBlock(
                contraptionEntity, localPos, SoundEvents.STONE_PLACE);
        return true;
    }

    private void removeUnregistered(Player player, BlockPos localPos,
                                    AbstractContraptionEntity contraptionEntity,
                                    StructureTemplate.StructureBlockInfo info) {
        if (contraptionEntity.level().isClientSide) {
            return;
        }
        if (!player.isCreative()) {
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(info.state().getBlock().asItem()));
        }
        TavernContraptionInteractionSupport.removeBlock(
                contraptionEntity, localPos, info.state());
    }
}
