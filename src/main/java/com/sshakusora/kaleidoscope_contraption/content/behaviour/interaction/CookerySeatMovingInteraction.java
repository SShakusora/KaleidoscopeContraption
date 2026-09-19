package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.ChairBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.util.CarpetColor;
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
import net.minecraftforge.items.ItemHandlerHelper;

/** Handles remove-key interaction for Cookery seats while preserving Create's seat behaviour. */
public final class CookerySeatMovingInteraction extends SeatInteractionBehaviour {
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

        removeSeat(player, localPos, contraptionEntity, info);
        return true;
    }

    private void removeSeat(Player player, BlockPos localPos,
                             AbstractContraptionEntity contraptionEntity,
                             StructureTemplate.StructureBlockInfo info) {
        if (contraptionEntity.level().isClientSide) {
            return;
        }

        CookerySeatSupport.ejectPassengers(contraptionEntity, localPos);
        if (!player.isCreative()) {
            ItemHandlerHelper.giveItemToPlayer(player,
                    new ItemStack(info.state().getBlock().asItem()));
            if (info.state().getBlock() instanceof ChairBlock
                    && info.state().getValue(ChairBlock.HAS_CARPET)) {
                int carpetColorId = info.nbt() == null ? DyeColor.WHITE.getId()
                        : info.nbt().getInt("CarpetColor");
                DyeColor carpetColor = DyeColor.byId(carpetColorId);
                ItemHandlerHelper.giveItemToPlayer(player,
                        CarpetColor.getCarpetByColor(carpetColor).getDefaultInstance());
            }
        }
        ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);
        var updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);
        contraptionEntity.getContraption().invalidateColliders();
        ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, updatedBounds);
        ContraptionInteractionUtil.playBreakSound(contraptionEntity, localPos, info.state());
    }
}
