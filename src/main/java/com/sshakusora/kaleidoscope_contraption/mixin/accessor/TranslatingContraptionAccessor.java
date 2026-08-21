package com.sshakusora.kaleidoscope_contraption.mixin.accessor;

import com.simibubi.create.content.contraptions.TranslatingContraption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(TranslatingContraption.class)
public interface TranslatingContraptionAccessor {
    @Accessor("cachedColliders")
    void setCachedColliders(Set<BlockPos> cachedColliders);

    @Accessor("cachedColliderDirection")
    void setCachedColliderDirection(Direction cachedColliderDirection);
}
