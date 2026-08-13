package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopetavern.api.blockentity.IBarrel;
import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.BarrelBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopetavern.item.BottleBlockItem;
import com.github.ysbbbbbb.kaleidoscopetavern.util.FluidUtils;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.TavernBrewingContraptionSupport;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TavernBarrelMovingInteraction extends SyncedMovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        if (activeHand != InteractionHand.MAIN_HAND) {
            return false;
        }
        StructureTemplate.StructureBlockInfo clicked =
                contraptionEntity.getContraption().getBlocks().get(localPos);
        if (clicked == null || !(clicked.state().getBlock() instanceof BarrelBlock)) {
            return false;
        }
        TavernBrewingContraptionSupport.BarrelData barrel =
                TavernBrewingContraptionSupport.findBarrel(
                        contraptionEntity.getContraption(), localPos, clicked.state());
        if (barrel == null) {
            return false;
        }
        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return removeBarrel(player, contraptionEntity, barrel, clicked.state());
        }

        BlockState clickedState = clicked.state();
        if (clickedState.getValue(BarrelBlock.LAYER) != AttachFace.CEILING) {
            if (!contraptionEntity.level().isClientSide) {
                showBrewInfo(player, barrel.nbt(), contraptionEntity.level().registryAccess());
            }
            return true;
        }

        CompoundTagAccess data = new CompoundTagAccess(barrel.nbt());
        boolean brewing = TavernBrewingContraptionSupport.isBrewing(data.nbt);
        boolean open = data.nbt.contains(TavernBrewingContraptionSupport.OPEN)
                ? data.nbt.getBoolean(TavernBrewingContraptionSupport.OPEN) : true;
        ItemStack held = player.getItemInHand(activeHand);
        boolean lid = clickedState.getValue(BarrelBlock.INDEX) == 4;

        if (!open) {
            if (brewing) {
                if (!contraptionEntity.level().isClientSide) {
                    tip(player, "brewing_unable_to_open");
                }
                return true;
            }
            return setOpen(contraptionEntity, barrel, true);
        }
        if (held.isEmpty() && !lid) {
            return setOpen(contraptionEntity, barrel, false);
        }
        if (!lid) {
            return false;
        }
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        boolean changed = interactWithLid(player, held, data, contraptionEntity.level().registryAccess());
        if (!changed) {
            return false;
        }
        TavernBrewingContraptionSupport.update(contraptionEntity, barrel.origin(),
                barrel.info().state(), data.nbt);
        return true;
    }

    private boolean interactWithLid(Player player, ItemStack held, CompoundTagAccess data,
                                    net.minecraft.core.HolderLookup.Provider registries) {
        if (TavernBrewingContraptionSupport.isBrewing(data.nbt)
                || !data.nbt.getBoolean(TavernBrewingContraptionSupport.OPEN)) {
            return false;
        }
        ItemStackHandler ingredients = TavernBrewingContraptionSupport.readItems(
                data.nbt, TavernBrewingContraptionSupport.INGREDIENT,
                IBarrel.MAX_ITEM_SLOTS, 16, registries);
        FluidTank fluid = TavernBrewingContraptionSupport.readFluid(
                data.nbt, IBarrel.MAX_FLUID_AMOUNT, registries);

        if (FluidUtils.isFluidContainer(held)) {
            if (!allEmpty(ingredients)) {
                tip(player, fluid.isEmpty()
                        ? "add_fluid_ingredient_not_empty"
                        : "remove_fluid_ingredient_not_empty");
                return false;
            }
            Optional<FluidStack> contained = FluidUtil.getFluidContained(held);
            boolean changed = contained.isPresent() && !contained.get().isEmpty()
                    ? FluidUtils.emptyItem(player, held, fluid, IBarrel.MAX_FLUID_AMOUNT)
                    : FluidUtils.fillItem(player, held, fluid, IBarrel.MAX_FLUID_AMOUNT);
            if (changed) {
                TavernBrewingContraptionSupport.writeFluid(data.nbt, fluid, registries);
            }
            return changed;
        }

        if (!held.isEmpty()) {
            if (fluid.getFluidAmount() < IBarrel.MAX_FLUID_AMOUNT) {
                tip(player, "add_ingredient_fluid_not_full");
                return false;
            }
            int before = held.getCount();
            ItemStack remaining = insertIngredientOnce(ingredients, held.copy());
            int inserted = before - remaining.getCount();
            if (inserted <= 0) {
                tip(player, "add_ingredient_no_space");
                return false;
            }
            if (!player.isCreative()) {
                held.shrink(inserted);
            }
            TavernBrewingContraptionSupport.writeItems(
                    data.nbt, TavernBrewingContraptionSupport.INGREDIENT, ingredients, registries);
            player.playSound(SoundEvents.ITEM_FRAME_ADD_ITEM);
            return true;
        }

        for (int slot = ingredients.getSlots() - 1; slot >= 0; slot--) {
            ItemStack stack = ingredients.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                ItemStack removed = ingredients.extractItem(slot, stack.getCount(), false);
                ItemHandlerHelper.giveItemToPlayer(player, removed);
                TavernBrewingContraptionSupport.writeItems(
                        data.nbt, TavernBrewingContraptionSupport.INGREDIENT, ingredients, registries);
                player.playSound(SoundEvents.ITEM_FRAME_REMOVE_ITEM);
                return true;
            }
        }
        return false;
    }

    private ItemStack insertIngredientOnce(ItemStackHandler ingredients, ItemStack stack) {
        int before = stack.getCount();
        for (int slot = 0; slot < ingredients.getSlots(); slot++) {
            ItemStack existing = ingredients.getStackInSlot(slot);
            if (ItemStack.isSameItemSameComponents(existing, stack)) {
                ItemStack remaining = ingredients.insertItem(slot, stack, false);
                if (remaining.getCount() < before) {
                    return remaining;
                }
            }
        }
        for (int slot = 0; slot < ingredients.getSlots(); slot++) {
            if (ingredients.getStackInSlot(slot).isEmpty()) {
                return ingredients.insertItem(slot, stack, false);
            }
        }
        return stack;
    }

    private boolean setOpen(AbstractContraptionEntity entity,
                            TavernBrewingContraptionSupport.BarrelData barrel,
                            boolean open) {
        if (entity.level().isClientSide) {
            return true;
        }
        net.minecraft.nbt.CompoundTag nbt = barrel.nbt().copy();
        nbt.putBoolean(TavernBrewingContraptionSupport.OPEN, open);
        TavernBrewingContraptionSupport.update(
                entity, barrel.origin(), barrel.info().state(), nbt);
        ContraptionInteractionUtil.playSound(entity, barrel.origin().above(2),
                open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE,
                SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    private boolean removeBarrel(Player player, AbstractContraptionEntity entity,
                                 TavernBrewingContraptionSupport.BarrelData barrel,
                                 BlockState clickedState) {
        if (entity.level().isClientSide) {
            return true;
        }
        if (!player.isCreative()) {
            ItemStack stack = new ItemStack(ModBlocks.BARREL.get());
            BlockItem.setBlockEntityData(stack, ModBlocks.BARREL_BE.get(), barrel.nbt().copy());
            ItemHandlerHelper.giveItemToPlayer(player, stack);
        }

        List<BlockPos> positions = new ArrayList<>(27);
        for (int y = 0; y < 3; y++) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    BlockPos pos = barrel.origin().offset(col - 1, y, row - 1);
                    StructureTemplate.StructureBlockInfo info =
                            entity.getContraption().getBlocks().get(pos);
                    if (info != null && info.state().getBlock() instanceof BarrelBlock
                            && BarrelBlock.getOriginPos(pos, info.state()).equals(barrel.origin())
                            && info.state().getValue(BarrelBlock.FACING)
                            == clickedState.getValue(BarrelBlock.FACING)) {
                        positions.add(pos);
                    }
                }
            }
        }
        for (BlockPos pos : positions) {
            ContraptionInteractionUtil.removeBlockFromContraption(entity, pos);
        }
        var bounds = ContraptionInteractionUtil.recalculateBounds(entity);
        entity.getContraption().invalidateColliders();
        ContraptionInteractionUtil.syncBlockRemoval(
                entity, bounds, positions.toArray(BlockPos[]::new));
        ContraptionInteractionUtil.playBreakSound(
                entity, barrel.origin(), barrel.info().state());
        return true;
    }

    private void showBrewInfo(Player player, net.minecraft.nbt.CompoundTag nbt,
                              net.minecraft.core.HolderLookup.Provider registries) {
        int level = BottleBlockItem.clampBrewLevel(
                nbt.getInt(TavernBrewingContraptionSupport.BREW_LEVEL));
        if (level < IBarrel.BREWING_STARTED) {
            player.displayClientMessage(
                    Component.translatable("message.kaleidoscope_tavern.barrel.not_brewing"), true);
            return;
        }
        ItemStack result = TavernBrewingContraptionSupport.getBarrelOutput(nbt, registries);
        Component levelText = Component.translatable(
                "message.kaleidoscope_tavern.barrel.brew_level.%d".formatted(level));
        if (level >= IBarrel.BREWING_FINISHED) {
            player.displayClientMessage(Component.translatable(
                    "message.kaleidoscope_tavern.barrel.brew_info.full",
                    result.getHoverName(), result.getCount(), levelText), true);
            return;
        }
        Component time = Component.literal(StringUtil.formatTickDuration(
                Math.max(0, nbt.getInt(TavernBrewingContraptionSupport.BREW_TIME)), 1.0F));
        player.displayClientMessage(Component.translatable(
                "message.kaleidoscope_tavern.barrel.brew_info.next",
                result.getHoverName(), result.getCount(), levelText, time), true);
    }

    private void tip(Player player, String key) {
        player.displayClientMessage(Component.translatable(
                "message.kaleidoscope_tavern.barrel.%s".formatted(key)), true);
    }

    private boolean allEmpty(ItemStackHandler items) {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static final class CompoundTagAccess {
        private final net.minecraft.nbt.CompoundTag nbt;

        private CompoundTagAccess(net.minecraft.nbt.CompoundTag nbt) {
            this.nbt = nbt.copy();
            if (!this.nbt.contains(TavernBrewingContraptionSupport.OPEN)) {
                this.nbt.putBoolean(TavernBrewingContraptionSupport.OPEN, true);
            }
        }
    }
}
