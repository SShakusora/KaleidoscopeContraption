package com.sshakusora.kaleidoscope_contraption.network;

import com.mojang.logging.LogUtils;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.mixin.accessor.ContraptionAccessor;
import com.sshakusora.kaleidoscope_contraption.util.DevEnvUtil;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.lang3.tuple.MutablePair;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class KCContraptionChangedPacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final int entityId;
    private final BlockPos localPos;
    private final BlockState newState;
    private final CompoundTag newNbt;
    private final AABB updatedBounds;

    public KCContraptionChangedPacket(int entityId, BlockPos localPos, BlockState newState, CompoundTag newNbt, AABB updatedBounds) {
        this.entityId = entityId;
        this.localPos = localPos;
        this.newState = newState;
        this.newNbt = newNbt;
        this.updatedBounds = updatedBounds;
    }

    public KCContraptionChangedPacket(int entityId, BlockPos localPos, BlockState newState, CompoundTag newNbt) {
        this(entityId, localPos, newState, newNbt, null);
    }

    public static void encode(KCContraptionChangedPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId);
        buffer.writeBlockPos(packet.localPos);
        buffer.writeNbt(NbtUtils.writeBlockState(packet.newState));
        buffer.writeNbt(packet.newNbt);
        buffer.writeBoolean(packet.updatedBounds != null);
        if (packet.updatedBounds != null) {
            CompoundTag boundsWrapper = new CompoundTag();
            boundsWrapper.put("Bounds", NBTHelper.writeAABB(packet.updatedBounds));
            buffer.writeNbt(boundsWrapper);
        }
    }

    public static KCContraptionChangedPacket decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        BlockPos localPos = buffer.readBlockPos();
        CompoundTag stateTag = buffer.readNbt();
        BlockState newState = NbtUtils.readBlockState(
                BuiltInRegistries.BLOCK.asLookup(), stateTag);
        CompoundTag newNbt = buffer.readNbt();
        AABB updatedBounds = null;
        if (buffer.readBoolean()) {
            CompoundTag boundsWrapper = buffer.readNbt();
            if (boundsWrapper != null && boundsWrapper.contains("Bounds", Tag.TAG_LIST)) {
                ListTag boundsTag = boundsWrapper.getList("Bounds", Tag.TAG_FLOAT);
                updatedBounds = NBTHelper.readAABB(boundsTag);
            }
        }
        return new KCContraptionChangedPacket(entityId, localPos, newState, newNbt, updatedBounds);
    }

    public static void handle(KCContraptionChangedPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                handleClient(packet);
            }
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(KCContraptionChangedPacket packet) {
        if (Minecraft.getInstance().level == null) return;

        if (Minecraft.getInstance().level.getEntity(packet.entityId) instanceof AbstractContraptionEntity contraptionEntity) {
            if (contraptionEntity.getContraption() == null) {
                LOGGER.warn("[KCContraption] Contraption is null for entity {}", packet.entityId);
                return;
            }

            if (DevEnvUtil.isDevEnvironment()) {
                LOGGER.info("[KCContraption] Handling NBT update for entity {} at pos {}. NBT: {}",
                        packet.entityId, packet.localPos, packet.newNbt);
            }

            // 检查是否是删除方块操作（空气状态表示删除）
            boolean isBlockRemoved = packet.newState.isAir();

            // 检查这个位置是否已经有方块（用于判断是更新还是新增）
            var existingInfo = contraptionEntity.getContraption().getBlocks().get(packet.localPos);
            boolean isNewBlock = (existingInfo == null) || existingInfo.state().isAir();

            if (isBlockRemoved) {
                // 删除方块：从客户端blocks中移除
                contraptionEntity.getContraption().getBlocks().remove(packet.localPos);
                contraptionEntity.getContraption().getInteractors().remove(packet.localPos);
                contraptionEntity.getContraption().getActors().removeIf(actor -> actor.getLeft().pos().equals(packet.localPos));
                ((ContraptionAccessor) contraptionEntity.getContraption()).getUpdateTags().remove(packet.localPos);
                if (DevEnvUtil.isDevEnvironment()) {
                    LOGGER.info("[KCContraption] Block removed at {}", packet.localPos);
                }
            } else {
                // 更新方块数据（包含NBT）
                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        packet.localPos, packet.newState, packet.newNbt);
                contraptionEntity.getContraption().getBlocks().put(packet.localPos, newInfo);

                // 更新interactors
                MovingInteractionBehaviour interactionBehaviour = MovingInteractionBehaviour.REGISTRY.get(packet.newState);
                if (interactionBehaviour != null) {
                    contraptionEntity.getContraption().getInteractors().put(packet.localPos, interactionBehaviour);
                    if (DevEnvUtil.isDevEnvironment()) {
                        LOGGER.info("[KCContraption] Registered interactor for new block at {}", packet.localPos);
                    }
                }

                // 更新actors列表 - 根据Contraption重建原理，需要同步更新
                MovementBehaviour movementBehaviour = MovementBehaviour.REGISTRY.get(packet.newState);
                if (movementBehaviour != null) {
                    var actors = contraptionEntity.getContraption().getActors();
                    // 检查是否已存在该位置的actor
                    boolean exists = false;
                    for (var actor : actors) {
                        if (actor.getLeft().pos().equals(packet.localPos)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        // 创建新的MovementContext
                        MovementContext context = new MovementContext(
                                Minecraft.getInstance().level, newInfo, contraptionEntity.getContraption());
                        actors.add(MutablePair.of(newInfo, context));
                        if (DevEnvUtil.isDevEnvironment()) {
                            LOGGER.info("[KCContraption] Registered actor for new block at {}", packet.localPos);
                        }
                    }
                }

                // 更新actor数据
                var actors = contraptionEntity.getContraption().getActors();
                if (DevEnvUtil.isDevEnvironment()) {
                    LOGGER.info("[KCContraption] Actor count: {}", actors.size());
                }
                for (int i = 0; i < actors.size(); i++) {
                    var actor = actors.get(i);
                    if (actor.getLeft().pos().equals(packet.localPos)) {
                        actor.setLeft(newInfo);
                        if (DevEnvUtil.isDevEnvironment()) {
                            LOGGER.info("[KCContraption] Updated actor at index {}", i);
                        }
                        break;
                    }
                }
            }

            // 更新方块实体渲染
            contraptionEntity.getContraption().resetClientContraption();

            // 更新bounds - 优先使用服务端同步的bounds
            boolean boundsUpdated = false;
            if (packet.updatedBounds != null) {
                contraptionEntity.getContraption().bounds = packet.updatedBounds;
                boundsUpdated = true;
                if (DevEnvUtil.isDevEnvironment()) {
                    LOGGER.info("[KCContraption] Bounds updated from server: {}", packet.updatedBounds);
                }
            } else if (!isBlockRemoved && isNewBlock) {
                contraptionEntity.getContraption().bounds = contraptionEntity.getContraption().bounds.minmax(new AABB(packet.localPos));
                boundsUpdated = true;
                if (DevEnvUtil.isDevEnvironment()) {
                    LOGGER.info("[KCContraption] New block detected at {}, updated bounds", packet.localPos);
                }
            }

            // 如果bounds更新了，需要刷新实体的碰撞箱
            if (boundsUpdated) {
                contraptionEntity.setPos(contraptionEntity.getX(), contraptionEntity.getY(), contraptionEntity.getZ());
                if (DevEnvUtil.isDevEnvironment()) {
                    LOGGER.info("[KCContraption] Refreshed entity bounding box");
                }
            }

            // 根据情况选择不同的刷新策略
            if (isBlockRemoved) {
                // 删除方块：使结构失效，重建主网格（而不仅仅是子元素）
                contraptionEntity.getContraption().invalidateClientContraptionStructure();
                if (DevEnvUtil.isDevEnvironment()) {
                    LOGGER.info("[KCContraption] Block removed, invalidated structure");
                }
            } else if (isNewBlock) {
                // 新添加的方块：需要完全重置ClientContraption
                contraptionEntity.getContraption().resetClientContraption();
            } else {
                // 现有方块的更新：检查BlockState是否发生变化
                boolean blockStateChanged = existingInfo == null || !existingInfo.state().equals(packet.newState);

                if (blockStateChanged) {
                    // BlockState发生变化：需要重建结构以更新渲染
                    if (DevEnvUtil.isDevEnvironment()) {
                        LOGGER.info("[KCContraption] BlockState changed at pos {}, invalidating structure", packet.localPos);
                    }
                    contraptionEntity.getContraption().invalidateClientContraptionStructure();

                    // 同时更新ClientContraption中的BlockEntity的BlockState
                    var clientContraption = contraptionEntity.getContraption().getOrCreateClientContraptionLazy();
                    var blockEntity = clientContraption.getBlockEntity(packet.localPos);
                    if (blockEntity != null) {
                        if (DevEnvUtil.isDevEnvironment()) {
                            LOGGER.info("[KCContraption] Updating BlockState for BlockEntity at pos {}: old={}, new={}",
                                    packet.localPos, blockEntity.getBlockState(), packet.newState);
                        }
                        // 更新BlockEntity的BlockState
                        blockEntity.setBlockState(packet.newState);
                        // 同时更新NBT
                        if (packet.newNbt != null) {
                            blockEntity.load(packet.newNbt);
                        }
                    }
                } else {
                    // 只有NBT变化：更新BlockEntity数据并刷新视觉
                    var clientContraption = contraptionEntity.getContraption().getOrCreateClientContraptionLazy();
                    var blockEntity = clientContraption.getBlockEntity(packet.localPos);
                    if (blockEntity != null) {
                        if (DevEnvUtil.isDevEnvironment()) {
                            LOGGER.info("[KCContraption] Found BlockEntity at pos {}: {}", packet.localPos, blockEntity.getClass().getSimpleName());
                        }
                        if (packet.newNbt != null) {
                            // 直接调用load方法来加载NBT数据
                            blockEntity.load(packet.newNbt);
                            if (DevEnvUtil.isDevEnvironment()) {
                                LOGGER.info("[KCContraption] Loaded NBT into BlockEntity: {}", packet.newNbt);
                            }
                        }
                    } else if (DevEnvUtil.isDevEnvironment()) {
                        LOGGER.warn("[KCContraption] No BlockEntity found at pos {}", packet.localPos);
                    }

                    // 触发客户端渲染更新
                    contraptionEntity.getContraption().invalidateClientContraptionChildren();
                    if (DevEnvUtil.isDevEnvironment()) {
                        LOGGER.info("[KCContraption] Called invalidateClientContraptionChildren()");
                    }
                }
            }
        } else {
            LOGGER.warn("[KCContraption] Could not find contraption entity with ID {}", packet.entityId);
        }
    }
}
