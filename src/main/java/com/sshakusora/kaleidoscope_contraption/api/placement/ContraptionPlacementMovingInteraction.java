package com.sshakusora.kaleidoscope_contraption.api.placement;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

public class ContraptionPlacementMovingInteraction extends MovingInteractionBehaviour {
    private final ResourceLocation pointType;

    public ContraptionPlacementMovingInteraction(ResourceLocation pointType) {
        this.pointType = Objects.requireNonNull(pointType, "pointType");
    }

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        return ContraptionPlacementManager.tryPlace(pointType, player, activeHand, localPos, contraptionEntity);
    }
}
