package com.sshakusora.kaleidoscope_contraption.api.placement;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

public final class ContraptionPlacementManager {
    private ContraptionPlacementManager() {
    }

    public static boolean tryPlace(ResourceLocation pointType, Player player, InteractionHand hand,
                                   BlockPos supportPos, AbstractContraptionEntity entity) {
        StructureTemplate.StructureBlockInfo supportInfo = entity.getContraption().getBlocks().get(supportPos);
        if (supportInfo == null) {
            return false;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        ContraptionPlacementContext context = new ContraptionPlacementContext(
                pointType, player, hand, heldItem, supportPos, supportPos.above(), supportInfo, entity);
        Optional<ContraptionPlacementRegistry.RegisteredRule> matchingRule =
                ContraptionPlacementRegistry.find(context);
        if (matchingRule.isEmpty()) {
            return false;
        }

        ContraptionPlacementRegistry.RegisteredRule registeredRule = matchingRule.get();
        ContraptionPlacementRule rule = registeredRule.rule();
        ContraptionPlacementResult result = rule.createPlacement(context);
        if (!ContraptionPlacementTransaction.canPlace(context, result)) {
            return false;
        }
        if (entity.level().isClientSide) {
            return true;
        }
        if (!ContraptionPlacementTransaction.commit(context, registeredRule.ruleId(), result)) {
            return false;
        }
        rule.afterPlaced(context, result);
        return true;
    }
}
