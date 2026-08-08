package com.sshakusora.kaleidoscope_contraption.registry;

import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.ChalkboardBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.SandwichBoardBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.properties.PositionType;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

/** Create attachment checks for Tavern's multi-block boards. */
public final class KCTavernBlockMovementChecks {
    private KCTavernBlockMovementChecks() {
    }

    public static void registerDefaults() {
        BlockMovementChecks.registerAttachedCheck(KCTavernBlockMovementChecks::isAttached);
        BlockMovementChecks.registerBrittleCheck(state -> {
            if (state.getBlock() instanceof SandwichBoardBlock
                    || state.getBlock() instanceof ChalkboardBlock) {
                return BlockMovementChecks.CheckResult.SUCCESS;
            }
            return BlockMovementChecks.CheckResult.PASS;
        });
    }

    private static BlockMovementChecks.CheckResult isAttached(BlockState state, Level level,
                                                              BlockPos pos, Direction direction) {
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
}
