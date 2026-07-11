package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.MillstoneBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.MillstoneBlockEntity;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class MillstoneBlockMovingInteraction extends BlockEntityDelegatingMovingInteraction {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand hand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo clickedInfo = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (clickedInfo == null || !(clickedInfo.state().getBlock() instanceof MillstoneBlock)) return false;
        var part = clickedInfo.state().getValue(MillstoneBlock.PART);
        BlockPos centerPos = localPos.subtract(new Vec3i(part.getPosX(), 0, part.getPosY()));
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(centerPos);
        if (info == null) return false;
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() || contraptionEntity.level().isClientSide) return false;
        MillstoneBlockEntity blockEntity = loadBlockEntity(
                new MillstoneBlockEntity(centerPos, info.state()), info.nbt(), contraptionEntity);
        boolean accepted = blockEntity.onPutItem(contraptionEntity.level(), held);
        if (accepted) saveBlockEntity(contraptionEntity, centerPos, info, blockEntity);
        return accepted;
    }
}
