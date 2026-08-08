package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.ChalkboardBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.SandwichBoardBlock;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.List;

/** Interaction adapter for Tavern's two- and six-block text boards. */
public class TavernTextBoardMovingInteraction extends BlockEntityDelegatingMovingInteraction {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof SandwichBoardBlock)
                && !(info.state().getBlock() instanceof ChalkboardBlock)) {
            return false;
        }

        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return removeBoard(player, localPos, contraptionEntity, info);
        }
        return TavernTextBoardSupport.handleInteraction(player, activeHand, localPos, contraptionEntity);
    }

    private boolean removeBoard(Player player, BlockPos clickedPos,
                                AbstractContraptionEntity contraptionEntity,
                                StructureTemplate.StructureBlockInfo clickedInfo) {
        if (contraptionEntity.level().isClientSide) {
            return true;
        }
        List<BlockPos> structurePositions = TavernTextBoardSupport
                .getStructurePositions(contraptionEntity, clickedPos);
        List<BlockPos> existingPositions = new ArrayList<>();
        for (BlockPos pos : structurePositions) {
            StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(pos);
            if (info != null && TavernTextBoardSupport.isBoard(info.state())) {
                existingPositions.add(pos);
            }
        }
        if (existingPositions.isEmpty()) {
            return false;
        }

        if (!player.isCreative()) {
            ItemStack board = new ItemStack(clickedInfo.state().getBlock().asItem());
            ContraptionInteractionUtil.dropItemToPlayer(contraptionEntity, clickedPos, player, board);
        }
        ContraptionInteractionUtil.removeBlocksFromContraption(
                contraptionEntity, existingPositions.toArray(BlockPos[]::new));
        var updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);
        contraptionEntity.getContraption().invalidateColliders();
        ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, updatedBounds,
                existingPositions.toArray(BlockPos[]::new));
        ContraptionInteractionUtil.playBreakSound(contraptionEntity, clickedPos, clickedInfo.state());
        return true;
    }
}
