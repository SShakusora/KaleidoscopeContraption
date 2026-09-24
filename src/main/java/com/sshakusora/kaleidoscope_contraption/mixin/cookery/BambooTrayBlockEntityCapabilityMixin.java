package com.sshakusora.kaleidoscope_contraption.mixin.cookery;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.BambooTrayBlockEntity;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bridges Cookery's sided tray handler to Create's unsided mounted-storage lookup. */
@Mixin(value = BambooTrayBlockEntity.class, remap = false)
public abstract class BambooTrayBlockEntityCapabilityMixin {
    @Inject(method = "getCapability", at = @At("HEAD"), cancellable = true, remap = false)
    private <T> void kaleidoscopeContraption$exposeUnsidedHandler(
            Capability<T> capability, @Nullable Direction side,
            CallbackInfoReturnable<LazyOptional<T>> cir) {
        if (side != null || capability != ForgeCapabilities.ITEM_HANDLER) {
            return;
        }

        BambooTrayBlockEntity tray = (BambooTrayBlockEntity) (Object) this;
        cir.setReturnValue(tray.getCapability(capability, Direction.UP));
    }
}
