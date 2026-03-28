package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteThreeByThreeBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.NinePart;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * FoodBiteThreeByThreeBlock 的 Contraption 交互行为
 * 这是一个 3x3 的食物方块，由 NinePart 的 9 个部分组成
 * 交互逻辑需要转发到 CENTER 位置处理
 */
public class FoodBiteThreeByThreeBlockMovingInteraction extends FoodBiteBlockMovingInteraction {

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return false;
        }

        BlockState state = info.state();
        if (!(state.getBlock() instanceof FoodBiteThreeByThreeBlock)) {
            return false;
        }

        // 获取当前部分
        NinePart part = state.getValue(FoodBiteThreeByThreeBlock.PART);

        // 计算 CENTER 位置（主控位置）
        BlockPos centerPos = localPos.subtract(new Vec3i(part.getPosX(), 0, part.getPosY()));

        // 获取 CENTER 位置的方块信息
        StructureTemplate.StructureBlockInfo centerInfo = contraptionEntity.getContraption().getBlocks().get(centerPos);
        if (centerInfo == null || !(centerInfo.state().getBlock() instanceof FoodBiteThreeByThreeBlock)) {
            return false;
        }

        // 使用 CENTER 位置调用父类的交互逻辑
        return super.handlePlayerInteraction(player, activeHand, centerPos, contraptionEntity);
    }
}
