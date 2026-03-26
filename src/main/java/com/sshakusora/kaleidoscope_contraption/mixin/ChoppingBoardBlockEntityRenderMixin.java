package com.sshakusora.kaleidoscope_contraption.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.ChoppingBoardBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.client.render.block.ChoppingBoardBlockEntityRender;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChoppingBoardBlockEntityRender.class)
public class ChoppingBoardBlockEntityRenderMixin {

    @Inject(method = "render(Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/kitchen/ChoppingBoardBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void onRender(ChoppingBoardBlockEntity choppingBoard, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, CallbackInfo ci) {
        ResourceLocation modelId = choppingBoard.getModelId();
        if (modelId == null || modelId.toString().equals("kaleidoscope_contraption:no_model")) {
            ci.cancel();
        }
    }
}
