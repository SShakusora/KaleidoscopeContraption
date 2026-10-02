package com.sshakusora.kaleidoscope_contraption.mixin;

import com.simibubi.create.content.contraptions.Contraption;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.movement.ContraptionSeatSupport;
import com.sshakusora.kaleidoscope_contraption.mixin.accessor.ContraptionAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures passengers for every registered backend before assembly. */
@Mixin(value = Contraption.class, remap = false)
public abstract class ContraptionSeatAssemblyMixin {
    @Inject(method = "addBlock", at = @At("TAIL"), remap = false)
    private void kaleidoscopeContraption$captureSeatPassenger(
            Level level, BlockPos pos, Pair<StructureTemplate.StructureBlockInfo, BlockEntity> pair,
            CallbackInfo ci) {
        if (!ContraptionSeatSupport.isSeat(pair.getLeft().state())) {
            return;
        }

        Entity passenger = ContraptionSeatSupport
                .findStaticPassenger(level, pos, pair.getLeft().state())
                .orElse(null);
        if (passenger == null) {
            return;
        }

        Contraption contraption = (Contraption) (Object) this;
        ContraptionAccessor accessor = (ContraptionAccessor) contraption;
        BlockPos localPos = pos.subtract(accessor.getAnchor());
        accessor.getInitialPassengers().put(localPos, passenger);
    }
}
