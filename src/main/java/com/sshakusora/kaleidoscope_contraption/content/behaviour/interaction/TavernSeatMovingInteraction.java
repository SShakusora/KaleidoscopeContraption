package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.BarStoolBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.IConnectionBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.SofaBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.properties.ConnectionType;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.actors.seat.SeatInteractionBehaviour;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.movement.TavernSeatSupport;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.HashSet;
import java.util.Set;

/** Preserves Tavern's own sitting behavior for moving sofas and bar stools. */
public class TavernSeatMovingInteraction extends SeatInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        if (activeHand != InteractionHand.MAIN_HAND
                || !KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return super.handlePlayerInteraction(player, activeHand, localPos, contraptionEntity);
        }

        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof SofaBlock)
                && !(info.state().getBlock() instanceof BarStoolBlock)) {
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
        TavernSeatSupport.ejectPassengers(contraptionEntity, localPos);
        if (!player.isCreative()) {
            ItemHandlerHelper.giveItemToPlayer(player,
                    new ItemStack(info.state().getBlock().asItem()));
        }
        ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);
        refreshSofaNeighbours(contraptionEntity, localPos);
        var updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);
        contraptionEntity.getContraption().invalidateColliders();
        ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, updatedBounds);
        ContraptionInteractionUtil.playBreakSound(contraptionEntity, localPos, info.state());
    }

    private void refreshSofaNeighbours(AbstractContraptionEntity contraptionEntity, BlockPos removedPos) {
        Set<BlockPos> neighbours = new HashSet<>();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            neighbours.add(removedPos.relative(direction));
        }
        for (BlockPos neighbourPos : neighbours) {
            StructureTemplate.StructureBlockInfo info =
                    contraptionEntity.getContraption().getBlocks().get(neighbourPos);
            if (info == null || !(info.state().getBlock() instanceof SofaBlock)) {
                continue;
            }
            BlockState refreshed = recalculateSofaState(contraptionEntity, neighbourPos, info.state());
            if (!refreshed.equals(info.state())) {
                ContraptionInteractionUtil.updateContraptionData(contraptionEntity, neighbourPos,
                        new StructureTemplate.StructureBlockInfo(info.pos(), refreshed, info.nbt()));
            }
        }
    }

    private BlockState recalculateSofaState(AbstractContraptionEntity contraptionEntity,
                                            BlockPos pos, BlockState state) {
        Direction facing = state.getValue(SofaBlock.FACING);
        BlockState left = getState(contraptionEntity, pos.relative(facing.getClockWise()));
        BlockState right = getState(contraptionEntity, pos.relative(facing.getCounterClockWise()));
        BlockState front = getState(contraptionEntity, pos.relative(facing));
        boolean leftConnected = IConnectionBlock.leftConnected(left, facing,
                candidate -> candidate.getBlock() instanceof SofaBlock);
        boolean rightConnected = IConnectionBlock.rightConnected(right, facing,
                candidate -> candidate.getBlock() instanceof SofaBlock);
        boolean frontLeftConnected = IConnectionBlock.frontLeftConnected(front, facing,
                candidate -> candidate.getBlock() instanceof SofaBlock);
        boolean frontRightConnected = IConnectionBlock.frontRightConnected(front, facing,
                candidate -> candidate.getBlock() instanceof SofaBlock);
        ConnectionType connection = IConnectionBlock.getConnectionType(
                leftConnected, rightConnected, frontLeftConnected, frontRightConnected);
        return state.setValue(SofaBlock.CONNECTION, connection);
    }

    private BlockState getState(AbstractContraptionEntity contraptionEntity, BlockPos pos) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(pos);
        return info == null ? Blocks.AIR.defaultBlockState() : info.state();
    }
}
