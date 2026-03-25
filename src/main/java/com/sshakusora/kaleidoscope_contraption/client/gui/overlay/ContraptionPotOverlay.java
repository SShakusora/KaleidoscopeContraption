package com.sshakusora.kaleidoscope_contraption.client.gui.overlay;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.PotBlock;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.PotBlockMovingInteraction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.Optional;

public class ContraptionPotOverlay implements IGuiOverlay {

    private static final int PUT_INGREDIENT = 0;
    private static final int COOKING = 1;
    private static final int FINISHED = 2;
    private static final int BURNT = 3;

    private static final String STATUS = "Status";

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = gui.getMinecraft();
        if (minecraft.gameMode == null || minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return;
        }

        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        // 计算玩家的视线
        Vec3 eyePosition = player.getEyePosition(partialTick);
        Vec3 lookVector = player.getViewVector(partialTick);
        double reachDistance = minecraft.gameMode.getPickRange();
        Vec3 endPosition = eyePosition.add(lookVector.x * reachDistance, lookVector.y * reachDistance, lookVector.z * reachDistance);

        // 查找玩家注视的Contraption中的PotBlock
        Optional<PotBlockTarget> target = findTargetedPotBlock(minecraft, eyePosition, endPosition, partialTick);
        if (target.isEmpty()) {
            return;
        }

        PotBlockTarget potTarget = target.get();
        BlockState state = potTarget.blockInfo().state();
        CompoundTag nbt = potTarget.blockInfo().nbt();

        if (nbt == null) {
            return;
        }

        // 检查是否有油以及下方是否有热源
        boolean hasOil = state.getValue(PotBlock.HAS_OIL);
        if (!hasOil || !PotBlockMovingInteraction.hasHeatSource(potTarget.contraptionEntity(), potTarget.localPos())) {
            return;
        }

        // 获取状态
        int status = nbt.getInt(STATUS);

        // 渲染提示信息
        Font font = Minecraft.getInstance().font;
        int x = screenWidth / 2;
        int y = screenHeight - 72;
        // 检查是否有覆盖消息显示
        if (minecraft.gui.overlayMessageTime > 0) {
            y = y - 12;
        }

        if (status == PUT_INGREDIENT) {
            drawWordWrap(guiGraphics, font, Component.translatable("tip.kaleidoscope_cookery.pot.add_ingredient"), x, y, 0xFFFFFF);
        } else if (status == COOKING) {
            drawWordWrap(guiGraphics, font, Component.translatable("tip.kaleidoscope_cookery.pot.need_stir_fry"), x, y, 0xFFFFFF);
        } else if (status == FINISHED) {
            drawWordWrap(guiGraphics, font, Component.translatable("tip.kaleidoscope_cookery.pot.done"), x, y, ChatFormatting.RED.getColor());
        }
    }

    /**
     * 查找玩家视线范围内的Contraption PotBlock
     */
    private Optional<PotBlockTarget> findTargetedPotBlock(Minecraft minecraft, Vec3 eyePos, Vec3 endPos, float partialTick) {
        if (minecraft.level == null) {
            return Optional.empty();
        }

        PotBlockTarget closestTarget = null;
        double closestDistance = Double.MAX_VALUE;

        // 遍历世界中所有的Contraption实体
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof AbstractContraptionEntity contraptionEntity)) {
                continue;
            }

            // 检查视线是否与Contraption的包围盒相交
            AABB contraptionBounds = contraptionEntity.getBoundingBox();
            if (contraptionBounds == null) {
                continue;
            }

            // 缩小包围盒以符合视觉范围
            AABB expandedBounds = contraptionBounds.inflate(-0.8);
            if (expandedBounds.clip(eyePos, endPos).isEmpty()) {
                continue;
            }

            // 将视线转换到Contraption的本地坐标系
            Vec3 localEyePos = contraptionEntity.toLocalVector(eyePos, partialTick);
            Vec3 localEndPos = contraptionEntity.toLocalVector(endPos, partialTick);

            // 在Contraption的本地坐标系中进行射线检测
            BlockPos hitLocalPos = raycastContraptionPotBlocks(contraptionEntity, localEyePos, localEndPos);
            if (hitLocalPos == null) {
                continue;
            }

            // 计算距离
            Vec3 globalHitPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(hitLocalPos), partialTick);
            double distance = eyePos.distanceToSqr(globalHitPos);

            if (distance < closestDistance) {
                closestDistance = distance;
                StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(hitLocalPos);
                closestTarget = new PotBlockTarget(contraptionEntity, hitLocalPos, info);
            }
        }

        return Optional.ofNullable(closestTarget);
    }

    /**
     * 在Contraption的本地坐标系中进行射线检测，返回命中的PotBlock位置
     */
    private BlockPos raycastContraptionPotBlocks(AbstractContraptionEntity contraptionEntity, Vec3 localStart, Vec3 localEnd) {
        var blocks = contraptionEntity.getContraption().getBlocks();
        BlockPos closestPos = null;
        double closestDistance = Double.MAX_VALUE;

        // 遍历Contraption中的所有方块进行射线检测
        for (var entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            StructureTemplate.StructureBlockInfo info = entry.getValue();

            // 只检测PotBlock
            if (!(info.state().getBlock() instanceof PotBlock)) {
                continue;
            }

            // 创建方块的AABB
            AABB blockAABB = new AABB(pos);

            // 检测射线是否与方块相交
            var intersection = blockAABB.clip(localStart, localEnd);
            if (intersection.isPresent()) {
                double distance = localStart.distanceToSqr(intersection.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPos = pos;
                }
            }
        }

        return closestPos;
    }

    private static void drawWordWrap(GuiGraphics graphics, Font font, MutableComponent text, int pX, int pY, int color) {
        for (FormattedCharSequence sequence : font.split(text, 100)) {
            graphics.drawString(font, sequence, pX - font.width(sequence) / 2, pY, color);
            pY += font.lineHeight;
        }
    }

    /**
     * 记录目标PotBlock的信息
     */
    private record PotBlockTarget(AbstractContraptionEntity contraptionEntity, BlockPos localPos, StructureTemplate.StructureBlockInfo blockInfo) {}
}
