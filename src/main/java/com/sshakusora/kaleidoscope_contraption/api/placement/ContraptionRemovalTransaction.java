package com.sshakusora.kaleidoscope_contraption.api.placement;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.sshakusora.kaleidoscope_contraption.network.KCContraptionChangedPacket;
import com.sshakusora.kaleidoscope_contraption.network.KCPacketHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.HashSet;
import java.util.Set;

public final class ContraptionRemovalTransaction {
    private ContraptionRemovalTransaction() {
    }

    public static boolean canRemove(ContraptionRemovalContext context, ContraptionRemovalResult result) {
        Contraption contraption = context.contraptionEntity().getContraption();
        Set<BlockPos> positions = new HashSet<>();
        for (BlockPos position : result.positions()) {
            if (!positions.add(position)) {
                return false;
            }
            StructureTemplate.StructureBlockInfo info = contraption.getBlocks().get(position);
            if (info == null || info.state().isAir()) {
                return false;
            }
        }
        for (StructureTemplate.StructureBlockInfo updatedBlock : result.updatedBlocks()) {
            if (updatedBlock.state().isAir() || !positions.add(updatedBlock.pos())) {
                return false;
            }
            StructureTemplate.StructureBlockInfo existing = contraption.getBlocks().get(updatedBlock.pos());
            if (existing == null || existing.state().isAir()) {
                return false;
            }
        }
        return true;
    }

    public static boolean commit(ContraptionRemovalContext context, ContraptionRemovalResult result) {
        AbstractContraptionEntity entity = context.contraptionEntity();
        if (entity.level().isClientSide || !canRemove(context, result)) {
            return false;
        }

        StructureTemplate.StructureBlockInfo soundInfo = entity.getContraption().getBlocks().get(context.targetPos());
        for (BlockPos position : result.positions()) {
            ContraptionInteractionUtil.removeBlockFromContraption(entity, position);
        }
        for (StructureTemplate.StructureBlockInfo updatedBlock : result.updatedBlocks()) {
            ContraptionInteractionUtil.updateContraptionDataLocally(entity, updatedBlock.pos(), updatedBlock);
        }

        var updatedBounds = ContraptionInteractionUtil.recalculateBounds(entity);
        entity.getContraption().invalidateColliders();
        ContraptionInteractionUtil.syncBlockRemoval(entity, updatedBounds,
                result.positions().toArray(BlockPos[]::new));
        for (StructureTemplate.StructureBlockInfo updatedBlock : result.updatedBlocks()) {
            StructureTemplate.StructureBlockInfo current = entity.getContraption().getBlocks().get(updatedBlock.pos());
            KCPacketHandler.sendToTracking(new KCContraptionChangedPacket(
                    entity.getId(), current.pos(), current.state(), current.nbt(), updatedBounds), entity);
        }

        if (!context.player().isCreative()) {
            for (ItemStack returnedItem : result.returnedItems()) {
                if (!returnedItem.isEmpty()) {
                    context.player().getInventory().placeItemBackInInventory(returnedItem.copy());
                }
            }
        }

        if (result.playBreakSound() && soundInfo != null) {
            ContraptionInteractionUtil.playBreakSound(entity, context.targetPos(), soundInfo.state());
        }
        return true;
    }
}
