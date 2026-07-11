package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.MillstoneBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.MillstoneBlockEntity;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionDataUtil;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

public class MillstoneBlockMovementBehaviour implements MovementBehaviour {
    @Override
    public void tick(MovementContext context) {
        if (context.world.isClientSide) return;
        StructureTemplate.StructureBlockInfo info = context.contraption.getBlocks().get(context.localPos);
        if (info == null || !(info.state().getBlock() instanceof MillstoneBlock)
                || !info.state().getValue(MillstoneBlock.PART).isCenter() || info.nbt() == null) return;

        BlockPos worldPos = BlockPos.containing(context.contraption.entity == null
                ? Vec3.atCenterOf(context.localPos)
                : context.contraption.entity.toGlobalVector(Vec3.atCenterOf(context.localPos), 1));
        MillstoneBlockEntity blockEntity = new MillstoneBlockEntity(worldPos, info.state());
        blockEntity.setLevel(context.world);
        blockEntity.load(info.nbt().copy());
        blockEntity.tick(context.world);
        CompoundTag updated = blockEntity.saveWithFullMetadata();
        if (info.nbt().hasUUID("EntityId")
                && !Util.NIL_UUID.equals(info.nbt().getUUID("EntityId"))
                && updated.hasUUID("EntityId")
                && info.nbt().getUUID("EntityId").equals(updated.getUUID("EntityId"))) {
            // The temporary BE rebinds the same entity every tick. Its bindEntity()
            // continuity correction is only valid on the first bind, so do not
            // persist that repeated CacheRot adjustment into the BER state.
            updated.putFloat("CacheRot", info.nbt().getFloat("CacheRot"));
        }
        updated.remove("x");
        updated.remove("y");
        updated.remove("z");
        ContraptionDataUtil.updateContraptionData(context, info.state(), updated,
                context.world.getGameTime() % 10 == 0);
    }
}
