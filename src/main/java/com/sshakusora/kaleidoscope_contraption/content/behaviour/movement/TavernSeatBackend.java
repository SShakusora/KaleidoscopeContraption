package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.BarStoolBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.SofaBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.deco.BarStoolBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.entity.SitEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopetavern.init.tag.TagMod;
import com.sshakusora.kaleidoscope_contraption.api.seat.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

/** Tavern implementation of the unified contraption seat backend. */
public final class TavernSeatBackend implements ContraptionSeatBackend {
    private static final double SOFA_SEAT_HEIGHT = 0.5125;
    private static final double BAR_STOOL_SEAT_HEIGHT = 0.875;

    public static final TavernSeatBackend INSTANCE = new TavernSeatBackend();

    private static final ContraptionSeatProvider SOFA_PROVIDER = new ContraptionSeatProvider() {
        @Override
        public double getSeatHeight(BlockState state) {
            return SOFA_SEAT_HEIGHT;
        }

        @Override
        public Direction getSeatFacing(BlockState state) {
            return state.getValue(SofaBlock.FACING);
        }
    };

    private static final ContraptionSeatProvider BAR_STOOL_PROVIDER = new ContraptionSeatProvider() {
        @Override
        public double getSeatHeight(BlockState state) {
            return BAR_STOOL_SEAT_HEIGHT;
        }

        @Override
        public Direction getSeatFacing(BlockState state) {
            return state.getValue(BarStoolBlock.FACING);
        }
    };

    private TavernSeatBackend() {
    }

    /** Registers the backend and Tavern's built-in seat definitions. */
    public static void registerDefaults() {
        ContraptionSeatBackends.register(ContraptionSeatBackends.TAVERN, INSTANCE);
        registerSofaDefaults();
        registerBarStoolDefaults();
    }

    private static void registerSofaDefaults() {
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.WHITE_SOFA.get(), ContraptionSeatBackends.TAVERN, SOFA_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.LIGHT_GRAY_SOFA.get(), ContraptionSeatBackends.TAVERN, SOFA_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.GRAY_SOFA.get(), ContraptionSeatBackends.TAVERN, SOFA_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.BLACK_SOFA.get(), ContraptionSeatBackends.TAVERN, SOFA_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.BROWN_SOFA.get(), ContraptionSeatBackends.TAVERN, SOFA_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.RED_SOFA.get(), ContraptionSeatBackends.TAVERN, SOFA_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.ORANGE_SOFA.get(), ContraptionSeatBackends.TAVERN, SOFA_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.YELLOW_SOFA.get(), ContraptionSeatBackends.TAVERN, SOFA_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.LIME_SOFA.get(), ContraptionSeatBackends.TAVERN, SOFA_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.GREEN_SOFA.get(), ContraptionSeatBackends.TAVERN, SOFA_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.CYAN_SOFA.get(), ContraptionSeatBackends.TAVERN, SOFA_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.LIGHT_BLUE_SOFA.get(), ContraptionSeatBackends.TAVERN, SOFA_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.BLUE_SOFA.get(), ContraptionSeatBackends.TAVERN, SOFA_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.PURPLE_SOFA.get(), ContraptionSeatBackends.TAVERN, SOFA_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.MAGENTA_SOFA.get(), ContraptionSeatBackends.TAVERN, SOFA_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.PINK_SOFA.get(), ContraptionSeatBackends.TAVERN, SOFA_PROVIDER);
    }

    private static void registerBarStoolDefaults() {
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.WHITE_BAR_STOOL.get(), ContraptionSeatBackends.TAVERN, BAR_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.LIGHT_GRAY_BAR_STOOL.get(), ContraptionSeatBackends.TAVERN, BAR_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.GRAY_BAR_STOOL.get(), ContraptionSeatBackends.TAVERN, BAR_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.BLACK_BAR_STOOL.get(), ContraptionSeatBackends.TAVERN, BAR_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.BROWN_BAR_STOOL.get(), ContraptionSeatBackends.TAVERN, BAR_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.RED_BAR_STOOL.get(), ContraptionSeatBackends.TAVERN, BAR_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.ORANGE_BAR_STOOL.get(), ContraptionSeatBackends.TAVERN, BAR_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.YELLOW_BAR_STOOL.get(), ContraptionSeatBackends.TAVERN, BAR_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.LIME_BAR_STOOL.get(), ContraptionSeatBackends.TAVERN, BAR_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.GREEN_BAR_STOOL.get(), ContraptionSeatBackends.TAVERN, BAR_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.CYAN_BAR_STOOL.get(), ContraptionSeatBackends.TAVERN, BAR_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.LIGHT_BLUE_BAR_STOOL.get(), ContraptionSeatBackends.TAVERN, BAR_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.BLUE_BAR_STOOL.get(), ContraptionSeatBackends.TAVERN, BAR_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.PURPLE_BAR_STOOL.get(), ContraptionSeatBackends.TAVERN, BAR_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.MAGENTA_BAR_STOOL.get(), ContraptionSeatBackends.TAVERN, BAR_STOOL_PROVIDER);
        ContraptionSeatRegistry.registerDefault(
                ModBlocks.PINK_BAR_STOOL.get(), ContraptionSeatBackends.TAVERN, BAR_STOOL_PROVIDER);
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

    @Override
    public void onSeatEntityRestored(SeatRestoreContext context) {
        if (context.level().getBlockEntity(context.worldPos()) instanceof BarStoolBlockEntity barStool
                && context.seatEntity() instanceof SitEntity sitEntity) {
            barStool.setSitEntity(sitEntity);
        }
    }
}
