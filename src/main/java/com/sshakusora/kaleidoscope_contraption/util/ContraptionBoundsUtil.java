package com.sshakusora.kaleidoscope_contraption.util;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.bearing.ClockworkContraption;
import com.simibubi.create.content.contraptions.mounted.MountedContraption;
import com.sshakusora.kaleidoscope_contraption.mixin.accessor.ClockworkContraptionAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;

/**
 * Contraption bounds 计算工具类
 * 用于在动态添加/移除方块时重新计算Contraption的bounds
 * 支持所有类型的Contraption（旋转类、平移类等）
 */
public class ContraptionBoundsUtil {

    /**
     * 重新计算Contraption的bounds
     * 根据Contraption类型自动选择合适的计算方式：
     * - BearingContraption, ClockworkContraption：保持轴向扩展
     * - MountedContraption：Y轴扩展
     * - 其他Contraption：简单的minmax计算
     *
     * @param contraption 需要重新计算bounds的Contraption
     * @return 更新后的bounds
     */
    public static AABB recalculateBounds(Contraption contraption) {
        // 1. 首先计算所有方块的minmax bounds
        AABB newBounds = calculateMinMaxBounds(contraption);

        // 2. 根据Contraption类型进行特殊处理
        if (contraption instanceof BearingContraption bearingContraption) {
            // BearingContraption：绕轴旋转，需要圆形bounds
            Direction.Axis axis = bearingContraption.getFacing().getAxis();
            newBounds = expandBoundsAroundAxis(newBounds, contraption.getBlocks().keySet(), axis);
        } else if (contraption instanceof ClockworkContraption clockworkContraption) {
            // ClockworkContraption：也是绕轴旋转
            Direction.Axis axis = ((ClockworkContraptionAccessor) clockworkContraption).getFacing().getAxis();
            if (axis != null) {
                newBounds = expandBoundsAroundAxis(newBounds, contraption.getBlocks().keySet(), axis);
            }
        } else if (contraption instanceof MountedContraption) {
            // MountedContraption：Y轴扩展（类似expandBoundsAroundAxis(Axis.Y)）
            newBounds = expandBoundsAroundAxis(newBounds, contraption.getBlocks().keySet(), Direction.Axis.Y);
        }
        // 其他Contraption类型（TranslatingContraption的子类如GantryContraption, PistonContraption, PulleyContraption等）
        // 使用简单的minmax bounds即可，因为它们主要是直线运动

        contraption.bounds = newBounds;
        return newBounds;
    }

    /**
     * 计算所有方块的minmax bounds
     */
    private static AABB calculateMinMaxBounds(Contraption contraption) {
        AABB newBounds = new AABB(BlockPos.ZERO);
        for (BlockPos pos : contraption.getBlocks().keySet()) {
            newBounds = newBounds.minmax(new AABB(pos));
        }
        return newBounds;
    }

    /**
     * 参考Contraption.expandBoundsAroundAxis的实现
     * 为旋转Contraption扩展bounds以容纳旋转时的空间需求
     *
     * @param bounds 当前的bounds
     * @param blocks 所有方块位置
     * @param axis 旋转轴
     * @return 扩展后的bounds
     */
    private static AABB expandBoundsAroundAxis(AABB bounds, Iterable<? extends Vec3i> blocks, Direction.Axis axis) {
        int radius = (int) (Math.ceil(getRadius(blocks, axis)));

        int maxX = radius + 2;
        int maxY = radius + 2;
        int maxZ = radius + 2;
        int minX = -radius - 1;
        int minY = -radius - 1;
        int minZ = -radius - 1;

        if (axis == Direction.Axis.X) {
            maxX = (int) bounds.maxX;
            minX = (int) bounds.minX;
        } else if (axis == Direction.Axis.Y) {
            maxY = (int) bounds.maxY;
            minY = (int) bounds.minY;
        } else if (axis == Direction.Axis.Z) {
            maxZ = (int) bounds.maxZ;
            minZ = (int) bounds.minZ;
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * 参考Contraption.getRadius的实现
     * 计算所有方块在垂直于指定轴的平面上的最大半径
     *
     * @param blocks 所有方块位置
     * @param axis 旋转轴
     * @return 最大半径
     */
    private static double getRadius(Iterable<? extends Vec3i> blocks, Direction.Axis axis) {
        Direction.Axis axisA;
        Direction.Axis axisB;

        switch (axis) {
            case X -> {
                axisA = Direction.Axis.Y;
                axisB = Direction.Axis.Z;
            }
            case Y -> {
                axisA = Direction.Axis.X;
                axisB = Direction.Axis.Z;
            }
            case Z -> {
                axisA = Direction.Axis.X;
                axisB = Direction.Axis.Y;
            }
            default -> throw new IllegalStateException("Unexpected value: " + axis);
        }

        int maxDistSq = 0;
        for (Vec3i vec : blocks) {
            int a = vec.get(axisA);
            int b = vec.get(axisB);

            int distSq = a * a + b * b;

            if (distSq > maxDistSq)
                maxDistSq = distSq;
        }

        return Math.sqrt(maxDistSq);
    }
}
