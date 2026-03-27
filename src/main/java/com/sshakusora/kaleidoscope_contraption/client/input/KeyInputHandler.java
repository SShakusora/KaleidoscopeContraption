package com.sshakusora.kaleidoscope_contraption.client.input;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.KaleidoscopeContraption;
import com.sshakusora.kaleidoscope_contraption.client.init.ClientSetupEvent;
import com.sshakusora.kaleidoscope_contraption.network.KCPacketHandler;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端按键输入处理器
 * 当玩家按下移除方块键时，检测是否指向Contraption中的方块，如果是则发送网络包到服务端
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT, modid = KaleidoscopeContraption.MOD_ID)
public class KeyInputHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // 检查是否是移除方块键被按下
        if (ClientSetupEvent.REMOVE_BLOCK_KEY.consumeClick()) {
            handleRemoveBlockKey();
        }
    }

    // 默认交互距离，与玩家普通交互距离一致
    private static final double DEFAULT_REACH_DISTANCE = 4.5;

    private static void handleRemoveBlockKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // 在客户端遍历查找目标Contraption和方块
        TargetResult target = findTargetContraption(mc, mc.player);
        if (target == null) {
            return;
        }

        // 发送网络包到服务端，包含contraptionId和目标方块位置
        KCPacketHandler.INSTANCE.sendToServer(new KCRemoveBlockPacket(target.contraptionId(), target.targetPos()));
    }

    /**
     * 在客户端遍历所有ContraptionEntity，找到玩家指向的目标
     * 参考 ContraptionPotOverlay.findTargetedPotBlock 的实现
     */
    private static TargetResult findTargetContraption(Minecraft mc, LocalPlayer player) {
        // 计算玩家的视线
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getViewVector(1.0f);
        Vec3 endPos = eyePos.add(lookVec.x * DEFAULT_REACH_DISTANCE, lookVec.y * DEFAULT_REACH_DISTANCE, lookVec.z * DEFAULT_REACH_DISTANCE);

        BlockPos targetPos = null;
        AbstractContraptionEntity targetContraption = null;
        double closestDistanceSqr = Double.MAX_VALUE;

        // 使用 entitiesForRendering() 遍历世界中所有的Contraption实体
        for (var entity : mc.level.entitiesForRendering()) {
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
            Vec3 localEyePos = contraptionEntity.toLocalVector(eyePos, 1.0f);
            Vec3 localEndPos = contraptionEntity.toLocalVector(endPos, 1.0f);

            // 在Contraption的本地坐标系中进行射线检测
            BlockPos hitLocalPos = raycastContraptionBlocks(contraptionEntity, localEyePos, localEndPos);
            if (hitLocalPos == null) {
                continue;
            }

            // 计算距离，找到最近的
            Vec3 globalHitPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(hitLocalPos), 1.0f);
            double distanceSqr = eyePos.distanceToSqr(globalHitPos);

            if (distanceSqr < closestDistanceSqr) {
                closestDistanceSqr = distanceSqr;
                targetPos = hitLocalPos;
                targetContraption = contraptionEntity;
            }
        }

        if (targetPos == null || targetContraption == null) {
            return null;
        }

        return new TargetResult(targetContraption.getId(), targetPos);
    }

    /**
     * 在Contraption的本地坐标系中进行射线检测
     */
    private static BlockPos raycastContraptionBlocks(AbstractContraptionEntity contraptionEntity, Vec3 localStart, Vec3 localEnd) {
        var blocks = contraptionEntity.getContraption().getBlocks();
        BlockPos closestPos = null;
        double closestDistSqr = Double.MAX_VALUE;

        for (var entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            AABB blockAABB = new AABB(pos);

            var intersection = blockAABB.clip(localStart, localEnd);
            if (intersection.isPresent()) {
                double distSqr = localStart.distanceToSqr(intersection.get());
                if (distSqr < closestDistSqr) {
                    closestDistSqr = distSqr;
                    closestPos = pos;
                }
            }
        }

        return closestPos;
    }

    /**
     * 记录目标Contraption的信息
     */
    private record TargetResult(int contraptionId, BlockPos targetPos) {}
}
