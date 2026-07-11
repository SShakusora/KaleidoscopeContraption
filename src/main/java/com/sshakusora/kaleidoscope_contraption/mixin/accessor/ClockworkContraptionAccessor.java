package com.sshakusora.kaleidoscope_contraption.mixin.accessor;

import com.simibubi.create.content.contraptions.bearing.ClockworkContraption;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClockworkContraption.class)
public interface ClockworkContraptionAccessor {
    @Accessor("facing")
    Direction getFacing();
}
