package com.sshakusora.kaleidoscope_contraption.mixin.tavern;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.movement.TavernSeatSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Transfers Tavern passengers to normal static Tavern seats during disassembly. */
@Mixin(value = Contraption.class, remap = false)
public abstract class TavernContraptionDisassemblyMixin {
    @Inject(method = "addPassengersToWorld", at = @At("HEAD"), remap = false)
    private void kaleidoscopeContraption$restoreTavernPassengers(
            Level level, StructureTransform transform, List<Entity> seatedEntities, CallbackInfo ci) {
        if (level.isClientSide) {
            return;
        }

        Contraption contraption = (Contraption) (Object) this;
        List<RestoreRequest> requests = new ArrayList<>();
        for (Entity passenger : new ArrayList<>(seatedEntities)) {
            Integer seatIndex = contraption.getSeatMapping().get(passenger.getUUID());
            if (seatIndex == null || seatIndex < 0 || seatIndex >= contraption.getSeats().size()) {
                continue;
            }

            BlockPos localPos = contraption.getSeats().get(seatIndex);
            StructureTemplate.StructureBlockInfo info = contraption.getBlocks().get(localPos);
            if (info == null) {
                continue;
            }

            BlockState state = transform.apply(info.state());
            if (!TavernSeatSupport.isTavernSeat(state)) {
                continue;
            }
            requests.add(new RestoreRequest(passenger, transform.apply(localPos), state));
        }

        for (RestoreRequest request : requests) {
            TavernSeatSupport.restorePassenger(level, request.worldPos(), request.state(), request.passenger());
        }
    }

    private record RestoreRequest(Entity passenger, BlockPos worldPos, BlockState state) {
    }
}

