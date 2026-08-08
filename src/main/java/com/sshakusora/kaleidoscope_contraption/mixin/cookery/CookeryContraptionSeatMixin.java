package com.sshakusora.kaleidoscope_contraption.mixin.cookery;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.movement.CookerySeatSupport;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps Cookery's original passenger height while using Create's seat mapping. */
@Mixin(AbstractContraptionEntity.class)
public abstract class CookeryContraptionSeatMixin {
    @Inject(method = "handlePlayerInteraction", at = @At("HEAD"), cancellable = true, remap = false)
    private void kaleidoscopeContraption$handleCookerySeatRemoval(Player player, BlockPos localPos,
                                                                    Direction side, InteractionHand interactionHand,
                                                                    CallbackInfoReturnable<Boolean> cir) {
        if (interactionHand != InteractionHand.MAIN_HAND
                || !KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            return;
        }

        AbstractContraptionEntity contraptionEntity = (AbstractContraptionEntity) (Object) this;
        if (contraptionEntity.getContraption() == null) {
            return;
        }

        StructureTemplate.StructureBlockInfo info =
                contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null || !CookerySeatSupport.isCookerySeat(info.state())) {
            return;
        }

        MovingInteractionBehaviour interaction = contraptionEntity.getContraption()
                .getInteractors().get(localPos);
        if (interaction == null) {
            return;
        }

        cir.setReturnValue(interaction.handlePlayerInteraction(
                player, interactionHand, localPos, contraptionEntity));
    }

    @Inject(method = "positionRider", at = @At("HEAD"), cancellable = true)
    private void kaleidoscopeContraption$positionCookeryPassenger(Entity passenger,
                                                                   Entity.MoveFunction callback,
                                                                   CallbackInfo ci) {
        AbstractContraptionEntity contraptionEntity = (AbstractContraptionEntity) (Object) this;
        if (passenger.getVehicle() != contraptionEntity) {
            return;
        }

        Vec3 position = CookerySeatSupport.getPassengerPosition(contraptionEntity, passenger, 1.0F);
        if (position == null) {
            return;
        }

        callback.accept(passenger, position.x, position.y, position.z);
        ci.cancel();
    }

    /** Also fixes the position Create stores when a passenger actively dismounts. */
    @Inject(method = "getPassengerPosition", at = @At("HEAD"), cancellable = true, remap = false)
    private void kaleidoscopeContraption$getCookeryPassengerPosition(Entity passenger,
                                                                      float partialTicks,
                                                                      CallbackInfoReturnable<Vec3> cir) {
        AbstractContraptionEntity contraptionEntity = (AbstractContraptionEntity) (Object) this;
        if (contraptionEntity.getContraption() == null) {
            return;
        }

        Vec3 position = CookerySeatSupport.getPassengerPosition(contraptionEntity, passenger, partialTicks);
        if (position != null) {
            cir.setReturnValue(position);
        }
    }
}
