package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.TeaBannerBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.decoration.TeaBannerBlockEntity;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.items.ItemHandlerHelper;

/** Preserves Tea Banner colour and pattern data inside a moving contraption. */
public final class TeaBannerBlockMovingInteraction extends BlockEntityDelegatingMovingInteraction {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand hand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        if (hand != InteractionHand.MAIN_HAND) {
            return false;
        }

        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof TeaBannerBlock)) {
            return false;
        }
        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return removeBanner(player, localPos, contraptionEntity, info);
        }

        ItemStack held = player.getMainHandItem();
        TeaBannerBlockEntity banner = loadBlockEntity(
                new TeaBannerBlockEntity(localPos, info.state()), info.nbt(), contraptionEntity);
        if (held.getItem() instanceof DyeItem dyeItem) {
            if (banner.getColor() == dyeItem.getDyeColor()) {
                return true;
            }
            if (contraptionEntity.level().isClientSide) {
                return true;
            }
            banner.setColor(dyeItem.getDyeColor());
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            saveBlockEntity(contraptionEntity, localPos, info, banner);
            return true;
        }

        if (held.getItem() instanceof BannerPatternItem && TeaBannerBlockEntity.isSupportedPattern(held)) {
            if (ItemStack.isSameItemSameTags(held, banner.getPatternItem())) {
                return true;
            }
            if (contraptionEntity.level().isClientSide) {
                return true;
            }
            ItemStack previous = banner.setPatternItem(held.copyWithCount(1));
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            if (!previous.isEmpty()) {
                ContraptionInteractionUtil.popResource(contraptionEntity, localPos, previous);
            }
            saveBlockEntity(contraptionEntity, localPos, info, banner);
            return true;
        }

        if (held.is(Items.SHEARS) && banner.hasPattern()) {
            if (contraptionEntity.level().isClientSide) {
                return true;
            }
            ContraptionInteractionUtil.popResource(contraptionEntity, localPos, banner.removePatternItem());
            held.hurtAndBreak(1, player, entity -> entity.broadcastBreakEvent(hand));
            saveBlockEntity(contraptionEntity, localPos, info, banner);
            return true;
        }

        return false;
    }

    private boolean removeBanner(Player player, BlockPos localPos,
                                 AbstractContraptionEntity contraptionEntity,
                                 StructureTemplate.StructureBlockInfo info) {
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        TeaBannerBlockEntity banner = loadBlockEntity(
                new TeaBannerBlockEntity(localPos, info.state()), info.nbt(), contraptionEntity);
        if (!player.isCreative()) {
            ItemHandlerHelper.giveItemToPlayer(player, banner.createItemStack());
        }
        ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);
        var updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);
        contraptionEntity.getContraption().invalidateColliders();
        ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, updatedBounds);
        ContraptionInteractionUtil.playBreakSound(contraptionEntity, localPos, info.state());
        return true;
    }
}
