package com.sshakusora.kaleidoscope_contraption.network;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.api.placement.ContraptionRemovalManager;
import com.sshakusora.kaleidoscope_contraption.util.DevEnvUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;


/**
 * 客户端发送到服务端的移除方块请求包
 * 当玩家按下移除方块键时发送，服务端验证并执行移除逻辑
 */
public class KCRemoveBlockPacket implements CustomPacketPayload {
    public static final Type<KCRemoveBlockPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("kaleidoscope_contraption", "remove_block"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KCRemoveBlockPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), KCRemoveBlockPacket::decode);

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double MAX_REMOVE_DISTANCE_SQR = 36.0;

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

    public static void handle(KCRemoveBlockPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) {
                LOGGER.warn("[KCRemoveBlockPacket] Player is null");
                return;
            }

            if (DevEnvUtil.isDevEnvironment()) {
                LOGGER.info("[KCRemoveBlockPacket] Handling remove request from player: {}, contraptionId: {}, targetPos: {}",
                        player.getName().getString(), packet.contraptionEntityId, packet.targetPos);
            }

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

            Vec3 targetCenter = contraptionEntity.toGlobalVector(Vec3.atCenterOf(packet.targetPos), 1.0F);
            if (player.getEyePosition().distanceToSqr(targetCenter) > MAX_REMOVE_DISTANCE_SQR
                    || !player.mayInteract(player.level(), BlockPos.containing(targetCenter))
                    || !isTargetedBlock(player, contraptionEntity, packet.targetPos)) {
                LOGGER.warn("[KCRemoveBlockPacket] Rejected invalid remove target from player {}",
                        player.getName().getString());
                return;
            }

            if (DevEnvUtil.isDevEnvironment()) {
                LOGGER.info("[KCRemoveBlockPacket] Removing block {} at {} in contraption {}",
                        blockInfo.state().getBlock().getName().getString(), packet.targetPos, packet.contraptionEntityId);
            }

            ContraptionRemovalManager.Result removalResult = ContraptionRemovalManager.tryRemove(
                    player, packet.targetPos, contraptionEntity);
            if (removalResult != ContraptionRemovalManager.Result.NOT_REGISTERED) {
                return;
            }

            // 旧存档中的方块没有规则ID，回退到原有Interaction移除逻辑
            triggerRemoveInteraction(player, contraptionEntity, packet.targetPos);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    private static boolean isTargetedBlock(ServerPlayer player, AbstractContraptionEntity entity,
                                           BlockPos requestedPos) {
        Vec3 origin = player.getEyePosition();
        Vec3 target = origin.add(player.getViewVector(1.0F).scale(Math.sqrt(MAX_REMOVE_DISTANCE_SQR)));
        Vec3 localOrigin = entity.toLocalVector(origin, 1.0F);
        Vec3 localTarget = entity.toLocalVector(target, 1.0F);
        BlockPos closestPos = null;
        double closestDistance = Double.MAX_VALUE;

        for (var entry : entity.getContraption().getBlocks().entrySet()) {
            BlockPos pos = entry.getKey();
            if (entity.getContraption().isHiddenInPortal(pos)) {
                continue;
            }
            VoxelShape shape = entry.getValue().state().getShape(
                    entity.getContraption().getContraptionWorld(), BlockPos.ZERO.below());
            BlockHitResult hit = shape.clip(localOrigin, localTarget, pos);
            if (hit == null) {
                continue;
            }
            double distance = localOrigin.distanceToSqr(hit.getLocation());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestPos = pos;
            }
        }
        if (!requestedPos.equals(closestPos)) {
            return false;
        }

        BlockHitResult worldHit = player.level().clip(new ClipContext(origin, target,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return worldHit.getType() == HitResult.Type.MISS
                || origin.distanceToSqr(worldHit.getLocation()) + 1.0E-4 >= closestDistance;
    }

    /**
     * 触发方块的移除交互逻辑
     * 通过调用对应InteractionBehaviour的handlePlayerInteraction方法，但传入特殊标记表示这是移除操作
     */
    private static void triggerRemoveInteraction(ServerPlayer player, AbstractContraptionEntity contraptionEntity,
                                                 BlockPos localPos) {
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
