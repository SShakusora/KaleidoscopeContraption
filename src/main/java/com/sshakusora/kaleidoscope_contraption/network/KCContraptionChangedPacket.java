package com.sshakusora.kaleidoscope_contraption.network;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.ChoppingBoardBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.PotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.misc.TrashCanBlockEntity;
import com.mojang.logging.LogUtils;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ClientContraption;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import com.sshakusora.kaleidoscope_contraption.mixin.accessor.ClientContraptionAccessor;
import com.sshakusora.kaleidoscope_contraption.mixin.accessor.ContraptionClientAccessor;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class KCContraptionChangedPacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final int entityId;
    private final BlockPos localPos;
    private final BlockState newState;
    private final CompoundTag newNbt;
    private final AABB updatedBounds;
    private final boolean needResetRender;

    public KCContraptionChangedPacket(int entityId, BlockPos localPos, BlockState newState, CompoundTag newNbt, AABB updatedBounds, Boolean needResetRender) {
        this.entityId = entityId;
        this.localPos = localPos;
        this.newState = newState;
        this.newNbt = newNbt;
        this.updatedBounds = updatedBounds;
        this.needResetRender = needResetRender;
    }

    public KCContraptionChangedPacket(int entityId, BlockPos localPos, BlockState newState, CompoundTag newNbt, AABB updatedBounds) {
        this(entityId, localPos, newState, newNbt, updatedBounds, false);
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
        buffer.writeBoolean(packet.needResetRender);
    }

    public static KCContraptionChangedPacket decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        BlockPos localPos = buffer.readBlockPos();
        CompoundTag stateTag = buffer.readNbt();
        BlockState newState = NbtUtils.readBlockState(
                BuiltInRegistries.BLOCK.asLookup(), Objects.requireNonNull(stateTag, "Missing block state"));
        CompoundTag newNbt = buffer.readNbt();
        AABB updatedBounds = null;
        if (buffer.readBoolean()) {
            CompoundTag boundsWrapper = buffer.readNbt();
            if (boundsWrapper != null && boundsWrapper.contains("Bounds", Tag.TAG_LIST)) {
                ListTag boundsTag = boundsWrapper.getList("Bounds", Tag.TAG_FLOAT);
                updatedBounds = NBTHelper.readAABB(boundsTag);
            }
        }
        boolean needResetRender = buffer.readBoolean();
        return new KCContraptionChangedPacket(entityId, localPos, newState, newNbt, updatedBounds, needResetRender);
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
            boolean blockStateChanged = existingInfo == null || !existingInfo.state().equals(packet.newState);
            MovementContext previousActorContext = findActorContext(
                    contraptionEntity.getContraption(), packet.localPos);

            if (isBlockRemoved) {
                ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, packet.localPos);
                if (DevEnvUtil.isDevEnvironment()) {
                    LOGGER.info("[KCContraption] Block removed at {}", packet.localPos);
                }
            } else {
                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                        packet.localPos, packet.newState, packet.newNbt);
                ContraptionInteractionUtil.updateContraptionDataLocally(
                        contraptionEntity, packet.localPos, newInfo);
            }

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

            if (blockStateChanged || isBlockRemoved) {
                contraptionEntity.getContraption().invalidateColliders();
            }

            MovementContext updatedActorContext = findActorContext(
                    contraptionEntity.getContraption(), packet.localPos);
            updateClientRenderData(contraptionEntity.getContraption(), packet.localPos, existingInfo,
                    isBlockRemoved ? null : contraptionEntity.getContraption().getBlocks().get(packet.localPos),
                    packet.needResetRender, previousActorContext != updatedActorContext);
        } else {
            LOGGER.warn("[KCContraption] Could not find contraption entity with ID {}", packet.entityId);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void updateClientRenderData(Contraption contraption, BlockPos localPos,
                                               StructureTemplate.StructureBlockInfo oldInfo,
                                               StructureTemplate.StructureBlockInfo newInfo,
                                               boolean forceReset,
                                               boolean actorContextChanged) {
        AtomicReference<ClientContraption> reference =
                ((ContraptionClientAccessor) contraption).getClientContraptionReference();
        ClientContraption clientContraption = reference.getAcquire();
        if (clientContraption == null) {
            // The lazy ClientContraption will be built from the already-updated
            // blocks and bounds when rendering starts.
            return;
        }

        // VirtualRenderWorld fixes its vertical range in the constructor. Create's
        // resetRenderLevel() reuses that world, so only a real replacement can make
        // a newly-added block in another Y section visible.
        if (newInfo != null && clientContraption.getRenderLevel().isOutsideBuildHeight(localPos)) {
            replaceClientContraptionForExpandedBounds(contraption, reference, localPos);
            return;
        }

        if (forceReset) {
            resetClientContraptionPreservingTransientState(contraption, clientContraption);
            return;
        }

        boolean stateChanged = oldInfo == null || newInfo == null
                || !oldInfo.state().equals(newInfo.state());
        VirtualRenderWorld renderLevel = clientContraption.getRenderLevel();
        BlockEntity liveBlockEntity = clientContraption.getBlockEntity(localPos);
        BlockEntityRenderInfo desiredRenderInfo = getBlockEntityRenderInfo(localPos,
                newInfo == null ? null : newInfo.state());

        boolean sameBlock = oldInfo != null && newInfo != null
                && oldInfo.state().getBlock() == newInfo.state().getBlock();
        boolean compatibleBlockEntity = liveBlockEntity != null && desiredRenderInfo != null
                && liveBlockEntity.getType() == desiredRenderInfo.type()
                && liveBlockEntity.getClass() == desiredRenderInfo.blockEntityClass()
                && sameBlock;
        boolean hasLiveBlockEntity = liveBlockEntity != null;
        boolean needsBlockEntity = desiredRenderInfo != null;
        boolean replaceBlockEntity = hasLiveBlockEntity != needsBlockEntity;
        if (hasLiveBlockEntity && needsBlockEntity) {
            replaceBlockEntity = !compatibleBlockEntity || newInfo.nbt() == null;
        }

        BlockState renderState = newInfo == null ? Blocks.AIR.defaultBlockState() : newInfo.state();
        if (replaceBlockEntity && liveBlockEntity != null) {
            renderLevel.removeBlockEntity(localPos);
        }
        if (stateChanged) {
            renderLevel.setBlock(localPos, renderState, 0);
        }

        BlockEntity updatedBlockEntity = liveBlockEntity;
        if (replaceBlockEntity) {
            if (liveBlockEntity != null) {
                liveBlockEntity.setRemoved();
            }
            updatedBlockEntity = null;
            if (needsBlockEntity) {
                updatedBlockEntity = clientContraption.readBlockEntity(renderLevel, newInfo, false);
                if (updatedBlockEntity != null) {
                    renderLevel.setBlockEntity(updatedBlockEntity);
                }
            }
        } else if (updatedBlockEntity != null) {
            if (stateChanged) {
                updatedBlockEntity.setBlockState(renderState);
            }
            prepareTransientRenderState(updatedBlockEntity, oldInfo, newInfo);
            updatedBlockEntity.handleUpdateTag(Objects.requireNonNull(newInfo.nbt()).copy());
        }

        boolean shouldRenderBlockEntity = updatedBlockEntity != null && desiredRenderInfo.rendered();
        boolean renderDataChanged = stateChanged
                || !Objects.equals(oldInfo == null ? null : oldInfo.nbt(),
                newInfo == null ? null : newInfo.nbt());
        boolean renderedMembershipChanged = syncRenderedBlockEntity(
                clientContraption, localPos, liveBlockEntity, updatedBlockEntity,
                shouldRenderBlockEntity, replaceBlockEntity, renderDataChanged);

        if (stateChanged) {
            renderLevel.runLightEngine();
        }

        clientContraption.invalidateStructure();
        MovementBehaviour oldMovement = oldInfo == null ? null : MovementBehaviour.REGISTRY.get(oldInfo.state());
        MovementBehaviour newMovement = newInfo == null ? null : MovementBehaviour.REGISTRY.get(newInfo.state());
        boolean hasMovementVisual = oldMovement != null || newMovement != null;
        boolean actorVisualChanged = actorContextChanged || oldMovement != newMovement
                || (stateChanged && hasMovementVisual);
        boolean blockEntityStateChanged = stateChanged
                && (hasLiveBlockEntity || updatedBlockEntity != null);
        if (replaceBlockEntity || renderedMembershipChanged
                || blockEntityStateChanged
                || actorVisualChanged) {
            clientContraption.invalidateChildren();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static boolean syncRenderedBlockEntity(ClientContraption clientContraption, BlockPos localPos,
                                                   BlockEntity oldBlockEntity, BlockEntity newBlockEntity,
                                                   boolean shouldRender, boolean replaced,
                                                   boolean renderDataChanged) {
        List<BlockEntity> renderedBlockEntities =
                ((ClientContraptionAccessor) clientContraption).getRenderedBlockEntities();
        int index = oldBlockEntity == null ? -1 : renderedBlockEntities.indexOf(oldBlockEntity);
        if (index < 0) {
            for (int i = 0; i < renderedBlockEntities.size(); i++) {
                if (renderedBlockEntities.get(i).getBlockPos().equals(localPos)) {
                    index = i;
                    break;
                }
            }
        }

        if (shouldRender) {
            if (index >= 0) {
                boolean instanceChanged = renderedBlockEntities.get(index) != newBlockEntity;
                if (instanceChanged) {
                    renderedBlockEntities.set(index, newBlockEntity);
                }
                if (replaced || instanceChanged || renderDataChanged) {
                    clientContraption.shouldRenderBlockEntities.set(index);
                    clientContraption.scratchErroredBlockEntities.clear();
                }
                return instanceChanged;
            }

            int newIndex = renderedBlockEntities.size();
            renderedBlockEntities.add(newBlockEntity);
            clientContraption.shouldRenderBlockEntities.set(newIndex);
            clientContraption.scratchErroredBlockEntities.clear();
            return true;
        }

        if (index < 0) {
            return false;
        }

        int previousSize = renderedBlockEntities.size();
        renderedBlockEntities.remove(index);
        shiftBitSetLeft(clientContraption.shouldRenderBlockEntities, index, previousSize);
        clientContraption.scratchErroredBlockEntities.clear();
        return true;
    }

    private static void shiftBitSetLeft(BitSet bits, int removedIndex, int previousSize) {
        for (int i = removedIndex; i < previousSize - 1; i++) {
            bits.set(i, bits.get(i + 1));
        }
        bits.clear(previousSize - 1);
    }

    private static MovementContext findActorContext(Contraption contraption, BlockPos localPos) {
        for (var actor : contraption.getActors()) {
            if (actor.getLeft().pos().equals(localPos)) {
                return actor.getRight();
            }
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    private static void replaceClientContraptionForExpandedBounds(Contraption contraption,
                                                                  AtomicReference<ClientContraption> reference,
                                                                  BlockPos requiredPos) {
        while (true) {
            ClientContraption current = reference.getAcquire();
            if (current == null || !current.getRenderLevel().isOutsideBuildHeight(requiredPos)) {
                return;
            }

            Map<BlockPos, BlockEntity> previousBlockEntities = captureBlockEntities(contraption, current);
            ClientContraption replacement =
                    ((ContraptionClientAccessor) contraption).invokeCreateClientContraption();
            if (replacement.getRenderLevel().isOutsideBuildHeight(requiredPos)) {
                LOGGER.error("[KCContraption] Updated bounds do not contain local block {}", requiredPos);
                return;
            }

            restoreTransientRenderState(previousBlockEntities, replacement);
            ClientContraptionAccessor replacementAccessor = (ClientContraptionAccessor) replacement;
            replacementAccessor.setStructureVersion(current.structureVersion());
            replacementAccessor.setChildrenVersion(current.childrenVersion());
            replacement.invalidateStructure();
            replacement.invalidateChildren();

            if (reference.compareAndSet(current, replacement)) {
                return;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void resetClientContraptionPreservingTransientState(Contraption contraption,
                                                                       ClientContraption clientContraption) {
        Map<BlockPos, BlockEntity> previousBlockEntities = captureBlockEntities(contraption, clientContraption);
        clientContraption.resetRenderLevel();
        restoreTransientRenderState(previousBlockEntities, clientContraption);
    }

    @OnlyIn(Dist.CLIENT)
    private static Map<BlockPos, BlockEntity> captureBlockEntities(Contraption contraption,
                                                                   ClientContraption clientContraption) {
        Map<BlockPos, BlockEntity> result = new HashMap<>();
        for (BlockPos pos : contraption.getBlocks().keySet()) {
            BlockEntity blockEntity = clientContraption.getBlockEntity(pos);
            if (blockEntity != null) {
                result.put(pos, blockEntity);
            }
        }
        return result;
    }

    @OnlyIn(Dist.CLIENT)
    private static void restoreTransientRenderState(Map<BlockPos, BlockEntity> previousBlockEntities,
                                                    ClientContraption clientContraption) {
        previousBlockEntities.forEach((pos, previous) -> {
            BlockEntity replacement = clientContraption.getBlockEntity(pos);
            if (replacement != null && replacement.getClass() == previous.getClass()
                    && replacement.getType() == previous.getType()) {
                copyTransientRenderState(previous, replacement);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void copyTransientRenderState(BlockEntity previous, BlockEntity replacement) {
        if (previous instanceof PotBlockEntity oldPot && replacement instanceof PotBlockEntity newPot) {
            newPot.animationData = oldPot.animationData;
        } else if (previous instanceof TeapotBlockEntity oldTeapot
                && replacement instanceof TeapotBlockEntity newTeapot) {
            newTeapot.boilingState = oldTeapot.boilingState;
        } else if (previous instanceof TrashCanBlockEntity oldTrashCan
                && replacement instanceof TrashCanBlockEntity newTrashCan) {
            newTrashCan.putState = oldTrashCan.putState;
            newTrashCan.withdrawState = oldTrashCan.withdrawState;
            newTrashCan.player1State = oldTrashCan.player1State;
            newTrashCan.player2State = oldTrashCan.player2State;
            newTrashCan.enterState = oldTrashCan.enterState;
        } else if (previous instanceof StockpotBlockEntity oldStockpot
                && replacement instanceof StockpotBlockEntity newStockpot) {
            if (newStockpot.getStatus() != 0
                    && Objects.equals(oldStockpot.getSoupBaseId(), newStockpot.getSoupBaseId())) {
                newStockpot.renderEntity = oldStockpot.renderEntity;
            }
        } else if (previous instanceof ChoppingBoardBlockEntity oldBoard
                && replacement instanceof ChoppingBoardBlockEntity newBoard
                && Objects.equals(oldBoard.getModelId(), newBoard.getModelId())
                && oldBoard.getMaxCutCount() == newBoard.getMaxCutCount()) {
            newBoard.previousModel = oldBoard.previousModel;
            newBoard.cacheModels = oldBoard.cacheModels;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static BlockEntityRenderInfo getBlockEntityRenderInfo(BlockPos pos, BlockState state) {
        if (state == null || !state.hasBlockEntity()
                || !(state.getBlock() instanceof EntityBlock entityBlock)) {
            return null;
        }
        BlockEntity blockEntity = entityBlock.newBlockEntity(pos, state);
        if (blockEntity == null) {
            return null;
        }
        BlockEntityType<?> type = blockEntity.getType();
        Class<? extends BlockEntity> blockEntityClass = blockEntity.getClass();
        blockEntity.setRemoved();
        MovementBehaviour movement = MovementBehaviour.REGISTRY.get(state);
        boolean rendered = movement == null || !movement.disableBlockEntityRendering();
        return new BlockEntityRenderInfo(type, blockEntityClass, rendered);
    }

    @OnlyIn(Dist.CLIENT)
    private static void prepareTransientRenderState(BlockEntity blockEntity,
                                                    StructureTemplate.StructureBlockInfo oldInfo,
                                                    StructureTemplate.StructureBlockInfo newInfo) {
        CompoundTag newNbt = Objects.requireNonNull(newInfo.nbt());
        if (blockEntity instanceof ChoppingBoardBlockEntity choppingBoard) {
            String oldModelId = oldInfo == null || oldInfo.nbt() == null
                    ? "" : oldInfo.nbt().getString("ModelId");
            String newModelId = newNbt.getString("ModelId");
            int oldMaxCutCount = oldInfo == null || oldInfo.nbt() == null
                    ? 0 : oldInfo.nbt().getInt("MaxCutCount");
            if (!oldModelId.equals(newModelId)
                    || oldMaxCutCount != newNbt.getInt("MaxCutCount")) {
                choppingBoard.previousModel = null;
                choppingBoard.cacheModels = null;
            }
            return;
        }

        if (!(blockEntity instanceof StockpotBlockEntity stockpot)) {
            return;
        }

        stockpot.visuals = null;
        if (!newNbt.contains("LidItem", Tag.TAG_COMPOUND)) {
            stockpot.setLidItem(ItemStack.EMPTY);
        }

        String oldSoupBase = oldInfo == null || oldInfo.nbt() == null
                ? "" : oldInfo.nbt().getString("SoupBaseId");
        String newSoupBase = newNbt.getString("SoupBaseId");
        if (newNbt.getInt("Status") == 0 || !oldSoupBase.equals(newSoupBase)) {
            stockpot.renderEntity = null;
        }
    }

    private record BlockEntityRenderInfo(BlockEntityType<?> type,
                                         Class<? extends BlockEntity> blockEntityClass,
                                         boolean rendered) {
    }
}
