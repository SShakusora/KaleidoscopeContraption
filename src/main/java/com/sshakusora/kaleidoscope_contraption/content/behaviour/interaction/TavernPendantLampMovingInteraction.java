package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.PendantLampBlock;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes both halves of Tavern's pendant lamps as one contraption block.
 */
public final class TavernPendantLampMovingInteraction extends MovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info =
                contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof PendantLampBlock)
                || !KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return false;
        }
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        List<BlockPos> positions = getLampPositions(localPos, info, contraptionEntity);
        DoubleBlockHalf half = info.state().getValue(PendantLampBlock.HALF);
        if (!player.isCreative() && shouldReturnItem(half, positions.size() == 2)) {
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(info.state().getBlock().asItem()));
        }
        ContraptionInteractionUtil.removeBlocksFromContraption(
                contraptionEntity, positions.toArray(BlockPos[]::new));
        var bounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);
        contraptionEntity.getContraption().invalidateColliders();
        ContraptionInteractionUtil.syncBlockRemoval(
                contraptionEntity, bounds, positions.toArray(BlockPos[]::new));
        ContraptionInteractionUtil.playBreakSound(contraptionEntity, localPos, info.state());
        return true;
    }

    private List<BlockPos> getLampPositions(BlockPos localPos,
                                            StructureTemplate.StructureBlockInfo info,
                                            AbstractContraptionEntity contraptionEntity) {
        List<BlockPos> positions = new ArrayList<>(2);
        positions.add(localPos);
        DoubleBlockHalf half = info.state().getValue(PendantLampBlock.HALF);
        BlockPos otherPos = getOtherHalfPos(localPos, half);
        StructureTemplate.StructureBlockInfo other =
                contraptionEntity.getContraption().getBlocks().get(otherPos);
        if (other != null && other.state().getBlock() == info.state().getBlock()
                && other.state().getValue(PendantLampBlock.HALF) != half) {
            positions.add(otherPos);
        }
        return positions;
    }

    static BlockPos getOtherHalfPos(BlockPos localPos, DoubleBlockHalf half) {
        return half == DoubleBlockHalf.UPPER ? localPos.below() : localPos.above();
    }

    static boolean shouldReturnItem(DoubleBlockHalf half, boolean hasOtherHalf) {
        return hasOtherHalf || half == DoubleBlockHalf.LOWER;
    }
}

