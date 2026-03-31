package com.sshakusora.kaleidoscope_contraption.client.gui.overlay;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.PotBlock;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.lang.ref.WeakReference;
import java.util.Collection;
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
        if (!hasOil || !ContraptionInteractionUtil.hasHeatSource(potTarget.contraptionEntity(), potTarget.localPos())) {
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
     * 参考 Create 的 rightClickingOnContraptionsGetsHandledLocally 实现
     */
    private Optional<PotBlockTarget> findTargetedPotBlock(Minecraft minecraft, Vec3 eyePos, Vec3 endPos, float partialTick) {
        if (minecraft.level == null) {
            return Optional.empty();
        }

        // 创建射线包围盒，与 Create 保持一致
        AABB aabb = new AABB(eyePos, endPos).inflate(16);

        // 使用 ContraptionHandler.loadedContraptions 获取已加载的 Contraption，更高效
        Collection<WeakReference<AbstractContraptionEntity>> contraptions =
            ContraptionHandler.loadedContraptions.get(minecraft.level).values();

        PotBlockTarget closestTarget = null;
        double closestDistance = Double.MAX_VALUE;

        for (WeakReference<AbstractContraptionEntity> ref : contraptions) {
            AbstractContraptionEntity contraptionEntity = ref.get();
            if (contraptionEntity == null) {
                continue;
            }

            // 检查包围盒是否相交，与 Create 保持一致
            if (!contraptionEntity.getBoundingBox().intersects(aabb)) {
                continue;
            }

            // 使用 Create 的 rayTraceContraption 进行精确射线检测
            BlockHitResult hitResult = ContraptionHandlerClient.rayTraceContraption(eyePos, endPos, contraptionEntity);
            if (hitResult == null) {
                continue;
            }

            BlockPos hitLocalPos = hitResult.getBlockPos();
            StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(hitLocalPos);

            // 只处理 PotBlock
            if (info == null || !(info.state().getBlock() instanceof PotBlock)) {
                continue;
            }

            // 计算距离，与 Create 保持一致
            double distance = contraptionEntity.toGlobalVector(hitResult.getLocation(), 1).distanceTo(eyePos);
            if (distance > closestDistance) {
                continue;
            }

            closestDistance = distance;
            closestTarget = new PotBlockTarget(contraptionEntity, hitLocalPos, info);
        }

        return Optional.ofNullable(closestTarget);
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
