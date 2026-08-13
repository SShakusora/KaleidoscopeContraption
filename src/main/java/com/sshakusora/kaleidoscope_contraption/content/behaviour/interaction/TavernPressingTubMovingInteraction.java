package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopetavern.api.blockentity.IPressingTub;
import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.PressingTubBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.crafting.recipe.PressingTubRecipe;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModRecipes;
import com.github.ysbbbbbb.kaleidoscopetavern.util.FluidUtils;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.TavernBrewingContraptionSupport;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import static com.github.ysbbbbbb.kaleidoscopetavern.config.GeneralConfig.PRESSING_TUB_DROP_CONTENTS_ON_NON_JUICEABLE;

public final class TavernPressingTubMovingInteraction extends SyncedMovingInteractionBehaviour {
    private static final String ITEMS = "items";

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info =
                contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof PressingTubBlock)) {
            return false;
        }
        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return removeTub(player, localPos, contraptionEntity, info);
        }

        CompoundTag nbt = TavernBrewingContraptionSupport.copyNbt(info);
        ItemStackHandler items = TavernBrewingContraptionSupport.readItems(
                nbt, ITEMS, 1, 64, contraptionEntity.level().registryAccess());
        FluidTank fluid = TavernBrewingContraptionSupport.readFluid(
                nbt, IPressingTub.MAX_FLUID_AMOUNT, contraptionEntity.level().registryAccess());
        ItemStack held = player.getItemInHand(activeHand);

        if (contraptionEntity.level().isClientSide) {
            return held.isEmpty() ? !items.getStackInSlot(0).isEmpty() : true;
        }

        boolean changed = false;
        if (held.isEmpty()) {
            int count = player.isSecondaryUseActive() ? 64 : 1;
            ItemStack removed = items.extractItem(0, count, false);
            if (!removed.isEmpty()) {
                ItemHandlerHelper.giveItemToPlayer(player, removed);
                playItemSound(contraptionEntity, localPos, SoundEvents.ITEM_FRAME_REMOVE_ITEM);
                changed = true;
            }
        } else if (fluid.getFluidAmount() >= IPressingTub.MAX_FLUID_AMOUNT
                && FluidUtils.fillItem(player, held, fluid, IPressingTub.MAX_FLUID_AMOUNT)) {
            changed = true;
        } else {
            int before = held.getCount();
            ItemStack remainder = items.insertItem(0, held.copy(), false);
            int inserted = before - remainder.getCount();
            if (inserted > 0) {
                if (!player.isCreative()) {
                    held.shrink(inserted);
                }
                playItemSound(contraptionEntity, localPos, SoundEvents.ITEM_FRAME_ADD_ITEM);
                changed = true;
            }
        }

        if (changed) {
            TavernBrewingContraptionSupport.writeItems(
                    nbt, ITEMS, items, contraptionEntity.level().registryAccess());
            TavernBrewingContraptionSupport.writeFluid(
                    nbt, fluid, contraptionEntity.level().registryAccess());
            TavernBrewingContraptionSupport.update(
                    contraptionEntity, localPos, info.state(), nbt);
        }
        return changed;
    }

    @Override
    public void handleEntityCollision(Entity entity, BlockPos localPos,
                                      AbstractContraptionEntity contraptionEntity) {
        if (contraptionEntity.level().isClientSide || !(entity instanceof LivingEntity living)
                || living.fallDistance < IPressingTub.MIN_FALL_DISTANCE) {
            return;
        }
        StructureTemplate.StructureBlockInfo info =
                contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !(info.state().getBlock() instanceof PressingTubBlock)
                || info.state().getValue(PressingTubBlock.TILT)) {
            return;
        }

        long marker = contraptionEntity.level().getGameTime();
        CompoundTag persistent = living.getPersistentData();
        if (persistent.getLong("KaleidoscopeContraptionLastTubPress") == marker
                && persistent.getLong("KaleidoscopeContraptionLastTubPos") == localPos.asLong()) {
            return;
        }
        persistent.putLong("KaleidoscopeContraptionLastTubPress", marker);
        persistent.putLong("KaleidoscopeContraptionLastTubPos", localPos.asLong());

        CompoundTag nbt = TavernBrewingContraptionSupport.copyNbt(info);
        ItemStackHandler items = TavernBrewingContraptionSupport.readItems(
                nbt, ITEMS, 1, 64, contraptionEntity.level().registryAccess());
        FluidTank fluid = TavernBrewingContraptionSupport.readFluid(
                nbt, IPressingTub.MAX_FLUID_AMOUNT, contraptionEntity.level().registryAccess());
        ItemStack input = items.getStackInSlot(0);
        if (input.isEmpty()) {
            playPressEffect(contraptionEntity, localPos, ItemStack.EMPTY,
                    fluid.isEmpty() ? SoundEvents.SLIME_BLOCK_FALL : SoundEvents.HONEY_BLOCK_HIT);
            return;
        }

        SingleRecipeInput container = new SingleRecipeInput(input.copy());
        RecipeHolder<PressingTubRecipe> recipeHolder = contraptionEntity.level().getRecipeManager()
                .getRecipeFor(ModRecipes.PRESSING_TUB_RECIPE, container, contraptionEntity.level())
                .orElse(null);
        if (recipeHolder == null) {
            playPressEffect(contraptionEntity, localPos, input, SoundEvents.SLIME_BLOCK_FALL);
            dropInvalidContents(contraptionEntity, localPos, info, nbt, items, fluid);
            return;
        }
        PressingTubRecipe recipe = recipeHolder.value();

        FluidStack produced = new FluidStack(recipe.getFluid(), recipe.getFluidAmount());
        if (!fluid.isEmpty() && !FluidStack.isSameFluidSameComponents(fluid.getFluid(), produced)) {
            playPressEffect(contraptionEntity, localPos, input, SoundEvents.SLIME_BLOCK_FALL);
            dropInvalidContents(contraptionEntity, localPos, info, nbt, items, fluid);
            return;
        }
        if (fluid.getFluidAmount() >= IPressingTub.MAX_FLUID_AMOUNT) {
            playPressEffect(contraptionEntity, localPos, input, SoundEvents.HONEY_BLOCK_HIT);
            return;
        }
        if (recipe.assemble(container, contraptionEntity.level().registryAccess()).isEmpty()) {
            playPressEffect(contraptionEntity, localPos, input, SoundEvents.SLIME_BLOCK_FALL);
            return;
        }

        fluid.fill(produced, IFluidHandler.FluidAction.EXECUTE);
        items.extractItem(0, 1, false);
        TavernBrewingContraptionSupport.writeItems(
                nbt, ITEMS, items, contraptionEntity.level().registryAccess());
        TavernBrewingContraptionSupport.writeFluid(
                nbt, fluid, contraptionEntity.level().registryAccess());
        TavernBrewingContraptionSupport.update(contraptionEntity, localPos, info.state(), nbt);
        playPressEffect(contraptionEntity, localPos, input, SoundEvents.SLIME_BLOCK_HIT);
    }

    private void dropInvalidContents(AbstractContraptionEntity entity, BlockPos localPos,
                                     StructureTemplate.StructureBlockInfo info, CompoundTag nbt,
                                     ItemStackHandler items, FluidTank fluid) {
        if (!PRESSING_TUB_DROP_CONTENTS_ON_NON_JUICEABLE.get()) {
            return;
        }
        ItemStack dropped = items.extractItem(0, 64, false);
        if (dropped.isEmpty()) {
            return;
        }
        Vec3 global = TavernBrewingContraptionSupport.globalCenter(entity, localPos);
        Block.popResource(entity.level(), BlockPos.containing(global), dropped);
        TavernBrewingContraptionSupport.writeItems(
                nbt, ITEMS, items, entity.level().registryAccess());
        TavernBrewingContraptionSupport.writeFluid(
                nbt, fluid, entity.level().registryAccess());
        TavernBrewingContraptionSupport.update(entity, localPos, info.state(), nbt);
    }

    private boolean removeTub(Player player, BlockPos localPos,
                              AbstractContraptionEntity contraptionEntity,
                              StructureTemplate.StructureBlockInfo info) {
        if (contraptionEntity.level().isClientSide) {
            return true;
        }
        if (!player.isCreative()) {
            ItemStack stack = new ItemStack(ModBlocks.PRESSING_TUB.get());
            if (info.nbt() != null) {
                BlockItem.setBlockEntityData(stack, ModBlocks.PRESSING_TUB_BE.get(), info.nbt().copy());
            }
            ItemHandlerHelper.giveItemToPlayer(player, stack);
        }
        ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);
        var bounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);
        contraptionEntity.getContraption().invalidateColliders();
        ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, bounds);
        ContraptionInteractionUtil.playBreakSound(contraptionEntity, localPos, info.state());
        return true;
    }

    private void playItemSound(AbstractContraptionEntity entity, BlockPos localPos,
                               net.minecraft.sounds.SoundEvent sound) {
        ContraptionInteractionUtil.playSound(entity, localPos, sound, SoundSource.BLOCKS,
                0.7F, 0.8F + entity.level().random.nextFloat() * 0.4F);
    }

    private void playPressEffect(AbstractContraptionEntity entity, BlockPos localPos,
                                 ItemStack stack, net.minecraft.sounds.SoundEvent sound) {
        ContraptionInteractionUtil.playSound(entity, localPos, sound, SoundSource.BLOCKS,
                0.8F, 0.8F + entity.level().random.nextFloat() * 0.3F);
        if (!(entity.level() instanceof ServerLevel level) || stack.isEmpty()) {
            return;
        }
        Vec3 pos = TavernBrewingContraptionSupport.globalCenter(entity, localPos);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack.copyWithCount(1)),
                pos.x, pos.y, pos.z, 8, 0.25, 0.2, 0.25, 0.04);
    }
}
