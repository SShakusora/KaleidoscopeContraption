package com.sshakusora.kaleidoscope_contraption.mixin.accessor;

import com.simibubi.create.content.contraptions.TranslatingContraption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(value = TranslatingContraption.class, remap = false)
public interface TranslatingContraptionAccessor {
    @Accessor(value = "cachedColliders", remap = false)
    void setCachedColliders(Set<BlockPos> cachedColliders);

    @Accessor(value = "cachedColliderDirection", remap = false)
    void setCachedColliderDirection(Direction cachedColliderDirection);
}
