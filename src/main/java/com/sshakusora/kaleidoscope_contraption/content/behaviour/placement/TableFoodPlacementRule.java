package com.sshakusora.kaleidoscope_contraption.content.behaviour.placement;

import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteOneByTwoBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteThreeByThreeBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.NinePart;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.Quality;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.QualityUtils;
import com.github.ysbbbbbb.kaleidoscopecookery.util.ItemUtils;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.api.placement.*;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction.FoodBiteBlockMovingInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TableFoodPlacementRule implements ContraptionPlacementRule {
    private static final String SHOW_ITEMS = "ShowItems";

    @Override
    public boolean matches(ContraptionPlacementContext context) {
        if (!(Block.byItem(context.heldItem().getItem()) instanceof FoodBiteBlock)) {
            return false;
        }
        CompoundTag nbt = context.supportInfo().nbt();
        ItemStackHandler tableItems = new ItemStackHandler(4);
        if (nbt != null && nbt.contains(SHOW_ITEMS)) {
            tableItems.deserializeNBT(nbt.getCompound(SHOW_ITEMS));
        }
        int lastIndex = ItemUtils.getLastStack(tableItems).getLeft();
        return context.player().isShiftKeyDown() || lastIndex >= tableItems.getSlots() - 1;
    }

    @Override
    public ContraptionPlacementResult createPlacement(ContraptionPlacementContext context) {
        FoodBiteBlock food = (FoodBiteBlock) Block.byItem(context.heldItem().getItem());
        Direction facing = context.player().getDirection().getOpposite();
        int quality = FoodBiteBlockMovingInteraction.getQualityId(context.heldItem());
        List<StructureTemplate.StructureBlockInfo> blocks = new ArrayList<>();

        if (food instanceof FoodBiteOneByTwoBlock oneByTwo) {
            BlockPos rightPos = context.targetPos();
            BlockPos leftPos = rightPos.relative(facing.getClockWise());
            blocks.add(info(rightPos, baseState(oneByTwo, facing, quality)
                    .setValue(FoodBiteOneByTwoBlock.POSITION, FoodBiteOneByTwoBlock.RIGHT)));
            blocks.add(info(leftPos, baseState(oneByTwo, facing, quality)
                    .setValue(FoodBiteOneByTwoBlock.POSITION, FoodBiteOneByTwoBlock.LEFT)));
        } else if (food instanceof FoodBiteThreeByThreeBlock threeByThree) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = context.targetPos().offset(x, 0, z);
                    BlockState state = baseState(threeByThree, facing, quality)
                            .setValue(FoodBiteThreeByThreeBlock.PART,
                                    Objects.requireNonNull(NinePart.getPartByPos(x, z)));
                    blocks.add(info(pos, state));
                }
            }
        } else {
            blocks.add(info(context.targetPos(), baseState(food, facing, quality)));
        }

        return new ContraptionPlacementResult(blocks, 1, true);
    }

    @Override
    public Optional<ContraptionRemovalResult> createRemoval(ContraptionRemovalContext context) {
        StructureTemplate.StructureBlockInfo targetInfo = context.targetInfo();
        if (!(targetInfo.state().getBlock() instanceof FoodBiteBlock)) {
            return Optional.empty();
        }

        List<BlockPos> positions = getStructurePositions(context.targetPos(), targetInfo.state());
        StructureTemplate.StructureBlockInfo primaryInfo = getPrimaryInfo(context, positions);
        if (primaryInfo == null || !(primaryInfo.state().getBlock() instanceof FoodBiteBlock food)) {
            return Optional.empty();
        }
        if (primaryInfo.state().getValue(food.getBites()) != 0
                || !isCompleteStructure(context, positions, food)) {
            return Optional.empty();
        }

        ItemStack returnedFood = new ItemStack(food.asItem());
        int qualityId = primaryInfo.state().getValue(FoodBiteBlock.QUALITY);
        if (qualityId != FoodBiteBlock.DEFAULT_QUALITY) {
            QualityUtils.setQuality(returnedFood, Quality.BY_ID.apply(qualityId));
        }
        return Optional.of(new ContraptionRemovalResult(
                positions, List.of(), List.of(returnedFood), true));
    }

    public boolean tryRemoveUnregistered(Player player, BlockPos targetPos,
                                         AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo targetInfo =
                contraptionEntity.getContraption().getBlocks().get(targetPos);
        if (targetInfo == null) {
            return false;
        }
        ContraptionRemovalContext context = new ContraptionRemovalContext(
                player, targetPos, targetInfo, contraptionEntity);
        Optional<ContraptionRemovalResult> removal = createRemoval(context);
        if (removal.isEmpty() || !ContraptionRemovalTransaction.canRemove(context, removal.get())) {
            return false;
        }
        return contraptionEntity.level().isClientSide
                || ContraptionRemovalTransaction.commit(context, removal.get());
    }

    private BlockState baseState(FoodBiteBlock food, Direction facing, int quality) {
        return food.defaultBlockState()
                .setValue(food.getBites(), 0)
                .setValue(FoodBiteBlock.FACING, facing)
                .setValue(FoodBiteBlock.QUALITY, quality);
    }

    private StructureTemplate.StructureBlockInfo info(BlockPos pos, BlockState state) {
        return new StructureTemplate.StructureBlockInfo(pos, state, null);
    }

    private List<BlockPos> getStructurePositions(BlockPos targetPos, BlockState state) {
        if (state.getBlock() instanceof FoodBiteOneByTwoBlock) {
            Direction facing = state.getValue(FoodBiteBlock.FACING);
            if (state.getValue(FoodBiteOneByTwoBlock.POSITION) == FoodBiteOneByTwoBlock.LEFT) {
                return List.of(targetPos, targetPos.relative(facing.getCounterClockWise()));
            }
            return List.of(targetPos.relative(facing.getClockWise()), targetPos);
        }
        if (state.getBlock() instanceof FoodBiteThreeByThreeBlock) {
            NinePart part = state.getValue(FoodBiteThreeByThreeBlock.PART);
            BlockPos centerPos = targetPos.subtract(new Vec3i(
                    part.getPosX(), 0, part.getPosY()));
            List<BlockPos> positions = new ArrayList<>(9);
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    positions.add(centerPos.offset(x, 0, z));
                }
            }
            return positions;
        }
        return List.of(targetPos);
    }

    private StructureTemplate.StructureBlockInfo getPrimaryInfo(ContraptionRemovalContext context,
                                                                List<BlockPos> positions) {
        return context.contraptionEntity().getContraption().getBlocks().get(positions.get(0));
    }

    private boolean isCompleteStructure(ContraptionRemovalContext context, List<BlockPos> positions,
                                        FoodBiteBlock food) {
        for (BlockPos position : positions) {
            StructureTemplate.StructureBlockInfo info =
                    context.contraptionEntity().getContraption().getBlocks().get(position);
            if (info == null || info.state().getBlock() != food
                    || info.state().getValue(food.getBites()) != 0) {
                return false;
            }
        }
        return true;
    }
}
