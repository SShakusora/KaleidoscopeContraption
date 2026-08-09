package com.sshakusora.kaleidoscope_contraption.registry;

import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.BarrelBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.PressingTubBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.TapBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.ChalkboardBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.SandwichBoardBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.properties.PositionType;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.Half;

/** Create attachment checks for Tavern's multi-block boards. */
public final class KCTavernBlockMovementChecks {
    private KCTavernBlockMovementChecks() {
    }

    public static void registerDefaults() {
        BlockMovementChecks.registerMovementNecessaryCheck((state, level, pos) ->
                isBrewingDevice(state)
                        ? BlockMovementChecks.CheckResult.SUCCESS
                        : BlockMovementChecks.CheckResult.PASS);
        BlockMovementChecks.registerMovementAllowedCheck((state, level, pos) ->
                isBrewingDevice(state)
                        ? BlockMovementChecks.CheckResult.SUCCESS
                        : BlockMovementChecks.CheckResult.PASS);
        BlockMovementChecks.registerAttachedCheck(KCTavernBlockMovementChecks::isAttached);
        BlockMovementChecks.registerBrittleCheck(state -> {
            if (state.getBlock() instanceof TapBlock) {
                return BlockMovementChecks.CheckResult.SUCCESS;
            }
            if (state.getBlock() instanceof SandwichBoardBlock
                    || state.getBlock() instanceof ChalkboardBlock) {
                return BlockMovementChecks.CheckResult.SUCCESS;
            }
            return BlockMovementChecks.CheckResult.PASS;
        });
    }

    private static BlockMovementChecks.CheckResult isAttached(BlockState state, Level level,
                                                              BlockPos pos, Direction direction) {
        if (state.getBlock() instanceof BarrelBlock) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (neighbor.getBlock() instanceof BarrelBlock
                    && neighbor.getValue(BarrelBlock.FACING) == state.getValue(BarrelBlock.FACING)
                    && BarrelBlock.getOriginPos(pos, state)
                    .equals(BarrelBlock.getOriginPos(pos.relative(direction), neighbor))) {
                return BlockMovementChecks.CheckResult.SUCCESS;
            }
            if (isFrontTapPart(state, direction)
                    && neighbor.getBlock() instanceof TapBlock
                    && neighbor.getValue(TapBlock.FACING) == state.getValue(BarrelBlock.FACING)) {
                return BlockMovementChecks.CheckResult.SUCCESS;
            }
            return BlockMovementChecks.CheckResult.PASS;
        }

        if (state.getBlock() instanceof TapBlock) {
            Direction facing = state.getValue(TapBlock.FACING);
            if (direction != facing.getOpposite()) {
                return BlockMovementChecks.CheckResult.PASS;
            }
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            return isValidTapSource(neighbor, facing)
                    ? BlockMovementChecks.CheckResult.SUCCESS
                    : BlockMovementChecks.CheckResult.PASS;
        }

        if (state.getBlock() instanceof SandwichBoardBlock) {
            Half half = state.getValue(SandwichBoardBlock.HALF);
            if (half == Half.BOTTOM && direction == Direction.UP
                    || half == Half.TOP && direction == Direction.DOWN) {
                BlockState neighbor = level.getBlockState(pos.relative(direction));
                return neighbor.getBlock() == state.getBlock()
                        && neighbor.getValue(SandwichBoardBlock.HALF) != half
                        ? BlockMovementChecks.CheckResult.SUCCESS
                        : BlockMovementChecks.CheckResult.FAIL;
            }
            return BlockMovementChecks.CheckResult.PASS;
        }

        if (!(state.getBlock() instanceof ChalkboardBlock)) {
            return BlockMovementChecks.CheckResult.PASS;
        }

        Half half = state.getValue(ChalkboardBlock.HALF);
        BlockState neighbor = level.getBlockState(pos.relative(direction));
        if (direction.getAxis() == Direction.Axis.Y) {
            if (half != Half.BOTTOM && half != Half.TOP) {
                return BlockMovementChecks.CheckResult.PASS;
            }
            return neighbor.getBlock() == state.getBlock()
                    && neighbor.getValue(ChalkboardBlock.FACING) == state.getValue(ChalkboardBlock.FACING)
                    && neighbor.getValue(ChalkboardBlock.POSITION) == state.getValue(ChalkboardBlock.POSITION)
                    && neighbor.getValue(ChalkboardBlock.HALF) != half
                    ? BlockMovementChecks.CheckResult.SUCCESS
                    : BlockMovementChecks.CheckResult.FAIL;
        }

        Direction facing = state.getValue(ChalkboardBlock.FACING);
        if (direction.getAxis() != facing.getClockWise().getAxis()
                || neighbor.getBlock() != state.getBlock()
                || neighbor.getValue(ChalkboardBlock.FACING) != facing
                || neighbor.getValue(ChalkboardBlock.HALF) != half) {
            return BlockMovementChecks.CheckResult.PASS;
        }

        PositionType position = state.getValue(ChalkboardBlock.POSITION);
        PositionType neighborPosition = neighbor.getValue(ChalkboardBlock.POSITION);
        boolean valid = position == PositionType.MIDDLE
                && ((direction == facing.getClockWise() && neighborPosition == PositionType.LEFT)
                || (direction == facing.getCounterClockWise() && neighborPosition == PositionType.RIGHT));
        valid |= position == PositionType.LEFT && direction == facing.getCounterClockWise()
                && neighborPosition == PositionType.MIDDLE;
        valid |= position == PositionType.RIGHT && direction == facing.getClockWise()
                && neighborPosition == PositionType.MIDDLE;
        return valid ? BlockMovementChecks.CheckResult.SUCCESS : BlockMovementChecks.CheckResult.FAIL;
    }

    private static boolean isBrewingDevice(BlockState state) {
        return state.getBlock() instanceof PressingTubBlock
                || state.getBlock() instanceof BarrelBlock
                || state.getBlock() instanceof TapBlock;
    }

    private static boolean isFrontTapPart(BlockState state, Direction direction) {
        Direction facing = state.getValue(BarrelBlock.FACING);
        return direction == facing && isValidTapSource(state, facing);
    }

    private static boolean isValidTapSource(BlockState state, Direction facing) {
        if (!(state.getBlock() instanceof BarrelBlock)
                || state.getValue(BarrelBlock.FACING) != facing
                || state.getValue(BarrelBlock.LAYER) != AttachFace.WALL) {
            return false;
        }
        int index = state.getValue(BarrelBlock.INDEX);
        return switch (facing) {
            case NORTH -> index == 1;
            case SOUTH -> index == 7;
            case WEST -> index == 3;
            case EAST -> index == 5;
            default -> false;
        };
    }
}
