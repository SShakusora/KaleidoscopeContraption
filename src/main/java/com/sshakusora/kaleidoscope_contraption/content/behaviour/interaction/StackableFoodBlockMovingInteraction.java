package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.StackableFoodBlock;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.items.ItemHandlerHelper;

public class StackableFoodBlockMovingInteraction extends MovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand hand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof StackableFoodBlock block)) {
            return false;
        }
        BlockState state = info.state();
        ItemStack held = player.getItemInHand(hand);
        var countProperty = block.getCountProperty();
        int count = state.getValue(countProperty);

        if (held.is(block.asItem()) && count < block.getMaxCount()) {
            if (!contraptionEntity.level().isClientSide) {
                if (!player.isCreative()) held.shrink(1);
                update(contraptionEntity, localPos, info, state.setValue(countProperty, count + 1));
            }
            playPlaceSound(contraptionEntity, localPos, state, player);
            return true;
        }
        if (!held.isEmpty()) return false;

        if (!contraptionEntity.level().isClientSide) {
            ItemHandlerHelper.giveItemToPlayer(player, block.asItem().getDefaultInstance());
            if (count == 1) {
                ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);
                var bounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);
                ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, bounds);
            } else {
                update(contraptionEntity, localPos, info, state.setValue(countProperty, count - 1));
            }
        }
        return true;
    }

    private void update(AbstractContraptionEntity entity, BlockPos pos,
                        StructureTemplate.StructureBlockInfo info, BlockState state) {
        ContraptionInteractionUtil.updateContraptionData(entity, pos,
                new StructureTemplate.StructureBlockInfo(info.pos(), state, info.nbt()));
    }

    private void playPlaceSound(AbstractContraptionEntity entity, BlockPos pos, BlockState state, Player player) {
        SoundType sound = state.getSoundType(entity.level(), pos, player);
        ContraptionInteractionUtil.playSound(entity, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1) / 2f, sound.getPitch() * 0.8f);
    }
}
