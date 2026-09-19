package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.drink.ClayPotMilkTeaBlock;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.items.ItemHandlerHelper;

/** Restores Cookery's disposable clay-pot milk tea interaction on contraptions. */
public final class ClayPotMilkTeaBlockMovingInteraction extends BlockEntityDelegatingMovingInteraction {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand hand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        if (hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()) {
            return false;
        }

        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof ClayPotMilkTeaBlock)) {
            return false;
        }
        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return removeBlock(player, localPos, contraptionEntity, info);
        }
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(info.state().getBlock().asItem()));
        removeFromContraption(localPos, contraptionEntity, info);
        return true;
    }

    private boolean removeBlock(Player player, BlockPos localPos,
                                AbstractContraptionEntity contraptionEntity,
                                StructureTemplate.StructureBlockInfo info) {
        if (contraptionEntity.level().isClientSide) {
            return true;
        }
        if (!player.isCreative()) {
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(info.state().getBlock().asItem()));
        }
        removeFromContraption(localPos, contraptionEntity, info);
        return true;
    }

    private void removeFromContraption(BlockPos localPos, AbstractContraptionEntity contraptionEntity,
                                       StructureTemplate.StructureBlockInfo info) {
        ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);
        var updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);
        contraptionEntity.getContraption().invalidateColliders();
        ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, updatedBounds);
        ContraptionInteractionUtil.playBreakSound(contraptionEntity, localPos, info.state());
    }
}
