package com.sshakusora.kaleidoscope_contraption.registry;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.BambooTrayBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/** Exposes Cookery's sided tray inventory to Create's mounted-storage API. */
public final class KCCookeryCapabilities {
    private KCCookeryCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlocks.BAMBOO_TRAY_BE.get(),
                (BambooTrayBlockEntity tray, Direction side) -> {
                    // Create asks for the unsided handler while mounting. The tray's
                    // sided wrapper exposes the same four slots on every face.
                    Direction exposedSide = side == null ? Direction.UP : side;
                    IItemHandler handler = tray.getItemHandler(exposedSide);
                    return handler;
                });

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlocks.TEAPOT_BE.get(),
                (TeapotBlockEntity teapot, Direction side) -> new TeapotInputModifiableHandler(teapot));
    }

    /**
     * Create's simple mounted storage requires a modifiable handler so it can
     * copy the inventory back when a moving structure is disassembled. Cookery
     * intentionally exposes the teapot input as a read-only handler, so this
     * adapter keeps its insertion rules while providing the required setter.
     */
    private static final class TeapotInputModifiableHandler implements IItemHandlerModifiable {
        private final TeapotBlockEntity teapot;
        private final IItemHandler delegate;

        private TeapotInputModifiableHandler(TeapotBlockEntity teapot) {
            this.teapot = teapot;
            this.delegate = teapot.getInputHandler();
        }

        @Override
        public int getSlots() {
            return delegate.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return delegate.getStackInSlot(slot).copy();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return delegate.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return delegate.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return delegate.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return delegate.isItemValid(slot, stack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (slot != 0 || stack.isEmpty() || !delegate.getStackInSlot(0).isEmpty()
                    || !teapot.canInsertIngredient(stack)) {
                return;
            }
            teapot.insertIngredient(stack.copyWithCount(1));
        }
    }
}
