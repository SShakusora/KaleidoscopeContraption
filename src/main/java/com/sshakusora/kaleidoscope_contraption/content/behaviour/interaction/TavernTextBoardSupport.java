package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.ChalkboardBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.SandwichBoardBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.deco.ChalkboardBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.deco.SandwichBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.deco.TextBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.block.properties.PositionType;
import com.github.ysbbbbbb.kaleidoscopetavern.util.TextAlignment;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.network.KCTavernPacketHandler;
import com.sshakusora.kaleidoscope_contraption.network.KCTavernTextOpenPacket;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.GlowInkSacItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.InkSacItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Shared text, layout and removal logic for Tavern's moving boards. */
public final class TavernTextBoardSupport {
    private static final double EDIT_DISTANCE_SQR = 64.0;

    private TavernTextBoardSupport() {
    }

    public static boolean handleInteraction(Player player, InteractionHand hand, BlockPos clickedPos,
                                            AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo clickedInfo =
                contraptionEntity.getContraption().getBlocks().get(clickedPos);
        if (clickedInfo == null || !isBoard(clickedInfo.state())) {
            return false;
        }

        BlockPos textPos = getTextPosition(contraptionEntity, clickedPos);
        if (textPos == null) {
            return false;
        }
        StructureTemplate.StructureBlockInfo textInfo =
                contraptionEntity.getContraption().getBlocks().get(textPos);
        if (textInfo == null) {
            return false;
        }

        TextBlockEntity textBlock = loadTextBlockEntity(contraptionEntity, textPos, textInfo);
        Vec3 globalPos = ContraptionInteractionUtil.getGlobalPos(contraptionEntity, textPos);
        if (player.distanceToSqr(globalPos.x, globalPos.y, globalPos.z) > EDIT_DISTANCE_SQR) {
            return false;
        }

        ItemStack held = player.getItemInHand(hand);
        if (textBlock.isWaxed()) {
            if (!contraptionEntity.level().isClientSide) {
                playSound(contraptionEntity, textPos, SoundEvents.WAXED_SIGN_INTERACT_FAIL);
            }
            return false;
        }

        if (held.getItem() instanceof DyeItem dyeItem
                && dyeItem.getDyeColor() != textBlock.getColor()) {
            if (!contraptionEntity.level().isClientSide) {
                textBlock.setColor(dyeItem.getDyeColor());
                saveTextBlock(contraptionEntity, textPos, textInfo, textBlock);
                if (!player.isCreative()) {
                    held.shrink(1);
                }
                playSound(contraptionEntity, textPos, SoundEvents.DYE_USE);
            }
            return true;
        }

        if (held.getItem() instanceof GlowInkSacItem && !textBlock.isGlowing()) {
            if (!contraptionEntity.level().isClientSide) {
                textBlock.setGlowing(true);
                saveTextBlock(contraptionEntity, textPos, textInfo, textBlock);
                if (!player.isCreative()) {
                    held.shrink(1);
                }
                playSound(contraptionEntity, textPos, SoundEvents.GLOW_INK_SAC_USE);
            }
            return true;
        }

        if (held.getItem() instanceof InkSacItem && textBlock.isGlowing()) {
            if (!contraptionEntity.level().isClientSide) {
                textBlock.setGlowing(false);
                saveTextBlock(contraptionEntity, textPos, textInfo, textBlock);
                if (!player.isCreative()) {
                    held.shrink(1);
                }
                playSound(contraptionEntity, textPos, SoundEvents.INK_SAC_USE);
            }
            return true;
        }

        if (held.getItem() instanceof HoneycombItem) {
            if (!contraptionEntity.level().isClientSide) {
                textBlock.setWaxed(true);
                saveTextBlock(contraptionEntity, textPos, textInfo, textBlock);
                if (!player.isCreative()) {
                    held.shrink(1);
                }
                playSound(contraptionEntity, textPos, SoundEvents.HONEYCOMB_WAX_ON);
            }
            return true;
        }

        if (tryTransformSandwich(player, held, contraptionEntity, textPos, textInfo)) {
            return true;
        }

        if (!contraptionEntity.level().isClientSide) {
            KCTavernPacketHandler.sendToClient(player, new KCTavernTextOpenPacket(
                    contraptionEntity.getId(), textPos, textBlock.getText(),
                    textBlock.getMaxTextLength(), textBlock.getTextAlignment()));
        }
        return true;
    }

    public static void updateText(Player player, int entityId, BlockPos textPos,
                                  String text, TextAlignment alignment) {
        if (!(player.level().getEntity(entityId) instanceof AbstractContraptionEntity contraptionEntity)
                || alignment == null) {
            return;
        }
        StructureTemplate.StructureBlockInfo info =
                contraptionEntity.getContraption().getBlocks().get(textPos);
        if (info == null || !isTextSource(info.state())) {
            return;
        }
        TextBlockEntity blockEntity = loadTextBlockEntity(contraptionEntity, textPos, info);
        Vec3 globalPos = ContraptionInteractionUtil.getGlobalPos(contraptionEntity, textPos);
        if (player.distanceToSqr(globalPos.x, globalPos.y, globalPos.z) > EDIT_DISTANCE_SQR
                || blockEntity.isWaxed()
                || text.length() > blockEntity.getMaxTextLength()) {
            return;
        }
        blockEntity.setText(text);
        blockEntity.setTextAlignment(alignment);
        saveTextBlock(contraptionEntity, textPos, info, blockEntity);
    }

    public static boolean isBoard(BlockState state) {
        return state.getBlock() instanceof SandwichBoardBlock
                || state.getBlock() instanceof ChalkboardBlock;
    }

    public static boolean isTextSource(BlockState state) {
        if (state.getBlock() instanceof SandwichBoardBlock) {
            return state.getValue(SandwichBoardBlock.HALF) == Half.BOTTOM;
        }
        return state.getBlock() instanceof ChalkboardBlock
                && state.getValue(ChalkboardBlock.HALF) == Half.BOTTOM
                && (state.getValue(ChalkboardBlock.POSITION) == PositionType.SINGLE
                || state.getValue(ChalkboardBlock.POSITION) == PositionType.MIDDLE);
    }

    public static BlockPos getTextPosition(AbstractContraptionEntity contraptionEntity, BlockPos clickedPos) {
        StructureTemplate.StructureBlockInfo info =
                contraptionEntity.getContraption().getBlocks().get(clickedPos);
        if (info == null || !isBoard(info.state())) {
            return null;
        }
        BlockState state = info.state();
        BlockPos textPos;
        if (state.getBlock() instanceof SandwichBoardBlock) {
            textPos = state.getValue(SandwichBoardBlock.HALF) == Half.TOP
                    ? clickedPos.below() : clickedPos;
        } else {
            PositionType position = state.getValue(ChalkboardBlock.POSITION);
            BlockPos bottom = state.getValue(ChalkboardBlock.HALF) == Half.TOP
                    ? clickedPos.below() : clickedPos;
            Direction facing = state.getValue(ChalkboardBlock.FACING);
            if (position == PositionType.LEFT) {
                textPos = bottom.relative(facing.getCounterClockWise());
            } else if (position == PositionType.RIGHT) {
                textPos = bottom.relative(facing.getClockWise());
            } else {
                textPos = bottom;
            }
        }

        StructureTemplate.StructureBlockInfo textInfo =
                contraptionEntity.getContraption().getBlocks().get(textPos);
        return textInfo != null && isTextSource(textInfo.state()) ? textPos : null;
    }

    public static List<BlockPos> getStructurePositions(AbstractContraptionEntity contraptionEntity,
                                                       BlockPos clickedPos) {
        BlockPos textPos = getTextPosition(contraptionEntity, clickedPos);
        if (textPos == null) {
            return List.of();
        }
        StructureTemplate.StructureBlockInfo textInfo =
                contraptionEntity.getContraption().getBlocks().get(textPos);
        if (textInfo == null || !isTextSource(textInfo.state())) {
            return List.of();
        }
        BlockState textState = textInfo.state();
        Set<BlockPos> positions = new LinkedHashSet<>();
        if (textState.getBlock() instanceof SandwichBoardBlock) {
            positions.add(textPos);
            positions.add(textPos.above());
        } else {
            PositionType position = textState.getValue(ChalkboardBlock.POSITION);
            if (position == PositionType.SINGLE) {
                positions.add(textPos);
                positions.add(textPos.above());
            } else {
                Direction facing = textState.getValue(ChalkboardBlock.FACING);
                positions.add(textPos);
                positions.add(textPos.relative(facing.getClockWise()));
                positions.add(textPos.relative(facing.getCounterClockWise()));
                positions.add(textPos.above());
                positions.add(textPos.relative(facing.getClockWise()).above());
                positions.add(textPos.relative(facing.getCounterClockWise()).above());
            }
        }
        return new ArrayList<>(positions);
    }

    public static TextBlockEntity loadTextBlockEntity(AbstractContraptionEntity contraptionEntity,
                                                       BlockPos localPos,
                                                       StructureTemplate.StructureBlockInfo info) {
        BlockPos globalPos = ContraptionInteractionUtil.getGlobalBlockPos(contraptionEntity, localPos);
        TextBlockEntity blockEntity;
        if (info.state().getBlock() instanceof SandwichBoardBlock) {
            blockEntity = new SandwichBlockEntity(globalPos, info.state());
        } else if (info.state().getBlock() instanceof ChalkboardBlock) {
            blockEntity = info.state().getValue(ChalkboardBlock.POSITION) == PositionType.MIDDLE
                    ? ChalkboardBlockEntity.large(globalPos, info.state())
                    : ChalkboardBlockEntity.small(globalPos, info.state());
        } else {
            throw new IllegalArgumentException("Not a Tavern text board: " + info.state());
        }
        blockEntity.setLevel(contraptionEntity.level());
        blockEntity.load(info.nbt() == null ? new net.minecraft.nbt.CompoundTag() : info.nbt().copy());
        return blockEntity;
    }

    private static void saveTextBlock(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                      StructureTemplate.StructureBlockInfo info,
                                      TextBlockEntity blockEntity) {
        net.minecraft.nbt.CompoundTag tag = blockEntity.saveWithFullMetadata();
        tag.remove("x");
        tag.remove("y");
        tag.remove("z");
        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, localPos,
                new StructureTemplate.StructureBlockInfo(info.pos(), info.state(), tag));
    }

    private static boolean tryTransformSandwich(Player player, ItemStack held,
                                                AbstractContraptionEntity contraptionEntity,
                                                BlockPos textPos,
                                                StructureTemplate.StructureBlockInfo info) {
        if (!(info.state().getBlock() instanceof SandwichBoardBlock current)) {
            return false;
        }
        Item item = held.getItem();
        SandwichBoardBlock transformed = SandwichBoardBlock.TRANSFORM_MAP.get(item);
        if (transformed == null || transformed == current) {
            return false;
        }
        if (contraptionEntity.level().isClientSide) {
            return true;
        }

        BlockState bottom = transformed.defaultBlockState()
                .setValue(SandwichBoardBlock.ROTATION, info.state().getValue(SandwichBoardBlock.ROTATION))
                .setValue(SandwichBoardBlock.HALF, Half.BOTTOM)
                .setValue(SandwichBoardBlock.WATERLOGGED, info.state().getValue(SandwichBoardBlock.WATERLOGGED));
        BlockPos topPos = textPos.above();
        StructureTemplate.StructureBlockInfo topInfo = contraptionEntity.getContraption().getBlocks().get(topPos);
        BlockState top = bottom.setValue(SandwichBoardBlock.HALF, Half.TOP);
        ContraptionInteractionUtil.updateContraptionData(contraptionEntity, textPos,
                new StructureTemplate.StructureBlockInfo(info.pos(), bottom, info.nbt()));
        if (topInfo != null) {
            ContraptionInteractionUtil.updateContraptionData(contraptionEntity, topPos,
                    new StructureTemplate.StructureBlockInfo(topInfo.pos(), top, topInfo.nbt()));
        }
        if (!player.isCreative()) {
            held.shrink(1);
        }
        playSound(contraptionEntity, textPos, SoundEvents.GRASS_PLACE);
        return true;
    }

    private static void playSound(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                  net.minecraft.sounds.SoundEvent sound) {
        ContraptionInteractionUtil.playSound(contraptionEntity, localPos, sound,
                SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
