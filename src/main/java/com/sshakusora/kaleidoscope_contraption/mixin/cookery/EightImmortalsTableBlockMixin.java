package com.sshakusora.kaleidoscope_contraption.mixin.cookery;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.EightImmortalsTableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the table's other parts intact while Create removes the assembled blocks. */
@Mixin(value = EightImmortalsTableBlock.class, remap = false)
public abstract class EightImmortalsTableBlockMixin {
    @Inject(method = "onRemove", at = @At("HEAD"), cancellable = true, remap = false)
    private void kaleidoscopeContraption$skipMovingCleanup(BlockState state, Level level, BlockPos pos,
                                                           BlockState newState, boolean isMoving,
                                                           CallbackInfo ci) {
        if (isMoving) {
            ci.cancel();
        }
    }
}
