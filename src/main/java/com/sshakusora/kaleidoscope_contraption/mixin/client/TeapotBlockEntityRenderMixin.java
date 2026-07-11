package com.sshakusora.kaleidoscope_contraption.mixin.client;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.client.render.block.TeapotBlockEntityRender;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.createmod.catnip.data.Couple;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.ref.WeakReference;

@Mixin(value = TeapotBlockEntityRender.class, remap = false)
public abstract class TeapotBlockEntityRenderMixin {
    @Redirect(
            method = "render(Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/kitchen/TeapotBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;cameraHitResult:Lnet/minecraft/world/phys/HitResult;",
                    opcode = Opcodes.GETFIELD,
                    remap = true
            ),
            remap = false
    )
    private HitResult kaleidoscopeContraption$useLocalContraptionHit(
            BlockEntityRenderDispatcher dispatcher, TeapotBlockEntity teapot, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        HitResult original = dispatcher.cameraHitResult;
        if (!(teapot.getLevel() instanceof VirtualRenderWorld)) {
            return original;
        }

        BlockHitResult localHit = kaleidoscopeContraption$findLocalHit(teapot);
        if (localHit != null && localHit.getBlockPos().equals(teapot.getBlockPos())) {
            return localHit;
        }

        Vec3 location = original == null ? Vec3.ZERO : original.getLocation();
        return BlockHitResult.miss(location, Direction.UP, teapot.getBlockPos().above());
    }

    @Unique
    private static BlockHitResult kaleidoscopeContraption$findLocalHit(TeapotBlockEntity teapot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return null;
        }

        Couple<Vec3> rayInputs = ContraptionHandlerClient.getRayInputs(minecraft.player);
        for (WeakReference<AbstractContraptionEntity> reference
                : ContraptionHandler.loadedContraptions.get(minecraft.level).values()) {
            AbstractContraptionEntity entity = reference.get();
            if (entity == null) {
                continue;
            }
            Contraption contraption = entity.getContraption();
            if (contraption == null
                    || contraption.getBlockEntityClientSide(teapot.getBlockPos()) != teapot) {
                continue;
            }
            return ContraptionHandlerClient.rayTraceContraption(
                    rayInputs.getFirst(), rayInputs.getSecond(), entity);
        }
        return null;
    }
}
