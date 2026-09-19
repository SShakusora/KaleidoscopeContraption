package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.ChairBlock;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.actors.seat.SeatInteractionBehaviour;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.movement.CookerySeatSupport;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Handles the remove-key path for Cookery's SitEntity-backed seats. */
public class CookerySeatMovingInteraction extends SeatInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        if (activeHand != InteractionHand.MAIN_HAND
                || !KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return super.handlePlayerInteraction(player, activeHand, localPos, contraptionEntity);
        }

        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !CookerySeatSupport.isCookerySeat(info.state())) {
            return false;
        }
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        CookerySeatSupport.ejectPassengers(contraptionEntity, localPos);
        if (!player.isCreative()) {
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(info.state().getBlock().asItem()));
            if (info.state().getBlock() instanceof ChairBlock
                    && info.state().getValue(ChairBlock.HAS_CARPET)
                    && info.nbt() != null) {
                DyeColor color = DyeColor.byId(info.nbt().getInt("CarpetColor"));
                ItemStack carpet = com.github.ysbbbbbb.kaleidoscopecookery.util.CarpetColor
                        .getCarpetByColor(color).getDefaultInstance();
                ItemHandlerHelper.giveItemToPlayer(player, carpet);
            }
        }

        ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);
        var updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);
        contraptionEntity.getContraption().invalidateColliders();
        ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, updatedBounds);
        ContraptionInteractionUtil.playBreakSound(contraptionEntity, localPos, info.state());
        return true;
    }
}
