package com.sshakusora.kaleidoscope_contraption.mixin.client;

import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.deco.TextBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.client.render.block.TextBlockEntityRender;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Uses the real contraption render path for Tavern text block entities. */
@Mixin(value = TextBlockEntityRender.class, remap = false)
public abstract class TavernTextBlockEntityRenderMixin {
    @Shadow(remap = false)
    protected abstract void renderModel(TextBlockEntity textBlock, PoseStack poseStack,
                                        MultiBufferSource buffer, int packedLight, int packedOverlay);

    @Shadow(remap = false)
    protected abstract void renderText(TextBlockEntity textBlock, PoseStack poseStack,
                                       MultiBufferSource buffer, int packedLight, int packedOverlay);

    @Inject(method = "render(Lcom/github/ysbbbbbb/kaleidoscopetavern/blockentity/deco/TextBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void kaleidoscopeContraption$renderVirtualText(TextBlockEntity textBlock, float partialTick,
                                                            PoseStack poseStack, MultiBufferSource buffer,
                                                            int packedLight, int packedOverlay, CallbackInfo ci) {
        if (!(textBlock.getLevel() instanceof VirtualRenderWorld)) {
            return;
        }

        renderModel(textBlock, poseStack, buffer, packedLight, packedOverlay);
        if (!textBlock.getText().isBlank()) {
            renderText(textBlock, poseStack, buffer, packedLight, packedOverlay);
        }
        ci.cancel();
    }
}

