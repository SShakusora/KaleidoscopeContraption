package com.sshakusora.kaleidoscope_contraption.registry;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.ShawarmaSpitBlock;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
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

        // 防止沙威玛烤架在Contraption中disassemble时消失
        BlockMovementChecks.registerBrittleCheck(state -> {
            if (state.getBlock() instanceof ShawarmaSpitBlock) {
                return BlockMovementChecks.CheckResult.SUCCESS;
            }
            return BlockMovementChecks.CheckResult.PASS;
        });
    }
}
