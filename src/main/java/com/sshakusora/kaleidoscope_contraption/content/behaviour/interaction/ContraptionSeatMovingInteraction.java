package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.actors.seat.SeatInteractionBehaviour;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.movement.ContraptionSeatSupport;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Objects;

/** Standard remove-key interaction for a registered entity-backed seat. */
public class ContraptionSeatMovingInteraction extends SeatInteractionBehaviour {
    private final ResourceLocation backend;

    public ContraptionSeatMovingInteraction(ResourceLocation backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        if (activeHand != InteractionHand.MAIN_HAND
                || !KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return super.handlePlayerInteraction(player, activeHand, localPos, contraptionEntity);
        }

        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !ContraptionSeatSupport.isSeat(info.state(), backend)) {
            return false;
        }
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        ContraptionSeatSupport.ejectPassengers(contraptionEntity, localPos);
        if (!player.isCreative()) {
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(info.state().getBlock().asItem()));
            giveAdditionalDrops(player, info);
        }

        ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);
        var updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);
        contraptionEntity.getContraption().invalidateColliders();
        ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, updatedBounds);
        ContraptionInteractionUtil.playBreakSound(contraptionEntity, localPos, info.state());
        return true;
    }

    /** Hook for backend-specific drops such as Cookery chair carpets. */
    protected void giveAdditionalDrops(Player player, StructureTemplate.StructureBlockInfo info) {
    }

    protected ResourceLocation backend() {
        return backend;
    }
}
