package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import com.sshakusora.kaleidoscope_contraption.client.render.ContraptionBarStoolRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Renders Tavern bar stool bodies as Create actors so they can follow seated passengers.
 */
public class TavernBarStoolMovementBehaviour extends TavernSeatMovementBehaviour {
    @Override
    public boolean disableBlockEntityRendering() {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
                                    ContraptionMatrices matrices, MultiBufferSource buffer) {
        ContraptionBarStoolRenderer.render(context, renderWorld, matrices, buffer);
    }
}
