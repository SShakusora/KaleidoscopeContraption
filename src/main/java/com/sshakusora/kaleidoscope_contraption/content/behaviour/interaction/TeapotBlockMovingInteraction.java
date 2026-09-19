package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.api.blockentity.ITeapot;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.TeapotBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.util.FluidUtils;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemHandlerHelper;

public class TeapotBlockMovingInteraction extends BlockEntityDelegatingMovingInteraction {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand hand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof TeapotBlock)) return false;
        ItemStack held = player.getItemInHand(hand);
        if (contraptionEntity.level().isClientSide) return true;

        TeapotBlockEntity blockEntity = loadBlockEntity(
                new TeapotBlockEntity(localPos, info.state()), info.nbt(), contraptionEntity);
        boolean changed;
        if (held.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) {
            changed = FluidUtils.hasFluid(held)
                    ? blockEntity.addTeaFluid(contraptionEntity.level(), player, held)
                    : blockEntity.removeTeaFluid(contraptionEntity.level(), player, held);
        } else if (!held.isEmpty()) {
            changed = blockEntity.addIngredient(contraptionEntity.level(), player, held);
        } else if (player.isSecondaryUseActive()) {
            changed = blockEntity.removeIngredient(contraptionEntity.level(), player);
        } else {
            givePendingMountedItems(player, contraptionEntity, localPos, blockEntity);
            for (ItemStack drop : blockEntity.getDrops()) ItemHandlerHelper.giveItemToPlayer(player, drop);
            ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);
            var bounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);
            ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, bounds);
            return true;
        }
        if (changed) {
            saveBlockEntity(contraptionEntity, localPos, info, blockEntity);
            syncMountedInput(contraptionEntity, localPos, blockEntity, held);
        }
        return changed;
    }

    private void syncMountedInput(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                   TeapotBlockEntity blockEntity, ItemStack held) {
        MountedItemStorage storage = contraptionEntity.getContraption().getStorage()
                .getAllItemStorages().get(localPos);
        if (storage == null || storage.getSlots() == 0) {
            return;
        }

        boolean fluidInteraction = held.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
        boolean preservePendingInput = fluidInteraction
                && blockEntity.getStatus() == ITeapot.PUT_INGREDIENT
                && blockEntity.getInput().isEmpty()
                && !storage.getStackInSlot(0).isEmpty();
        if (preservePendingInput) {
            return;
        }

        ItemStack mirrored = blockEntity.getStatus() == ITeapot.PUT_INGREDIENT
                ? blockEntity.getInput().copyWithCount(1)
                : ItemStack.EMPTY;
        storage.setStackInSlot(0, mirrored);
    }

    /**
     * Merges a hopper item that is still in Create's mounted copy before the
     * teapot is removed. The copy can either mirror the NBT input or be a new
     * pending item that has not reached the block entity yet.
     */
    private void givePendingMountedItems(Player player, AbstractContraptionEntity contraptionEntity,
                                         BlockPos localPos, TeapotBlockEntity blockEntity) {
        MountedItemStorage storage = contraptionEntity.getContraption().getStorage()
                .getAllItemStorages().get(localPos);
        if (storage == null || storage.getSlots() == 0) {
            return;
        }

        ItemStack mounted = storage.getStackInSlot(0).copy();
        if (mounted.isEmpty()) {
            return;
        }

        ItemStack input = blockEntity.getInput();
        if (input.isEmpty() && blockEntity.getStatus() == ITeapot.PUT_INGREDIENT
                && blockEntity.canInsertIngredient(mounted)) {
            blockEntity.insertIngredient(mounted.copyWithCount(1));
            mounted.shrink(1);
        } else if (!input.isEmpty()
                && mounted.getCount() == 1
                && ItemStack.isSameItemSameTags(input, mounted)) {
            // A one-item mounted copy is the normal mirror of the NBT input.
            mounted = ItemStack.EMPTY;
        }

        if (!mounted.isEmpty()) {
            ItemHandlerHelper.giveItemToPlayer(player, mounted);
        }
        storage.setStackInSlot(0, ItemStack.EMPTY);
    }
}
