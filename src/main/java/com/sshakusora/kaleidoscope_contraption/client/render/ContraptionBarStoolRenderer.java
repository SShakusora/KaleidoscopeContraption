package com.sshakusora.kaleidoscope_contraption.client.render;

import com.github.ysbbbbbb.kaleidoscopetavern.KaleidoscopeTavern;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.BarStoolBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.client.model.deco.BarStoolBodyModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.EnumMap;
import java.util.Map;

/**
 * Client-only renderer for the animated body of Tavern bar stools on Contraptions.
 */
@OnlyIn(Dist.CLIENT)
public final class ContraptionBarStoolRenderer {
    private static final Map<DyeColor, ResourceLocation> TEXTURES = new EnumMap<>(DyeColor.class);
    private static BarStoolBodyModel model;

    private ContraptionBarStoolRenderer() {
    }

    public static void render(MovementContext context, VirtualRenderWorld renderWorld,
                              ContraptionMatrices matrices,
                              MultiBufferSource bufferSource) {
        if (!(context.state.getBlock() instanceof BarStoolBlock barStool)) {
            return;
        }

        AbstractContraptionEntity contraptionEntity = context.contraption.entity;
        if (contraptionEntity == null) {
            return;
        }
        float partialTick = Minecraft.getInstance().getFrameTime();
        float renderRotation = context.state.getValue(BarStoolBlock.FACING).toYRot();
        LivingEntity passenger = findPassenger(context.contraption, contraptionEntity, context.localPos);
        if (passenger != null) {
            renderRotation = getLocalBodyRotation(contraptionEntity, passenger, partialTick);
        }

        PoseStack poseStack = matrices.getModelViewProjection();
        poseStack.pushPose();
        poseStack.translate(context.localPos.getX() + 0.5,
                context.localPos.getY() + 1.5,
                context.localPos.getZ() + 0.5);
        poseStack.mulPose(Axis.ZN.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YN.rotationDegrees(180.0F - renderRotation));

        ResourceLocation texture = TEXTURES.computeIfAbsent(barStool.getColor(), color ->
                KaleidoscopeTavern.modLoc(
                        "textures/entity/deco/bar_stool/%s.png".formatted(color.getName())));
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        getModel().renderToBuffer(poseStack, consumer,
                getPackedLight(context, renderWorld, contraptionEntity, partialTick),
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
    }

    private static BarStoolBodyModel getModel() {
        if (model == null) {
            model = new BarStoolBodyModel(Minecraft.getInstance().getEntityModels()
                    .bakeLayer(BarStoolBodyModel.LAYER_LOCATION));
        }
        return model;
    }

    private static LivingEntity findPassenger(Contraption contraption,
                                              AbstractContraptionEntity contraptionEntity,
                                              BlockPos localPos) {
        int seatIndex = contraption.getSeats().indexOf(localPos);
        if (seatIndex < 0) {
            return null;
        }

        for (Entity passenger : contraptionEntity.getPassengers()) {
            Integer mappedSeat = contraption.getSeatMapping().get(passenger.getUUID());
            if (mappedSeat != null && mappedSeat == seatIndex && passenger instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    private static float getLocalBodyRotation(AbstractContraptionEntity contraptionEntity,
                                              LivingEntity passenger, float partialTick) {
        float worldBodyRotation = Mth.wrapDegrees(
                Mth.lerp(partialTick, passenger.yBodyRotO, passenger.yBodyRot));
        Vec3 worldFacing = Vec3.directionFromRotation(0.0F, worldBodyRotation);
        Vec3 localFacing = contraptionEntity.reverseRotation(worldFacing, partialTick);
        return Mth.wrapDegrees((float) Math.toDegrees(Mth.atan2(-localFacing.x, localFacing.z)));
    }

    private static int getPackedLight(MovementContext context, VirtualRenderWorld renderWorld,
                                      AbstractContraptionEntity contraptionEntity,
                                      float partialTick) {
        Vec3 worldCenter = contraptionEntity.toGlobalVector(Vec3.atCenterOf(context.localPos), partialTick);
        int realLevelLight = LevelRenderer.getLightColor(context.world, BlockPos.containing(worldCenter));
        renderWorld.setExternalLight(realLevelLight);
        try {
            return LevelRenderer.getLightColor(renderWorld, context.localPos);
        } finally {
            renderWorld.resetExternalLight();
        }
    }
}
