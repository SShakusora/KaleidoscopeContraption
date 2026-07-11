package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.misc.TrashCanBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.entity.SitEntity;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TrashCanBlockMovementBehaviour implements MovementBehaviour {
    @Override
    public void tick(MovementContext context) {
        if (!context.world.isClientSide || context.contraption.entity == null) return;
        if (context.contraption.getBlockEntityClientSide(context.localPos) instanceof TrashCanBlockEntity blockEntity) {
            tickClientAnimations(context, blockEntity);
        }
    }

    private void tickClientAnimations(MovementContext context, TrashCanBlockEntity blockEntity) {
        long offset = context.world.getGameTime() + context.localPos.hashCode();
        if (Math.floorMod(offset, 5) != 0 && Math.floorMod(offset, 61) != 0) {
            return;
        }

        Vec3 center = context.contraption.entity.toGlobalVector(Vec3.atCenterOf(context.localPos), 1);
        AABB bounds = new AABB(center.x - 0.5, center.y - 0.5, center.z - 0.5,
                center.x + 0.5, center.y + 0.5, center.z + 0.5);
        boolean occupied = !context.world.getEntitiesOfClass(SitEntity.class, bounds).isEmpty();

        if (Math.floorMod(offset, 61) == 0 && occupied) {
            if (context.world.random.nextBoolean()) {
                blockEntity.player2State.stop();
                blockEntity.player1State.start((int) context.world.getGameTime());
            } else {
                blockEntity.player1State.stop();
                blockEntity.player2State.start((int) context.world.getGameTime());
            }
        }
        if (Math.floorMod(offset, 5) == 0 && !occupied) {
            blockEntity.player1State.stop();
            blockEntity.player2State.stop();
        }
    }
}
