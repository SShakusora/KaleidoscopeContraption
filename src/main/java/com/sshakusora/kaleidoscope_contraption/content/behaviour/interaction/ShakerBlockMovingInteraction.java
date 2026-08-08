package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopetavern.block.mixology.ShakerBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.mixology.ShakerBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopetavern.init.tag.TagMod;
import com.github.ysbbbbbb.kaleidoscopetavern.item.ShakerItem;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import com.sshakusora.kaleidoscope_contraption.util.TavernContraptionRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.items.ItemHandlerHelper;

/** Adapts ShakerBlock's three ingredient slots and result item to contraption NBT. */
public class ShakerBlockMovingInteraction extends BlockEntityDelegatingMovingInteraction {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        if (activeHand != InteractionHand.MAIN_HAND) {
            return false;
        }

        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof ShakerBlock)) {
            return false;
        }

        ItemStack held = player.getItemInHand(activeHand);
        boolean removeKeyPressed = KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID());
        if (removeKeyPressed || held.isEmpty()) {
            removeShaker(player, localPos, contraptionEntity, info, removeKeyPressed);
            return true;
        }
        if (!held.is(TagMod.COCKTAIL_INGREDIENT)) {
            return false;
        }
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        // ShakerBlockEntity emits its sound and particles using worldPosition. Use the
        // transformed position so those effects do not appear at the contraption's local origin.
        BlockPos effectPos = ContraptionInteractionUtil.getGlobalBlockPos(contraptionEntity, localPos);
        ShakerBlockEntity blockEntity = loadShakerBlockEntity(
                effectPos, info.state(), info.nbt(), contraptionEntity);
        if (!blockEntity.addIngredient(held, player)) {
            return false;
        }

        saveShakerBlockEntity(contraptionEntity, localPos, info, blockEntity);
        return true;
    }

    private void saveShakerBlockEntity(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                       StructureTemplate.StructureBlockInfo info,
                                       ShakerBlockEntity blockEntity) {
        CompoundTag tag = blockEntity.saveWithFullMetadata();
        tag.remove("x");
        tag.remove("y");
        tag.remove("z");

        int previousAnimation = info.nbt() == null ? 0
                : info.nbt().getInt(TavernContraptionRenderState.SHAKER_PUT_ANIMATION);
        tag.putInt(TavernContraptionRenderState.SHAKER_PUT_ANIMATION, previousAnimation + 1);

        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos,
                new StructureTemplate.StructureBlockInfo(info.pos(), info.state(), tag));
    }

    private void removeShaker(Player player, BlockPos localPos,
                              AbstractContraptionEntity contraptionEntity,
                              StructureTemplate.StructureBlockInfo info,
                              boolean removeKeyPressed) {
        if (contraptionEntity.level().isClientSide) {
            return;
        }

        BlockPos effectPos = ContraptionInteractionUtil.getGlobalBlockPos(contraptionEntity, localPos);
        ShakerBlockEntity blockEntity = loadShakerBlockEntity(
                effectPos, info.state(), info.nbt(), contraptionEntity);
        if (!removeKeyPressed || !player.isCreative()) {
            ItemStack shaker = ModItems.SHAKER.get().getDefaultInstance();
            ShakerItem.setStorage(shaker, blockEntity.getStorage());
            if (!blockEntity.getResult().isEmpty()) {
                ShakerItem.setResult(shaker, blockEntity.getResult());
            }
            ItemHandlerHelper.giveItemToPlayer(player, shaker);
        }

        TavernContraptionInteractionSupport.removeBlock(
                contraptionEntity, localPos, SoundEvents.LANTERN_BREAK);
    }

    private ContraptionShakerBlockEntity loadShakerBlockEntity(BlockPos effectPos, BlockState state,
                                                               CompoundTag tag,
                                                               AbstractContraptionEntity contraptionEntity) {
        ContraptionShakerBlockEntity blockEntity = loadBlockEntity(
                new ContraptionShakerBlockEntity(effectPos, state), tag, contraptionEntity);

        // ShakerBlockEntity.load uses ItemStack.deserializeNBT on ItemStack.EMPTY.
        // Forge restores only the tag there, so restore the result's item and count explicitly.
        if (tag != null && tag.contains("result", Tag.TAG_COMPOUND)) {
            blockEntity.setResult(ItemStack.of(tag.getCompound("result")));
        }
        return blockEntity;
    }

    /** Keeps Tavern's sounds and particles while preventing refresh() from updating real-world coordinates. */
    private static final class ContraptionShakerBlockEntity extends ShakerBlockEntity {
        private ContraptionShakerBlockEntity(BlockPos pos, BlockState state) {
            super(pos, state);
        }

        @Override
        public void refresh() {
            // ContraptionInteractionBehaviour persists the virtual block data itself.
        }
    }
}
