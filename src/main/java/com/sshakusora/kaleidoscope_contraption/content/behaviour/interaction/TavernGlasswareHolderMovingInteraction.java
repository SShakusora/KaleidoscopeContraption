package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.GlasswareHolderBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.deco.GlasswareHolderBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModItems;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Optional;

public class TavernGlasswareHolderMovingInteraction extends BlockEntityDelegatingMovingInteraction {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        if (activeHand != InteractionHand.MAIN_HAND) {
            return false;
        }

        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof GlasswareHolderBlock)) {
            return false;
        }

        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return removeGlasswareHolder(player, localPos, contraptionEntity, info);
        }

        Optional<TavernContraptionInteractionSupport.Hit> hit =
                TavernContraptionInteractionSupport.findHit(player, localPos, contraptionEntity);
        if (hit.isEmpty()) {
            return false;
        }

        int slot = TavernContraptionInteractionSupport.getGlasswareSlot(localPos, hit.get());
        GlasswareHolderBlockEntity holder = loadBlockEntity(
                new GlasswareHolderBlockEntity(localPos, info.state()), info.nbt(), contraptionEntity);
        ItemStackHandler items = holder.getItems();
        ItemStack held = player.getItemInHand(activeHand);
        ItemStack stored = items.getStackInSlot(slot);

        if (held.is(ModItems.EMPTY_GLASSWARE.get())) {
            if (!stored.isEmpty()) {
                return false;
            }
            if (contraptionEntity.level().isClientSide) {
                return true;
            }

            items.setStackInSlot(slot, held.copyWithCount(1));
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            saveBlockEntity(contraptionEntity, localPos, info, holder);
            TavernContraptionInteractionSupport.playSound(contraptionEntity, localPos,
                    SoundEvents.AMETHYST_BLOCK_PLACE);
            return true;
        }

        if (!held.isEmpty() || !stored.isEmpty()) {
            if (!held.isEmpty()) {
                return false;
            }
            if (contraptionEntity.level().isClientSide) {
                return true;
            }

            ItemStack extracted = items.extractItem(slot, 1, false);
            player.setItemInHand(activeHand, extracted);
            saveBlockEntity(contraptionEntity, localPos, info, holder);
            TavernContraptionInteractionSupport.playSound(contraptionEntity, localPos,
                    SoundEvents.AMETHYST_BLOCK_PLACE);
            return true;
        }

        return false;
    }

    private boolean removeGlasswareHolder(Player player, BlockPos localPos,
                                          AbstractContraptionEntity contraptionEntity,
                                          StructureTemplate.StructureBlockInfo info) {
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        GlasswareHolderBlockEntity holder = loadBlockEntity(
                new GlasswareHolderBlockEntity(localPos, info.state()), info.nbt(), contraptionEntity);
        if (!player.isCreative()) {
            for (int slot = 0; slot < holder.getItems().getSlots(); slot++) {
                ItemStack stored = holder.getItems().getStackInSlot(slot);
                if (!stored.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(stored.copy());
                }
            }
            player.getInventory().placeItemBackInInventory(new ItemStack(info.state().getBlock().asItem()));
        }
        TavernContraptionInteractionSupport.removeBlock(contraptionEntity, localPos, info.state());
        return true;
    }
}

