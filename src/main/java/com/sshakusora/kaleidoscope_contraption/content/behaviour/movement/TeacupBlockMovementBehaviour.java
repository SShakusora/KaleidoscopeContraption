package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.github.ysbbbbbb.kaleidoscopecookery.block.drink.TeacupBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModParticles;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

public class TeacupBlockMovementBehaviour implements MovementBehaviour {
    @Override
    public void tick(MovementContext context) {
        if (!(context.world instanceof ServerLevel serverLevel)
                || serverLevel.random.nextInt(20) != 0) {
            return;
        }
        StructureTemplate.StructureBlockInfo info = context.contraption.getBlocks().get(context.localPos);
        if (info == null || !(info.state().getBlock() instanceof TeacupBlock)) {
            return;
        }

        Vec3 center = context.contraption.entity == null
                ? Vec3.atCenterOf(context.localPos)
                : context.contraption.entity.toGlobalVector(Vec3.atCenterOf(context.localPos), 1.0F);
        RandomSource random = serverLevel.random;
        serverLevel.sendParticles(ModParticles.COOKING.get(),
                center.x + random.nextDouble() / 3 * (random.nextBoolean() ? 1 : -1),
                center.y + random.nextDouble() / 3,
                center.z + random.nextDouble() / 3 * (random.nextBoolean() ? 1 : -1),
                1, 0.3, 0.1, 0.3, 0);
    }
}
