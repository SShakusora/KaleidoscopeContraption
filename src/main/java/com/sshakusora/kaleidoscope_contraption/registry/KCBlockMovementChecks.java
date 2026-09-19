package com.sshakusora.kaleidoscope_contraption.registry;

import com.github.ysbbbbbb.kaleidoscopecookery.block.crop.TeaTreeBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.EightImmortalsTableBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.LongBenchBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.TeaBannerBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteOneByTwoBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.BambooTrayBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.ShawarmaSpitBlock;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Create方块移动检查注册类
 * <p>
 * 用于注册Contraption组装时的方块附着检查，确保多部件方块能正确整体移动。
 * <p>
 * <b>日后如需继续注册Check，请在此类中添加：</b>
 * <ul>
 *     <li>{@link BlockMovementChecks#registerAttachedCheck(BlockMovementChecks.AttachedCheck)} - 附着检查</li>
 *     <li>{@link BlockMovementChecks#registerMovementAllowedCheck(BlockMovementChecks.MovementAllowedCheck)} - 移动允许检查</li>
 *     <li>{@link BlockMovementChecks#registerMovementNecessaryCheck(BlockMovementChecks.MovementNecessaryCheck)} - 移动必要检查</li>
 *     <li>{@link BlockMovementChecks#registerBrittleCheck(BlockMovementChecks.BrittleCheck)} - 易碎检查</li>
 *     <li>{@link BlockMovementChecks#registerNotSupportiveCheck(BlockMovementChecks.NotSupportiveCheck)} - 非支撑检查</li>
 * </ul>
 */
public class KCBlockMovementChecks {

    public static void registerDefaults() {
        // 注册沙威玛烤架的附着检查（类似原版门，上下两部分互相附着）
        BlockMovementChecks.registerAttachedCheck((state, world, pos, direction) -> {
            Block block = state.getBlock();
            // 检查是否为沙威玛烤架方块
            if (block instanceof ShawarmaSpitBlock) {
                DoubleBlockHalf half = state.getValue(ShawarmaSpitBlock.HALF);
                // 下半部分附着于上半部分（上方）
                if (half == DoubleBlockHalf.LOWER && direction == Direction.UP) {
                    return BlockMovementChecks.CheckResult.SUCCESS;
                }
                // 上半部分附着于下半部分（下方）
                if (half == DoubleBlockHalf.UPPER && direction == Direction.DOWN) {
                    return BlockMovementChecks.CheckResult.SUCCESS;
                }
                return BlockMovementChecks.CheckResult.FAIL;
            }
            return BlockMovementChecks.CheckResult.PASS; // 让其他检查器处理
        });

        // A tray placed on another tray uses STAND to describe the support link.
        BlockMovementChecks.registerAttachedCheck((state, world, pos, direction) -> {
            if (!(state.getBlock() instanceof BambooTrayBlock)) {
                return BlockMovementChecks.CheckResult.PASS;
            }
            if (direction == Direction.DOWN && state.getValue(BambooTrayBlock.STAND)) {
                return BlockMovementChecks.CheckResult.SUCCESS;
            }
            if (direction == Direction.UP) {
                BlockState above = world.getBlockState(pos.relative(Direction.UP));
                if (above.getBlock() instanceof BambooTrayBlock
                        && above.getValue(BambooTrayBlock.STAND)) {
                    return BlockMovementChecks.CheckResult.SUCCESS;
                }
            }
            return BlockMovementChecks.CheckResult.PASS;
        });

        // Eight Immortals Tables are four directional parts that must move as one.
        BlockMovementChecks.registerMovementAllowedCheck((state, world, pos) ->
                state.getBlock() instanceof EightImmortalsTableBlock
                        ? BlockMovementChecks.CheckResult.SUCCESS
                        : BlockMovementChecks.CheckResult.PASS);
        BlockMovementChecks.registerAttachedCheck((state, world, pos, direction) -> {
            if (!(state.getBlock() instanceof EightImmortalsTableBlock)) {
                return BlockMovementChecks.CheckResult.PASS;
            }
            EightImmortalsTableBlock.Part part = state.getValue(EightImmortalsTableBlock.PART);
            Direction facing = state.getValue(EightImmortalsTableBlock.FACING);
            Direction left = facing.getCounterClockWise();
            boolean attached = switch (part) {
                case RIGHT_BOTTOM -> direction == left || direction == facing;
                case LEFT_BOTTOM -> direction == facing.getClockWise() || direction == facing;
                case RIGHT_TOP -> direction == facing.getOpposite() || direction == left;
                case LEFT_TOP -> direction == facing.getOpposite() || direction == facing.getClockWise();
            };
            return attached ? BlockMovementChecks.CheckResult.SUCCESS : BlockMovementChecks.CheckResult.PASS;
        });

        // Tea banners can be attached to a floor or a wall inside a contraption.
        BlockMovementChecks.registerAttachedCheck((state, world, pos, direction) -> {
            if (!(state.getBlock() instanceof TeaBannerBlock)) {
                return BlockMovementChecks.CheckResult.PASS;
            }
            AttachFace face = state.getValue(TeaBannerBlock.FACE);
            Direction support = switch (face) {
                case FLOOR -> Direction.DOWN;
                case CEILING -> Direction.UP;
                case WALL -> state.getValue(TeaBannerBlock.FACING).getOpposite();
            };
            return direction == support
                    ? BlockMovementChecks.CheckResult.SUCCESS
                    : BlockMovementChecks.CheckResult.PASS;
        });

        // Long bench segments connect along their horizontal axis and position.
        BlockMovementChecks.registerAttachedCheck((state, world, pos, direction) -> {
            if (!(state.getBlock() instanceof LongBenchBlock)) {
                return BlockMovementChecks.CheckResult.PASS;
            }
            Direction.Axis axis = state.getValue(LongBenchBlock.AXIS);
            if (direction.getAxis() != axis) {
                return BlockMovementChecks.CheckResult.PASS;
            }
            BlockState neighbour = world.getBlockState(pos.relative(direction));
            if (!(neighbour.getBlock() instanceof LongBenchBlock)
                    || neighbour.getValue(LongBenchBlock.AXIS) != axis) {
                return BlockMovementChecks.CheckResult.FAIL;
            }

            Direction negative = axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
            int position = state.getValue(LongBenchBlock.POSITION);
            int neighbourPosition = neighbour.getValue(LongBenchBlock.POSITION);
            boolean valid = direction == negative
                    ? (position == LongBenchBlock.LEFT || position == LongBenchBlock.MIDDLE)
                        && (neighbourPosition == LongBenchBlock.RIGHT
                            || neighbourPosition == LongBenchBlock.MIDDLE)
                    : (position == LongBenchBlock.RIGHT || position == LongBenchBlock.MIDDLE)
                        && (neighbourPosition == LongBenchBlock.LEFT
                            || neighbourPosition == LongBenchBlock.MIDDLE);
            return valid ? BlockMovementChecks.CheckResult.SUCCESS : BlockMovementChecks.CheckResult.FAIL;
        });

        // 防止沙威玛烤架和多方快食物在Contraption中disassemble时消失
        BlockMovementChecks.registerBrittleCheck(state -> {
            if (state.getBlock() instanceof ShawarmaSpitBlock
                    || state.getBlock() instanceof FoodBiteOneByTwoBlock
                    || state.getBlock() instanceof TeaTreeBlock
                    || state.getBlock() instanceof EightImmortalsTableBlock
                    || state.getBlock() instanceof TeaBannerBlock) {
                return BlockMovementChecks.CheckResult.SUCCESS;
            }
            return BlockMovementChecks.CheckResult.PASS;
        });
    }
}
