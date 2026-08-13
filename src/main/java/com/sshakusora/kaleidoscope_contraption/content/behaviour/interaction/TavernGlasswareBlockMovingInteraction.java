package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopetavern.block.mixology.GlasswareBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.mixology.SignatureCocktailBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.mixology.SignatureCocktailBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopetavern.item.ShakerItem;
import com.github.ysbbbbbb.kaleidoscopetavern.util.CocktailEffectHelper;
import com.github.ysbbbbbb.kaleidoscopetavern.util.ColorUtils;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.api.placement.ContraptionRemovalManager;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Handles simple Tavern glassware and cocktail blocks inside a contraption. */
public class TavernGlasswareBlockMovingInteraction extends MovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof GlasswareBlock)) {
            return false;
        }

        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            ContraptionRemovalManager.Result result = ContraptionRemovalManager.tryRemove(
                    player, localPos, contraptionEntity);
            if (result == ContraptionRemovalManager.Result.NOT_REGISTERED) {
                removeUnregistered(player, localPos, contraptionEntity, info);
                return true;
            }
            return result == ContraptionRemovalManager.Result.REMOVED;
        }

        ItemStack held = player.getItemInHand(activeHand);
        if (info.state().is(ModBlocks.EMPTY_GLASSWARE.get())
                && held.is(ModItems.SHAKER.get())
                && ShakerItem.hasResult(held)) {
            return pourShakerResult(held, localPos, contraptionEntity, info);
        }

        if (!held.isEmpty()) {
            return false;
        }
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(info.state().getBlock().asItem()));
        TavernContraptionInteractionSupport.removeBlock(
                contraptionEntity, localPos, SoundEvents.STONE_PLACE);
        return true;
    }

    /** Matches ShakerItem.useOn for an empty glassware block inside a moving contraption. */
    private boolean pourShakerResult(ItemStack shaker, BlockPos localPos,
                                     AbstractContraptionEntity contraptionEntity,
                                     StructureTemplate.StructureBlockInfo info) {
        ItemStack result = ShakerItem.getResult(shaker);
        if (!(result.getItem() instanceof BlockItem blockItem)) {
            return false;
        }

        BlockState resultState = blockItem.getBlock().defaultBlockState();
        if (resultState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                && info.state().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            resultState = resultState.setValue(BlockStateProperties.HORIZONTAL_FACING,
                    info.state().getValue(BlockStateProperties.HORIZONTAL_FACING));
        }

        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        CompoundTag nbt = new CompoundTag();
        if (resultState.getBlock() instanceof SignatureCocktailBlock) {
            CocktailEffectHelper.CollectedData data = CocktailEffectHelper.collectFromStorage(
                    ShakerItem.getStorage(shaker));
            SignatureCocktailBlockEntity blockEntity = new SignatureCocktailBlockEntity(localPos, resultState);
            blockEntity.setColor(ColorUtils.mixColors(data.colors()));
            blockEntity.setEffects(CocktailEffectHelper.mergeEffects(data.effects()));
            nbt = blockEntity.saveWithFullMetadata(contraptionEntity.level().registryAccess());
            nbt.remove("x");
            nbt.remove("y");
            nbt.remove("z");
        }

        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos,
                new StructureTemplate.StructureBlockInfo(info.pos(), resultState, nbt));
        ShakerItem.removeAll(shaker);
        playPourEffects(contraptionEntity, localPos);
        return true;
    }

    private void playPourEffects(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        TavernContraptionInteractionSupport.playSound(contraptionEntity, localPos, SoundEvents.BOTTLE_FILL);
        if (contraptionEntity.level() instanceof ServerLevel serverLevel) {
            Vec3 effectPos = ContraptionInteractionUtil.getGlobalPos(contraptionEntity, localPos);
            serverLevel.sendParticles(ParticleTypes.EFFECT,
                    effectPos.x, effectPos.y, effectPos.z,
                    20, 0.1, 0.1, 0.1, 0.5);
        }
    }

    private void removeUnregistered(Player player, BlockPos localPos,
                                    AbstractContraptionEntity contraptionEntity,
                                    StructureTemplate.StructureBlockInfo info) {
        if (contraptionEntity.level().isClientSide) {
            return;
        }
        if (!player.isCreative()) {
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(info.state().getBlock().asItem()));
        }
        TavernContraptionInteractionSupport.removeBlock(
                contraptionEntity, localPos, info.state());
    }
}
