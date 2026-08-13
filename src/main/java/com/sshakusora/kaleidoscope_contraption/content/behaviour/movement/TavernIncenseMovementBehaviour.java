package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.IncenseBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModParticles;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionWorld;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.TavernBrewingContraptionSupport;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionDataUtil;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Runs Tavern incense redstone, particles, and undead effects while in motion.
 */
public final class TavernIncenseMovementBehaviour implements MovementBehaviour {
    @Override
    public void tick(MovementContext context) {
        if (context.contraption.entity == null) {
            return;
        }
        StructureTemplate.StructureBlockInfo info =
                context.contraption.getBlocks().get(context.localPos);
        if (info == null || !(info.state().getBlock() instanceof IncenseBlock)) {
            return;
        }

        if (context.world.isClientSide) {
            spawnParticles(context, info.state());
            return;
        }

        BlockState state = updateRedstone(context, info);
        if (state.getValue(BlockStateProperties.OPEN)
                && context.world.getGameTime() % 120 == 0) {
            applyUndeadEffect(context);
        }
    }

    private BlockState updateRedstone(MovementContext context,
                                      StructureTemplate.StructureBlockInfo info) {
        BlockState state = info.state();
        boolean signal = hasSignal(context);
        if (signal == state.getValue(BlockStateProperties.POWERED)) {
            return state;
        }

        BlockState updated = state
                .setValue(BlockStateProperties.POWERED, signal)
                .setValue(BlockStateProperties.OPEN, signal);
        ContraptionDataUtil.updateContraptionData(context, updated,
                info.nbt() == null ? null : info.nbt().copy(), true);
        if (state.getValue(BlockStateProperties.OPEN) != signal) {
            ContraptionInteractionUtil.playSound(context.contraption.entity, context.localPos,
                    signal ? SoundEvents.STONE_BUTTON_CLICK_ON : SoundEvents.STONE_BUTTON_CLICK_OFF,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return updated;
    }

    private boolean hasSignal(MovementContext context) {
        ContraptionWorld localWorld = new ContraptionWorld(context.world, context.contraption);
        if (localWorld.hasNeighborSignal(context.localPos)) {
            return true;
        }
        AbstractContraptionEntity entity = context.contraption.entity;
        Vec3 global = TavernBrewingContraptionSupport.globalCenter(entity, context.localPos);
        return context.world.hasNeighborSignal(BlockPos.containing(global));
    }

    private void applyUndeadEffect(MovementContext context) {
        AbstractContraptionEntity entity = context.contraption.entity;
        Vec3 global = TavernBrewingContraptionSupport.globalCenter(entity, context.localPos);
        AABB area = new AABB(BlockPos.containing(global)).inflate(32);
        for (LivingEntity living : context.world.getEntitiesOfClass(
                LivingEntity.class, area,
                candidate -> candidate.getType().is(EntityTypeTags.UNDEAD) && candidate.isAlive())) {
            living.hurt(context.world.damageSources().magic(), 1.0F);
            if (living instanceof ZombieVillager zombieVillager
                    && zombieVillager.getHealth() <= 1.0F) {
                zombieVillager.startConverting(null, 60);
            }
        }
    }

    private void spawnParticles(MovementContext context, BlockState state) {
        ParticleSet particles = getParticles(state);
        if (particles == null) {
            return;
        }
        RandomSource random = context.world.random;
        Vec3 global = TavernBrewingContraptionSupport.globalCenter(
                context.contraption.entity, context.localPos);

        if (random.nextInt(3) == 0) {
            context.world.addParticle(particles.small(),
                    global.x, global.y, global.z,
                    random.nextGaussian() * 0.01,
                    0.02 + random.nextDouble() * 0.01,
                    random.nextGaussian() * 0.01);
        }
        if (!state.getValue(BlockStateProperties.OPEN)) {
            return;
        }
        for (int i = 0; i < 5; i++) {
            context.world.addParticle(particles.large(),
                    global.x + (random.nextDouble() - 0.5) * 32,
                    global.y + particles.largeYOffset()
                            + random.nextDouble() * particles.largeYRange(),
                    global.z + (random.nextDouble() - 0.5) * 32,
                    0, 0, 0);
        }
    }

    private ParticleSet getParticles(BlockState state) {
        if (state.is(ModBlocks.SAKURA_INCENSE.get())) {
            return new ParticleSet(ModParticles.SAKURA_INCENSE_PARTICLE.get(),
                    ParticleTypes.CHERRY_LEAVES, -2, 16);
        }
        if (state.is(ModBlocks.PINE_INCENSE.get())) {
            return new ParticleSet(ModParticles.PINE_INCENSE_PARTICLE.get(),
                    ModParticles.PINE_INCENSE_LARGE_PARTICLE.get(), -2, 16);
        }
        if (state.is(ModBlocks.GINKGO_INCENSE.get())) {
            return new ParticleSet(ModParticles.GINKGO_INCENSE_PARTICLE.get(),
                    ModParticles.GINKGO_INCENSE_LARGE_PARTICLE.get(), -2, 16);
        }
        if (state.is(ModBlocks.SPORE_INCENSE.get())) {
            return new ParticleSet(ModParticles.SPORE_INCENSE_PARTICLE.get(),
                    ParticleTypes.SPORE_BLOSSOM_AIR, -2, 16);
        }
        if (state.is(ModBlocks.CATNIP_INCENSE.get())) {
            return new ParticleSet(ModParticles.CATNIP_INCENSE_PARTICLE.get(),
                    ModParticles.CATNIP_INCENSE_LARGE_PARTICLE.get(), -2, 16);
        }
        if (state.is(ModBlocks.SNOW_INCENSE.get())) {
            return new ParticleSet(ModParticles.SNOW_INCENSE_PARTICLE.get(),
                    ModParticles.SNOW_INCENSE_LARGE_PARTICLE.get(), -2, 16);
        }
        if (state.is(ModBlocks.BUTTERFLY_INCENSE.get())) {
            return new ParticleSet(ModParticles.BUTTERFLY_INCENSE_PARTICLE.get(),
                    ModParticles.BUTTERFLY_INCENSE_LARGE_PARTICLE.get(), -2, 16);
        }
        if (state.is(ModBlocks.FIREFLY_INCENSE.get())) {
            return new ParticleSet(ModParticles.FIREFLY_INCENSE_PARTICLE.get(),
                    ModParticles.FIREFLY_INCENSE_LARGE_PARTICLE.get(), -0.67, 5.33);
        }
        return null;
    }

    private record ParticleSet(ParticleOptions small, ParticleOptions large,
                               double largeYOffset, double largeYRange) {
    }
}
