package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.misc.TrashCanBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.misc.TrashCanBlockEntity;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class TrashCanBlockMovingInteraction extends BlockEntityDelegatingMovingInteraction {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand hand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof TrashCanBlock)) return false;
        ItemStack held = player.getItemInHand(hand);
        if (contraptionEntity.level().isClientSide) {
            if (contraptionEntity.getContraption().getBlockEntityClientSide(localPos)
                    instanceof TrashCanBlockEntity blockEntity) {
                int gameTime = (int) contraptionEntity.level().getGameTime();
                if (held.isEmpty() && player.isSecondaryUseActive()) {
                    for (int slot = blockEntity.getStorage().getSlots() - 1; slot >= 0; slot--) {
                        if (!blockEntity.getStorage().getStackInSlot(slot).isEmpty()) {
                            blockEntity.withdrawState.start(gameTime);
                            break;
                        }
                    }
                } else if (!held.isEmpty()) {
                    blockEntity.putState.start(gameTime);
                }
            }
            return !held.isEmpty() || player.isSecondaryUseActive();
        }
        if (!held.isEmpty() || player.isSecondaryUseActive()) {
            TrashCanBlockEntity blockEntity = loadBlockEntity(
                    new TrashCanBlockEntity(localPos, info.state()), info.nbt(), contraptionEntity);
            if (held.isEmpty()) blockEntity.withdrawItem(player); else blockEntity.putItem(held);
            saveBlockEntity(contraptionEntity, localPos, info, blockEntity);
        }
        return !held.isEmpty() || player.isSecondaryUseActive();
    }
}
