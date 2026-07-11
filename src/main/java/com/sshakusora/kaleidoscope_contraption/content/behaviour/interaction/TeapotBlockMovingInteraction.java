package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.TeapotBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.util.FluidUtils;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class TeapotBlockMovingInteraction extends BlockEntityDelegatingMovingInteraction {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand hand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof TeapotBlock)) return false;
        ItemStack held = player.getItemInHand(hand);
        if (contraptionEntity.level().isClientSide) return true;

        TeapotBlockEntity blockEntity = loadBlockEntity(
                new TeapotBlockEntity(localPos, info.state()), info.nbt(), contraptionEntity);
        boolean changed;
        if (held.getCapability(Capabilities.FluidHandler.ITEM) != null) {
            changed = FluidUtils.hasFluid(held)
                    ? blockEntity.addTeaFluid(contraptionEntity.level(), player, held)
                    : blockEntity.removeTeaFluid(contraptionEntity.level(), player, held);
        } else if (!held.isEmpty()) {
            changed = blockEntity.addIngredient(contraptionEntity.level(), player, held);
        } else if (player.isSecondaryUseActive()) {
            changed = blockEntity.removeIngredient(contraptionEntity.level(), player);
        } else {
            for (ItemStack drop : blockEntity.getDrops()) ItemHandlerHelper.giveItemToPlayer(player, drop);
            ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);
            var bounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);
            ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, bounds);
            return true;
        }
        if (changed) saveBlockEntity(contraptionEntity, localPos, info, blockEntity);
        return changed;
    }
}
