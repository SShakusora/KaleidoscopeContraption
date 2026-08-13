package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.IncenseBlock;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.api.placement.ContraptionRemovalManager;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Preserves IncenseBlock's manual open/close interaction on a contraption.
 */
public final class TavernIncenseMovingInteraction extends MovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info =
                contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof IncenseBlock)) {
            return false;
        }

        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            ContraptionRemovalManager.Result result = ContraptionRemovalManager.tryRemove(
                    player, localPos, contraptionEntity);
            if (result != ContraptionRemovalManager.Result.NOT_REGISTERED) {
                return result == ContraptionRemovalManager.Result.REMOVED;
            }
            removeIncense(player, localPos, contraptionEntity, info);
            return true;
        }

        if (contraptionEntity.level().isClientSide) {
            return true;
        }
        BlockState updated = info.state().cycle(BlockStateProperties.OPEN);
        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos,
                new StructureTemplate.StructureBlockInfo(info.pos(), updated, info.nbt()));
        ContraptionInteractionUtil.playSound(contraptionEntity, localPos,
                updated.getValue(BlockStateProperties.OPEN)
                        ? SoundEvents.STONE_BUTTON_CLICK_ON : SoundEvents.STONE_BUTTON_CLICK_OFF,
                SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    private void removeIncense(Player player, BlockPos localPos,
                               AbstractContraptionEntity contraptionEntity,
                               StructureTemplate.StructureBlockInfo info) {
        if (contraptionEntity.level().isClientSide) {
            return;
        }
        if (!player.isCreative()) {
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(info.state().getBlock().asItem()));
        }
        TavernContraptionInteractionSupport.removeBlock(contraptionEntity, localPos, info.state());
    }
}

