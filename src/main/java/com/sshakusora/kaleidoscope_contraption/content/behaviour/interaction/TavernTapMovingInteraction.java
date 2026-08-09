package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.TapBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.movement.TavernTapMovementBehaviour;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.items.ItemHandlerHelper;

public final class TavernTapMovingInteraction extends SyncedMovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        if (activeHand != InteractionHand.MAIN_HAND) {
            return false;
        }
        StructureTemplate.StructureBlockInfo info =
                contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof TapBlock)) {
            return false;
        }
        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return removeTap(player, localPos, contraptionEntity, info);
        }
        if (contraptionEntity.level().isClientSide) {
            return true;
        }
        if (info.state().getValue(TapBlock.OPEN)) {
            TavernTapMovementBehaviour.close(
                    contraptionEntity, localPos, info.state(), info.nbt(), true);
            return true;
        }
        return TavernTapMovementBehaviour.start(
                contraptionEntity, localPos, info.state(), info.nbt(), player);
    }

    private boolean removeTap(Player player, BlockPos localPos,
                              AbstractContraptionEntity entity,
                              StructureTemplate.StructureBlockInfo info) {
        if (entity.level().isClientSide) {
            return true;
        }
        if (!player.isCreative()) {
            ItemStack stack = new ItemStack(ModBlocks.TAP.get());
            if (info.nbt() != null) {
                BlockItem.setBlockEntityData(stack, ModBlocks.TAP_BE.get(), info.nbt().copy());
            }
            ItemHandlerHelper.giveItemToPlayer(player, stack);
        }
        ContraptionInteractionUtil.removeBlockFromContraption(entity, localPos);
        var bounds = ContraptionInteractionUtil.recalculateBounds(entity);
        entity.getContraption().invalidateColliders();
        ContraptionInteractionUtil.syncBlockRemoval(entity, localPos, bounds);
        ContraptionInteractionUtil.playBreakSound(entity, localPos, info.state());
        return true;
    }
}
