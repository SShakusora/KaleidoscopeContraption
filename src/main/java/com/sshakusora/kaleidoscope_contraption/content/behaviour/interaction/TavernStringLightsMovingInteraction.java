package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.StringLightsBlock;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Supports recoloring and removing Tavern string lights while moving.
 */
public final class TavernStringLightsMovingInteraction extends MovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info =
                contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof StringLightsBlock current)) {
            return false;
        }

        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            ContraptionRemovalManager.Result result = ContraptionRemovalManager.tryRemove(
                    player, localPos, contraptionEntity);
            if (result != ContraptionRemovalManager.Result.NOT_REGISTERED) {
                return result == ContraptionRemovalManager.Result.REMOVED;
            }
            removeLights(player, localPos, contraptionEntity, info);
            return true;
        }

        ItemStack held = player.getItemInHand(activeHand);
        Item dye = held.getItem();
        StringLightsBlock transformed = StringLightsBlock.TRANSFORM_MAP.get(dye);
        if (transformed == null || dye == current.dyeItem) {
            return false;
        }
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        BlockState updated = transformed.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING,
                        info.state().getValue(BlockStateProperties.HORIZONTAL_FACING))
                .setValue(BlockStateProperties.WATERLOGGED,
                        info.state().getValue(BlockStateProperties.WATERLOGGED));
        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos,
                new StructureTemplate.StructureBlockInfo(info.pos(), updated, info.nbt()));
        if (!player.isCreative()) {
            held.shrink(1);
        }
        ContraptionInteractionUtil.playSound(contraptionEntity, localPos,
                SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        contraptionEntity.level().levelEvent(player,
                LevelEvent.PARTICLES_PLANT_GROWTH,
                ContraptionInteractionUtil.getGlobalBlockPos(contraptionEntity, localPos), 0);
        return true;
    }

    private void removeLights(Player player, BlockPos localPos,
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
