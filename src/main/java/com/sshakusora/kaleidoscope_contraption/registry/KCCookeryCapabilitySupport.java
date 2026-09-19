package com.sshakusora.kaleidoscope_contraption.registry;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.inventory.itemhandler.TeapotInputHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

/** Forge-only adapters used to expose Cookery inventories to Create mounted storage. */
public final class KCCookeryCapabilitySupport {
    private KCCookeryCapabilitySupport() {
    }

    public static IItemHandlerModifiable teapotHandler(TeapotBlockEntity teapot) {
        return new TeapotInputModifiableHandler(teapot);
    }

    private static final class TeapotInputModifiableHandler implements IItemHandlerModifiable {
        private final TeapotBlockEntity teapot;
        private final IItemHandler delegate;

        private TeapotInputModifiableHandler(TeapotBlockEntity teapot) {
            this.teapot = teapot;
            this.delegate = new TeapotInputHandler(teapot);
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
            if (slot != 0 || stack.isEmpty()) {
                return;
            }
            ItemStack current = teapot.getInput();
            if (!current.isEmpty() || !teapot.canInsertIngredient(stack)) {
                return;
            }
            teapot.insertIngredient(stack.copyWithCount(1));
        }
    }
}
