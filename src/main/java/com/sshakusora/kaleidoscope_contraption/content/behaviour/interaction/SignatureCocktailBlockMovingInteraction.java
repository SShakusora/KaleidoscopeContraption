package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopetavern.block.mixology.SignatureCocktailBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.mixology.SignatureCocktailBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopetavern.item.SignatureCocktailBlockItem;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.items.ItemHandlerHelper;

/** Preserves and extracts Signature Cocktail BlockEntity data inside a contraption. */
public class SignatureCocktailBlockMovingInteraction extends BlockEntityDelegatingMovingInteraction {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof SignatureCocktailBlock)) {
            return false;
        }

        boolean removeKeyPressed = KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID());
        if (!removeKeyPressed && !player.getItemInHand(activeHand).isEmpty()) {
            return false;
        }
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        SignatureCocktailBlockEntity blockEntity = loadBlockEntity(
                new SignatureCocktailBlockEntity(localPos, info.state()), info.nbt(), contraptionEntity);
        if (!player.isCreative() || !removeKeyPressed) {
            ItemStack cocktail = ModItems.SIGNATURE_COCKTAIL.get().getDefaultInstance();
            SignatureCocktailBlockItem.setEffects(cocktail, blockEntity.getEffects());
            SignatureCocktailBlockItem.setColor(cocktail, blockEntity.getColor());
            ItemHandlerHelper.giveItemToPlayer(player, cocktail);
        }

        // GlasswareBlock uses the stone placement sound when the finished drink is picked up.
        TavernContraptionInteractionSupport.removeBlock(
                contraptionEntity, localPos, SoundEvents.STONE_PLACE);
        return true;
    }
}
