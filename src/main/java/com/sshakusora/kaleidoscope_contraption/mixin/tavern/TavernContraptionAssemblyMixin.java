package com.sshakusora.kaleidoscope_contraption.mixin.tavern;

import com.github.ysbbbbbb.kaleidoscopetavern.entity.SitEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.movement.TavernSeatSupport;
import com.sshakusora.kaleidoscope_contraption.mixin.accessor.ContraptionAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures Tavern's pre-assembly passenger in Create's native initial-passenger map. */
@Mixin(value = Contraption.class, remap = false)
public abstract class TavernContraptionAssemblyMixin {
    @Inject(method = "addBlock", at = @At("TAIL"), remap = false)
    private void kaleidoscopeContraption$captureTavernPassenger(
            Level level, BlockPos worldPos, Pair<StructureTemplate.StructureBlockInfo, BlockEntity> captured,
            CallbackInfo ci) {
        if (!TavernSeatSupport.isTavernSeat(captured.getLeft().state())) {
            return;
        }

        Entity passenger = null;
        for (SitEntity sitEntity : level.getEntitiesOfClass(SitEntity.class, new AABB(worldPos))) {
            passenger = sitEntity.getFirstPassenger();
            if (passenger != null) {
                break;
            }
        }
        if (passenger == null) {
            return;
        }

        Contraption contraption = (Contraption) (Object) this;
        ContraptionAccessor accessor = (ContraptionAccessor) contraption;
        BlockPos localPos = worldPos.subtract(accessor.getAnchor());
        accessor.getInitialPassengers().put(localPos, passenger);
    }
}
