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

        // 八仙桌主动禁止活塞推动，但 Create 组装时仍应允许它作为整体移动。
        BlockMovementChecks.registerMovementAllowedCheck((state, world, pos) ->
                state.getBlock() instanceof EightImmortalsTableBlock
                        ? BlockMovementChecks.CheckResult.SUCCESS
                        : BlockMovementChecks.CheckResult.PASS);

        // 顶部竹托盘通过 STAND 标记附着到底部竹托盘，保证叠放的托盘整体移动。
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

        // 八仙桌由四个带方向的部件组成，必须整体收集。
        BlockMovementChecks.registerAttachedCheck((state, world, pos, direction) -> {
            if (!(state.getBlock() instanceof EightImmortalsTableBlock)) {
                return BlockMovementChecks.CheckResult.PASS;
            }
            EightImmortalsTableBlock.Part part = state.getValue(EightImmortalsTableBlock.PART);
            Direction facing = state.getValue(EightImmortalsTableBlock.FACING);
            Direction left = facing.getCounterClockWise();
            Direction right = facing.getClockWise();
            boolean attached = switch (part) {
                case RIGHT_BOTTOM -> direction == left || direction == facing;
                case LEFT_BOTTOM -> direction == right || direction == facing;
                case RIGHT_TOP -> direction == facing.getOpposite() || direction == left;
                case LEFT_TOP -> direction == facing.getOpposite() || direction == right;
            };
            return attached ? BlockMovementChecks.CheckResult.SUCCESS : BlockMovementChecks.CheckResult.PASS;
        });

        // Tea banners are face-attached blocks. Keep the support relationship
        // explicit while a contraption is assembled.
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

        // 长凳的连续段沿 AXIS 互相附着，并校验 POSITION 的左右连接关系。
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
