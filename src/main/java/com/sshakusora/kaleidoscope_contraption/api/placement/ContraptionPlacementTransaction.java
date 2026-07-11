package com.sshakusora.kaleidoscope_contraption.api.placement;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.sshakusora.kaleidoscope_contraption.network.KCContraptionChangedPacket;
import com.sshakusora.kaleidoscope_contraption.network.KCPacketHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionBoundsUtil;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

public final class ContraptionPlacementTransaction {
    private ContraptionPlacementTransaction() {
    }

    public static boolean canPlace(ContraptionPlacementContext context, ContraptionPlacementResult result) {
        Contraption contraption = context.contraptionEntity().getContraption();
        Set<BlockPos> positions = new HashSet<>();

        if (!context.player().isCreative() && context.heldItem().getCount() < result.consumedItems()) {
            return false;
        }

        for (StructureTemplate.StructureBlockInfo info : result.blocks()) {
            if (info.state().isAir() || !positions.add(info.pos())) {
                return false;
            }
            StructureTemplate.StructureBlockInfo existing = contraption.getBlocks().get(info.pos());
            if (existing != null && !existing.state().isAir()) {
                return false;
            }
        }
        return true;
    }

    public static boolean commit(ContraptionPlacementContext context, ResourceLocation ruleId,
                                 ContraptionPlacementResult result) {
        AbstractContraptionEntity entity = context.contraptionEntity();
        if (entity.level().isClientSide || !canPlace(context, result)) {
            return false;
        }

        for (StructureTemplate.StructureBlockInfo info : result.blocks()) {
            CompoundTag nbt = info.nbt() == null ? new CompoundTag() : info.nbt().copy();
            nbt.putString(ContraptionPlacementRegistry.PLACEMENT_RULE_TAG, ruleId.toString());
            StructureTemplate.StructureBlockInfo markedInfo = new StructureTemplate.StructureBlockInfo(
                    info.pos(), info.state(), nbt);
            ContraptionInteractionUtil.updateContraptionDataLocally(entity, markedInfo.pos(), markedInfo);
        }

        AABB updatedBounds = ContraptionBoundsUtil.recalculateBounds(entity.getContraption());
        entity.getContraption().invalidateColliders();

        for (StructureTemplate.StructureBlockInfo info : result.blocks()) {
            StructureTemplate.StructureBlockInfo markedInfo = entity.getContraption().getBlocks().get(info.pos());
            KCPacketHandler.sendToTracking(new KCContraptionChangedPacket(
                    entity.getId(), markedInfo.pos(), markedInfo.state(), markedInfo.nbt(), updatedBounds), entity);
        }

        ItemStack heldItem = context.heldItem();
        if (!context.player().isCreative() && result.consumedItems() > 0) {
            heldItem.shrink(result.consumedItems());
        }

        if (result.playPlaceSound()) {
            StructureTemplate.StructureBlockInfo first = result.blocks().get(0);
            BlockPos soundPos = ContraptionInteractionUtil.getGlobalBlockPos(entity, first.pos());
            entity.level().playSound(null, soundPos, first.state().getSoundType().getPlaceSound(),
                    SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        return true;
    }
}
