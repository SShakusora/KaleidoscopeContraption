package com.sshakusora.kaleidoscope_contraption.mixin.accessor;

import com.simibubi.create.content.contraptions.bearing.ClockworkContraption;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ClockworkContraption.class, remap = false)
public interface ClockworkContraptionAccessor {
    @Accessor(value = "facing", remap = false)
    Direction getFacing();
}
