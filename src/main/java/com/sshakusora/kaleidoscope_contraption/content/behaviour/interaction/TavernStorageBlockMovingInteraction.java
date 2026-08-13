package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopetavern.block.AbstractStorageBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.BottleBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.CellarCabinetBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.CircularRackBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.HolderBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.TiltedRackBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.brew.CellarCabinetBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.deco.CircularRackBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.deco.HolderBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.deco.StorageBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.deco.TiltedRackBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.init.tag.TagMod;
import com.github.ysbbbbbb.kaleidoscopetavern.item.BottleBlockItem;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Optional;

public class TavernStorageBlockMovingInteraction extends BlockEntityDelegatingMovingInteraction {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        if (activeHand != InteractionHand.MAIN_HAND) {
            return false;
        }

        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof AbstractStorageBlock)) {
            return false;
        }

        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return removeStorageBlock(player, localPos, contraptionEntity, info);
        }

        Optional<TavernContraptionInteractionSupport.Hit> hit =
                TavernContraptionInteractionSupport.findHit(player, localPos, contraptionEntity);
        if (hit.isEmpty()) {
            return false;
        }

        int slot = TavernContraptionInteractionSupport.getStorageSlot(info.state(), localPos, hit.get());
        if (slot < 0) {
            return false;
        }

        StorageBlockEntity storage = createStorageBlockEntity(localPos, info.state());
        if (storage == null) {
            return false;
        }
        loadBlockEntity(storage, info.nbt(), contraptionEntity);

        ItemStack held = player.getItemInHand(activeHand);
        ItemStackHandler items = storage.getItems();
        ItemStack stored = items.getStackInSlot(slot);

        if (held.isEmpty()) {
            if (stored.isEmpty()) {
                return false;
            }
            if (contraptionEntity.level().isClientSide) {
                return true;
            }

            ItemStack extracted = items.extractItem(slot, 1, false);
            player.setItemInHand(activeHand, extracted);
            saveBlockEntity(contraptionEntity, localPos, info, storage);
            TavernContraptionInteractionSupport.playSound(contraptionEntity, localPos,
                    SoundEvents.ITEM_FRAME_REMOVE_ITEM);
            return true;
        }

        if (!(held.getItem() instanceof BottleBlockItem bottleItem)
                || !(bottleItem.getBlock() instanceof BottleBlock)
                || isBlocklisted(info.state().getBlock(), held)
                || !stored.isEmpty()) {
            return false;
        }

        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        items.setStackInSlot(slot, held.split(1));
        saveBlockEntity(contraptionEntity, localPos, info, storage);
        TavernContraptionInteractionSupport.playSound(contraptionEntity, localPos,
                SoundEvents.STONE_PLACE);
        return true;
    }

    private boolean removeStorageBlock(Player player, BlockPos localPos,
                                       AbstractContraptionEntity contraptionEntity,
                                       StructureTemplate.StructureBlockInfo info) {
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        StorageBlockEntity storage = createStorageBlockEntity(localPos, info.state());
        if (storage != null) {
            loadBlockEntity(storage, info.nbt(), contraptionEntity);
            if (!player.isCreative()) {
                for (int slot = 0; slot < storage.getItems().getSlots(); slot++) {
                    ItemStack stored = storage.getItems().getStackInSlot(slot);
                    if (!stored.isEmpty()) {
                        player.getInventory().placeItemBackInInventory(stored.copy());
                    }
                }
            }
        }

        if (!player.isCreative()) {
            player.getInventory().placeItemBackInInventory(new ItemStack(info.state().getBlock().asItem()));
        }
        TavernContraptionInteractionSupport.removeBlock(contraptionEntity, localPos, info.state());
        return true;
    }

    private boolean isBlocklisted(Block block, ItemStack stack) {
        if (block instanceof HolderBlock) {
            return stack.is(TagMod.HOLDER_BLOCKLIST);
        }
        if (block instanceof TiltedRackBlock) {
            return stack.is(TagMod.TILTED_RACK_BLOCKLIST);
        }
        if (block instanceof CircularRackBlock) {
            return stack.is(TagMod.CIRCULAR_RACK_BLOCKLIST);
        }
        if (block instanceof CellarCabinetBlock) {
            return stack.is(TagMod.CELLAR_CABINET_BLOCKLIST);
        }
        return true;
    }

    private StorageBlockEntity createStorageBlockEntity(BlockPos localPos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof HolderBlock) {
            return new HolderBlockEntity(localPos, state);
        }
        if (block instanceof TiltedRackBlock) {
            return new TiltedRackBlockEntity(localPos, state);
        }
        if (block instanceof CircularRackBlock) {
            return new CircularRackBlockEntity(localPos, state);
        }
        if (block instanceof CellarCabinetBlock) {
            return new CellarCabinetBlockEntity(localPos, state);
        }
        return null;
    }
}

