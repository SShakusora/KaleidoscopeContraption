package com.sshakusora.kaleidoscope_contraption.api.placement;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

public final class ContraptionRemovalManager {
    public enum Result {
        NOT_REGISTERED,
        REJECTED,
        REMOVED
    }

    private ContraptionRemovalManager() {
    }

    public static Result tryRemove(Player player, BlockPos targetPos, AbstractContraptionEntity entity) {
        StructureTemplate.StructureBlockInfo targetInfo = entity.getContraption().getBlocks().get(targetPos);
        if (targetInfo == null) {
            return Result.REJECTED;
        }

        CompoundTag nbt = targetInfo.nbt();
        ResourceLocation ruleId = nbt == null ? null
                : ResourceLocation.tryParse(nbt.getString(ContraptionPlacementRegistry.PLACEMENT_RULE_TAG));
        if (ruleId == null) {
            return Result.NOT_REGISTERED;
        }

        Optional<ContraptionPlacementRegistry.RegisteredRule> registeredRule =
                ContraptionPlacementRegistry.findById(ruleId);
        if (registeredRule.isEmpty()) {
            return Result.NOT_REGISTERED;
        }

        ContraptionPlacementRule rule = registeredRule.get().rule();
        ContraptionRemovalContext context = new ContraptionRemovalContext(player, targetPos, targetInfo, entity);
        Optional<ContraptionRemovalResult> removal = rule.createRemoval(context);
        if (removal.isEmpty() || !ContraptionRemovalTransaction.canRemove(context, removal.get())) {
            return Result.REJECTED;
        }
        if (entity.level().isClientSide) {
            return Result.REMOVED;
        }
        if (!ContraptionRemovalTransaction.commit(context, removal.get())) {
            return Result.REJECTED;
        }
        rule.afterRemoved(context, removal.get());
        return Result.REMOVED;
    }
}
