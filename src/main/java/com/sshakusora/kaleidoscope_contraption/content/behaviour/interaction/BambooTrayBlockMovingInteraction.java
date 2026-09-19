package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.BambooTrayBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.BambooTrayBlockEntity;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.Optional;

/** Adapts Cookery's four-slot bamboo tray to Create's moving interaction API. */
public class BambooTrayBlockMovingInteraction extends BlockEntityDelegatingMovingInteraction {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand hand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        if (hand != InteractionHand.MAIN_HAND) {
            return false;
        }

        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof BambooTrayBlock)) {
            return false;
        }

        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return removeTray(player, localPos, contraptionEntity, info);
        }

        Optional<Hit> hit = findHit(player, localPos, contraptionEntity, info);
        if (hit.isEmpty() || hit.get().face() != Direction.UP) {
            return false;
        }

        int slot = getTraySlot(localPos, hit.get());
        ItemStack held = player.getMainHandItem();
        BambooTrayBlockEntity tray = loadBlockEntity(
                new BambooTrayBlockEntity(localPos, info.state()), info.nbt(), contraptionEntity);
        copyMountedItemsToEntity(contraptionEntity, localPos, tray);

        if (contraptionEntity.level().isClientSide) {
            return held.isEmpty() || tray.getItem(slot).isEmpty() || !held.isEmpty();
        }

        boolean changed;
        if (held.isEmpty()) {
            changed = tray.onTakeOut(contraptionEntity.level(), player, slot,
                    player.isSecondaryUseActive());
        } else {
            changed = tray.onPutItem(contraptionEntity.level(), player, held, slot);
        }

        if (changed) {
            copyEntityItemsToMountedStorage(contraptionEntity, localPos, tray);
            saveBlockEntity(contraptionEntity, localPos, info, tray);
        }
        return changed;
    }

    private boolean removeTray(Player player, BlockPos localPos,
                               AbstractContraptionEntity contraptionEntity,
                               StructureTemplate.StructureBlockInfo info) {
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        BambooTrayBlockEntity tray = loadBlockEntity(
                new BambooTrayBlockEntity(localPos, info.state()), info.nbt(), contraptionEntity);
        copyMountedItemsToEntity(contraptionEntity, localPos, tray);
        if (!player.isCreative()) {
            for (ItemStack stack : tray.getItems()) {
                if (!stack.isEmpty()) {
                    ItemHandlerHelper.giveItemToPlayer(player, stack.copy());
                }
            }
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(info.state().getBlock().asItem()));
        }

        ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);
        var updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);
        contraptionEntity.getContraption().invalidateColliders();
        ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, updatedBounds);
        ContraptionInteractionUtil.playBreakSound(contraptionEntity, localPos, info.state());
        return true;
    }

    private void copyMountedItemsToEntity(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                           BambooTrayBlockEntity tray) {
        MountedItemStorage storage = contraptionEntity.getContraption().getStorage()
                .getAllItemStorages().get(localPos);
        if (storage == null || storage.getSlots() < tray.getItems().size()) {
            return;
        }
        for (int slot = 0; slot < tray.getItems().size(); slot++) {
            tray.getItems().set(slot, storage.getStackInSlot(slot).copy());
        }
    }

    private void copyEntityItemsToMountedStorage(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                                  BambooTrayBlockEntity tray) {
        MountedItemStorage storage = contraptionEntity.getContraption().getStorage()
                .getAllItemStorages().get(localPos);
        if (storage == null || storage.getSlots() < tray.getItems().size()) {
            return;
        }
        for (int slot = 0; slot < tray.getItems().size(); slot++) {
            storage.setStackInSlot(slot, tray.getItem(slot).copy());
        }
    }

    private Optional<Hit> findHit(Player player, BlockPos localPos,
                                  AbstractContraptionEntity contraptionEntity,
                                  StructureTemplate.StructureBlockInfo info) {
        Vec3 eyePosition = player.getEyePosition(1.0F);
        Vec3 endPosition = eyePosition.add(player.getViewVector(1.0F)
                .scale(player.blockInteractionRange()));
        Vec3 localEyePosition = contraptionEntity.toLocalVector(eyePosition, 1.0F);
        Vec3 localEndPosition = contraptionEntity.toLocalVector(endPosition, 1.0F);
        VoxelShape shape = info.state().getShape(
                contraptionEntity.getContraption().getContraptionWorld(),
                localPos,
                CollisionContext.of(player));
        BlockHitResult hit = shape.clip(localEyePosition, localEndPosition, localPos);
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            return Optional.empty();
        }
        return Optional.of(new Hit(hit.getLocation(), hit.getDirection()));
    }

    private int getTraySlot(BlockPos localPos, Hit hit) {
        double localX = hit.point().x - localPos.getX();
        double localZ = hit.point().z - localPos.getZ();
        return (localZ >= 0.5 ? 2 : 0) + (localX >= 0.5 ? 1 : 0);
    }

    private record Hit(Vec3 point, Direction face) {
    }
}
