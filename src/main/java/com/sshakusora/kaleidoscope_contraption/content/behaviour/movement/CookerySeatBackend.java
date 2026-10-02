package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.ChairBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.CookStoolBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.LongBenchBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.entity.SitEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.tag.TagMod;
import com.sshakusora.kaleidoscope_contraption.api.seat.ContraptionSeatBackend;
import com.sshakusora.kaleidoscope_contraption.api.seat.ContraptionSeatBackends;
import com.sshakusora.kaleidoscope_contraption.api.seat.ContraptionSeatProvider;
import com.sshakusora.kaleidoscope_contraption.api.seat.ContraptionSeatRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

/** Cookery implementation of the unified contraption seat backend. */
public final class CookerySeatBackend implements ContraptionSeatBackend {
    private static final double CHAIR_SEAT_HEIGHT = 0.5125;
    private static final double STOOL_SEAT_HEIGHT = 0.4375;
    private static final double BENCH_SEAT_HEIGHT = 0.5;

    public static final CookerySeatBackend INSTANCE = new CookerySeatBackend();

    private static final ContraptionSeatProvider CHAIR_PROVIDER = new ContraptionSeatProvider() {
        @Override
        public double getSeatHeight(BlockState state) {
            return CHAIR_SEAT_HEIGHT;
        }

        @Override
        public Direction getSeatFacing(BlockState state) {
            return state.getValue(ChairBlock.FACING);
        }
    };

    private static final ContraptionSeatProvider COOK_STOOL_PROVIDER = new ContraptionSeatProvider() {
        @Override
        public double getSeatHeight(BlockState state) {
            return STOOL_SEAT_HEIGHT;
        }

        @Override
        public Direction getSeatFacing(BlockState state) {
            return state.getValue(CookStoolBlock.FACING);
        }
    };

    private static final ContraptionSeatProvider LONG_BENCH_PROVIDER = new ContraptionSeatProvider() {
        @Override
        public double getSeatHeight(BlockState state) {
            return BENCH_SEAT_HEIGHT;
        }

        @Override
        public Direction getSeatFacing(BlockState state) {
            return state.getValue(LongBenchBlock.AXIS) == Direction.Axis.X
                    ? Direction.SOUTH : Direction.EAST;
        }
    };

    private CookerySeatBackend() {
    }

    /** Registers the backend and Cookery's built-in seat definitions. */
    public static void registerDefaults() {
        ContraptionSeatBackends.register(ContraptionSeatBackends.COOKERY, INSTANCE);

        registerChairDefaults();
        registerCookStoolDefaults();
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.LONG_BENCH.get(), ContraptionSeatBackends.COOKERY, LONG_BENCH_PROVIDER);
    }

    private static void registerChairDefaults() {
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.CHAIR_OAK.get(), ContraptionSeatBackends.COOKERY, CHAIR_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.CHAIR_SPRUCE.get(), ContraptionSeatBackends.COOKERY, CHAIR_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.CHAIR_ACACIA.get(), ContraptionSeatBackends.COOKERY, CHAIR_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.CHAIR_BAMBOO.get(), ContraptionSeatBackends.COOKERY, CHAIR_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.CHAIR_BIRCH.get(), ContraptionSeatBackends.COOKERY, CHAIR_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.CHAIR_CHERRY.get(), ContraptionSeatBackends.COOKERY, CHAIR_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.CHAIR_CRIMSON.get(), ContraptionSeatBackends.COOKERY, CHAIR_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.CHAIR_DARK_OAK.get(), ContraptionSeatBackends.COOKERY, CHAIR_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.CHAIR_JUNGLE.get(), ContraptionSeatBackends.COOKERY, CHAIR_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.CHAIR_MANGROVE.get(), ContraptionSeatBackends.COOKERY, CHAIR_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.CHAIR_WARPED.get(), ContraptionSeatBackends.COOKERY, CHAIR_PROVIDER);
    }

    private static void registerCookStoolDefaults() {
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.COOK_STOOL_OAK.get(), ContraptionSeatBackends.COOKERY, COOK_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.COOK_STOOL_SPRUCE.get(), ContraptionSeatBackends.COOKERY, COOK_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.COOK_STOOL_ACACIA.get(), ContraptionSeatBackends.COOKERY, COOK_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.COOK_STOOL_BAMBOO.get(), ContraptionSeatBackends.COOKERY, COOK_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.COOK_STOOL_BIRCH.get(), ContraptionSeatBackends.COOKERY, COOK_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.COOK_STOOL_CHERRY.get(), ContraptionSeatBackends.COOKERY, COOK_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.COOK_STOOL_CRIMSON.get(), ContraptionSeatBackends.COOKERY, COOK_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.COOK_STOOL_DARK_OAK.get(), ContraptionSeatBackends.COOKERY, COOK_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.COOK_STOOL_JUNGLE.get(), ContraptionSeatBackends.COOKERY, COOK_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.COOK_STOOL_MANGROVE.get(), ContraptionSeatBackends.COOKERY, COOK_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.COOK_STOOL_WARPED.get(), ContraptionSeatBackends.COOKERY, COOK_STOOL_PROVIDER);
    }

    @Override
    public Optional<Entity> findPassenger(Level level, AABB bounds) {
        for (SitEntity sitEntity : level.getEntitiesOfClass(SitEntity.class, bounds)) {
            Entity passenger = sitEntity.getFirstPassenger();
            if (passenger != null) {
                return Optional.of(passenger);
            }
        }
        return Optional.empty();
    }

    @Override
    public Entity createSeatEntity(Level level, BlockPos pos, double seatHeight) {
        return new SitEntity(level, pos, seatHeight);
    }

    @Override
    public boolean isValidSupportingBlock(BlockState state) {
        return state.is(TagMod.SITTABLE);
    }
}
