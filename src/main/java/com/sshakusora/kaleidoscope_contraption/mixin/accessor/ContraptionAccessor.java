package com.sshakusora.kaleidoscope_contraption.mixin.accessor;

import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = Contraption.class, remap = false)
public interface ContraptionAccessor {
    @Accessor("updateTags")
    Map<BlockPos, CompoundTag> getUpdateTags();
}
