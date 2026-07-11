package com.sshakusora.kaleidoscope_contraption.client.render;

import com.github.ysbbbbbb.kaleidoscopecookery.api.blockentity.ITeapot;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.serializer.TeapotRecipeSerializer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
import com.sshakusora.kaleidoscope_contraption.KaleidoscopeContraption;
import net.createmod.catnip.data.Couple;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = KaleidoscopeContraption.MOD_ID, value = Dist.CLIENT)
public final class ContraptionTeapotTextRenderer {
    private static final Function<ResourceLocation, Component> FLUID_NAME_CACHE = Util.memoize(id -> {
        if (id.equals(TeapotRecipeSerializer.EMPTY_TEA_FLUID)) {
            return Component.translatable("mco.configure.world.slot.empty");
        }
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(id);
        return fluid == null
                ? Component.literal(id.toString())
                : Component.translatable(fluid.getFluidType().getDescriptionId());
    });

    private ContraptionTeapotTextRenderer() {
    }

    @SubscribeEvent
    public static void renderTeapotText(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null || minecraft.gameMode == null) {
            return;
        }

        Target target = findTarget(level, player, event.getPartialTick());
        if (target == null) {
            return;
        }

        Vec3 textPosition = target.entity().toGlobalVector(
                Vec3.atLowerCornerWithOffset(target.teapot().getBlockPos(), 0.5, 1.0, 0.5),
                event.getPartialTick());
        Vec3 cameraPosition = event.getCamera().getPosition();

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(
                textPosition.x - cameraPosition.x,
                textPosition.y - cameraPosition.y,
                textPosition.z - cameraPosition.z);
        poseStack.mulPose(Axis.YN.rotationDegrees(180.0F + event.getCamera().getYRot()));
        poseStack.scale(0.015625F, -0.015625F, 0.015625F);

        try {
            int packedLight = LevelRenderer.getLightColor(level, BlockPos.containing(textPosition));
            MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
            renderText(target.teapot(), poseStack, buffers, minecraft.font, packedLight);
            buffers.endBatch();
        } finally {
            poseStack.popPose();
        }
    }

    private static Target findTarget(ClientLevel level, LocalPlayer player, float partialTick) {
        Couple<Vec3> ray = ContraptionHandlerClient.getRayInputs(player);
        Vec3 origin = ray.getFirst();
        Vec3 target = ray.getSecond();
        Target closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (WeakReference<AbstractContraptionEntity> reference
                : ContraptionHandler.loadedContraptions.get(level).values()) {
            AbstractContraptionEntity entity = reference.get();
            if (entity == null) {
                continue;
            }
            BlockHitResult hit = ContraptionHandlerClient.rayTraceContraption(origin, target, entity);
            if (hit == null) {
                continue;
            }

            double distance = entity.toGlobalVector(hit.getLocation(), partialTick).distanceToSqr(origin);
            if (distance >= closestDistance) {
                continue;
            }

            closestDistance = distance;
            Contraption contraption = entity.getContraption();
            if (contraption == null
                    || !(contraption.getBlockEntityClientSide(hit.getBlockPos()) instanceof TeapotBlockEntity teapot)) {
                closest = null;
                continue;
            }
            closest = new Target(entity, teapot);
        }
        return closest;
    }

    private static void renderText(TeapotBlockEntity teapot, PoseStack poseStack,
                                   MultiBufferSource buffers, Font font, int packedLight) {
        drawCentered(font, teapot.getStatusText(), -5, poseStack, buffers, packedLight);

        int status = teapot.getStatus();
        if (status == ITeapot.PUT_INGREDIENT) {
            Component fluidText = FLUID_NAME_CACHE.apply(teapot.getTeaFluidId());
            ItemStack input = teapot.getInput();
            int count = input.getCount();
            Component itemText = input.isEmpty()
                    ? Component.translatable("mco.configure.world.slot.empty")
                    : ComponentUtils.formatList(Arrays.asList(
                    input.getHoverName(), Component.literal("x%d".formatted(count))), CommonComponents.space());
            Component info = Component.translatable(
                    "tooltip.kaleidoscope_cookery.teapot.statue.fluid_ingredient",
                    fluidText, itemText, count);
            drawCentered(font, info, 5, poseStack, buffers, packedLight);
        } else if (status == ITeapot.FINISHED) {
            ItemStack result = teapot.getResult();
            int count = result.getCount();
            Component itemText = result.isEmpty()
                    ? Component.translatable("mco.configure.world.slot.empty")
                    : ComponentUtils.formatList(Arrays.asList(
                    result.getHoverName(), Component.literal("x%d".formatted(count))), CommonComponents.space());
            Component info = Component.translatable(
                    "tooltip.kaleidoscope_cookery.teapot.statue.result", itemText, count);
            drawCentered(font, info, 5, poseStack, buffers, packedLight);
        }
    }

    private static void drawCentered(Font font, Component text, float y, PoseStack poseStack,
                                     MultiBufferSource buffers, int packedLight) {
        float x = -font.width(text) / 2.0F + 0.5F;
        font.drawInBatch(text, x, y, 0xFFFFFF, false, poseStack.last().pose(),
                buffers, Font.DisplayMode.POLYGON_OFFSET, 0, packedLight);
    }

    private record Target(AbstractContraptionEntity entity, TeapotBlockEntity teapot) {
    }
}
