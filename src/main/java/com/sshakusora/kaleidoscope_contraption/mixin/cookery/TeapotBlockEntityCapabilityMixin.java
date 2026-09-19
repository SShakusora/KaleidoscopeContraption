package com.sshakusora.kaleidoscope_contraption.mixin.cookery;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity;
import com.sshakusora.kaleidoscope_contraption.registry.KCCookeryCapabilitySupport;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes Cookery's read-only teapot input compatible with Create mounted storage. */
@Mixin(value = TeapotBlockEntity.class, remap = false)
public abstract class TeapotBlockEntityCapabilityMixin {
    @Inject(method = "getCapability", at = @At("HEAD"), cancellable = true, remap = false)
    private <T> void kaleidoscopeContraption$exposeModifiableHandler(
            Capability<T> capability, @Nullable Direction side,
            CallbackInfoReturnable<LazyOptional<T>> cir) {
        if (side != null || capability != ForgeCapabilities.ITEM_HANDLER) {
            return;
        }

        TeapotBlockEntity teapot = (TeapotBlockEntity) (Object) this;
        cir.setReturnValue(LazyOptional.of(() -> KCCookeryCapabilitySupport.teapotHandler(teapot)).cast());
    }
}
