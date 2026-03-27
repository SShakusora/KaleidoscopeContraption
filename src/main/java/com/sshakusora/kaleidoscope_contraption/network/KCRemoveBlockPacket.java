package com.sshakusora.kaleidoscope_contraption.network;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * 客户端发送到服务端的移除方块请求包
 * 当玩家按下移除方块键时发送，服务端验证并执行移除逻辑
 */
public class KCRemoveBlockPacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final int contraptionEntityId;
    private final BlockPos targetPos;

    public KCRemoveBlockPacket(int contraptionEntityId, BlockPos targetPos) {
        this.contraptionEntityId = contraptionEntityId;
        this.targetPos = targetPos;
    }

    public static void encode(KCRemoveBlockPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.contraptionEntityId);
        buffer.writeBlockPos(packet.targetPos);
    }

    public static KCRemoveBlockPacket decode(FriendlyByteBuf buffer) {
        return new KCRemoveBlockPacket(buffer.readInt(), buffer.readBlockPos());
    }

    public static void handle(KCRemoveBlockPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                LOGGER.warn("[KCRemoveBlockPacket] Player is null");
                return;
            }

            LOGGER.info("[KCRemoveBlockPacket] Handling remove request from player: {}, contraptionId: {}, targetPos: {}",
                    player.getName().getString(), packet.contraptionEntityId, packet.targetPos);

            // 获取Contraption实体（使用客户端上传的ID）
            var entity = player.level().getEntity(packet.contraptionEntityId);
            if (!(entity instanceof AbstractContraptionEntity contraptionEntity)) {
                LOGGER.warn("[KCRemoveBlockPacket] Contraption entity {} not found or not a contraption", packet.contraptionEntityId);
                return;
            }

            // 获取该位置的方块信息
            StructureTemplate.StructureBlockInfo blockInfo = contraptionEntity.getContraption().getBlocks().get(packet.targetPos);
            if (blockInfo == null) {
                LOGGER.warn("[KCRemoveBlockPacket] Block info is null at {}", packet.targetPos);
                return;
            }

            LOGGER.info("[KCRemoveBlockPacket] Removing block {} at {} in contraption {}",
                    blockInfo.state().getBlock().getName().getString(), packet.targetPos, packet.contraptionEntityId);

            // 触发对应方块的移除逻辑
            triggerRemoveInteraction(player, contraptionEntity, packet.targetPos, blockInfo);
        });
        context.setPacketHandled(true);
    }

    /**
     * 查找玩家视线指向的Contraption中的方块位置
     * 参考 ContraptionPotOverlay.raycastContraptionPotBlocks 的实现
     */
    private static BlockPos findTargetBlock(AbstractContraptionEntity contraptionEntity, Vec3 localStart, Vec3 localEnd) {
        var blocks = contraptionEntity.getContraption().getBlocks();
        BlockPos closestPos = null;
        double closestDistSqr = Double.MAX_VALUE;

        // 遍历Contraption中的所有方块，找到与射线相交的
        for (var entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            AABB blockAABB = new AABB(pos);

            // 检测射线是否与方块相交
            var intersection = blockAABB.clip(localStart, localEnd);
            if (intersection.isPresent()) {
                // 使用 distanceToSqr 避免开方运算，性能更好
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
     * 触发方块的移除交互逻辑
     * 通过调用对应InteractionBehaviour的handlePlayerInteraction方法，但传入特殊标记表示这是移除操作
     */
    private static void triggerRemoveInteraction(ServerPlayer player, AbstractContraptionEntity contraptionEntity,
                                                  BlockPos localPos, StructureTemplate.StructureBlockInfo blockInfo) {
        // 获取该位置的交互行为
        var interactionBehaviour = contraptionEntity.getContraption().getInteractors().get(localPos);
        if (interactionBehaviour == null) {
            return;
        }

        // 使用一个特殊的静态标志来标记这是移除操作
        // 在Interaction类中会检查这个标志
        KCRemoveBlockHandler.setRemoveKeyPressed(player.getUUID(), true);
        try {
            interactionBehaviour.handlePlayerInteraction(player, player.getUsedItemHand(), localPos, contraptionEntity);
        } finally {
            KCRemoveBlockHandler.setRemoveKeyPressed(player.getUUID(), false);
        }
    }
}
