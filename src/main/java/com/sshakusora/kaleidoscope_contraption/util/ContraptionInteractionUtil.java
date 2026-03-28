package com.sshakusora.kaleidoscope_contraption.util;

import com.github.ysbbbbbb.kaleidoscopecookery.init.tag.TagMod;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.mixin.accessor.ContraptionAccessor;
import com.sshakusora.kaleidoscope_contraption.network.KCContraptionChangedPacket;
import com.sshakusora.kaleidoscope_contraption.network.KCPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.MutablePair;

public class ContraptionInteractionUtil {

    /**
     * 更新Contraption中的方块数据，包括blocks、actors和updateTags，并同步到客户端
     */
    public static void updateContraptionData(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                               StructureTemplate.StructureBlockInfo newInfo) {
        // 更新方块数据
        contraptionEntity.getContraption().getBlocks().put(localPos, newInfo);
        
        // 标记为更新，避免重进存档后NBT消失
        if (newInfo.nbt() != null) {
            ((ContraptionAccessor) contraptionEntity.getContraption()).getUpdateTags().put(localPos, newInfo.nbt());
        }

        // 查找并更新actor数据
        var actors = contraptionEntity.getContraption().getActors();
        for (MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor : actors) {
            if (actor.getLeft().pos().equals(localPos)) {
                actor.setLeft(newInfo);
                break;
            }
        }

        // 发送自定义数据包同步NBT数据到客户端
        if (!contraptionEntity.level().isClientSide) {
            KCPacketHandler.sendToTracking(
                    new KCContraptionChangedPacket(
                            contraptionEntity.getId(),
                            localPos,
                            newInfo.state(),
                            newInfo.nbt()
                    ),
                    contraptionEntity
            );
        }
    }

    public static void updateContraptionDataWithBound(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                             StructureTemplate.StructureBlockInfo newInfo, AABB updatedBounds) {
        // 更新方块数据
        contraptionEntity.getContraption().getBlocks().put(localPos, newInfo);

        // 标记为更新，避免重进存档后NBT消失
        if (newInfo.nbt() != null) {
            ((ContraptionAccessor) contraptionEntity.getContraption()).getUpdateTags().put(localPos, newInfo.nbt());
        }

        // 查找并更新actor数据
        var actors = contraptionEntity.getContraption().getActors();
        for (MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor : actors) {
            if (actor.getLeft().pos().equals(localPos)) {
                actor.setLeft(newInfo);
                break;
            }
        }

        // 发送自定义数据包同步NBT数据到客户端
        if (!contraptionEntity.level().isClientSide) {
            KCPacketHandler.sendToTracking(
                    new KCContraptionChangedPacket(
                            contraptionEntity.getId(),
                            localPos,
                            newInfo.state(),
                            newInfo.nbt(),
                            updatedBounds
                    ),
                    contraptionEntity
            );
        }
    }

    /**
     * 从Contraption中移除一个方块，包括从blocks、interactors、actors中移除，并更新bounds
     */
    public static void removeBlockFromContraption(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        // 从blocks中移除
        contraptionEntity.getContraption().getBlocks().remove(localPos);

        // 从interactors中移除
        contraptionEntity.getContraption().getInteractors().remove(localPos);

        // 从actors中移除
        contraptionEntity.getContraption().getActors().removeIf(actor -> actor.getLeft().pos().equals(localPos));
    }

    /**
     * 从Contraption中移除多个方块
     */
    public static void removeBlocksFromContraption(AbstractContraptionEntity contraptionEntity, BlockPos... positions) {
        for (BlockPos pos : positions) {
            removeBlockFromContraption(contraptionEntity, pos);
        }
    }

    /**
     * 同步移除方块到客户端（发送空气状态包）
     */
    public static void syncBlockRemoval(AbstractContraptionEntity contraptionEntity, BlockPos localPos, AABB updatedBounds) {
        if (contraptionEntity.level().isClientSide) {
            return;
        }
        
        BlockState airState = Blocks.AIR.defaultBlockState();
        KCPacketHandler.sendToTracking(
                new KCContraptionChangedPacket(
                        contraptionEntity.getId(),
                        localPos,
                        airState,
                        null,
                        updatedBounds
                ),
                contraptionEntity
        );
    }

    /**
     * 同步多个方块移除到客户端
     */
    public static void syncBlockRemoval(AbstractContraptionEntity contraptionEntity, AABB updatedBounds, BlockPos... positions) {
        for (BlockPos pos : positions) {
            syncBlockRemoval(contraptionEntity, pos, updatedBounds);
        }
    }

    /**
     * 检查Contraption中指定位置下方是否有热源
     */
    public static boolean hasHeatSource(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        Contraption contraption = contraptionEntity.getContraption();
        BlockPos belowLocalPos = localPos.below();

        // 首先检查 Contraption 内部下方是否有方块
        StructureTemplate.StructureBlockInfo belowInfo = contraption.getBlocks().get(belowLocalPos);
        if (belowInfo != null) {
            BlockState belowState = belowInfo.state();
            // 检查是否有 LIT 属性
            if (belowState.hasProperty(BlockStateProperties.LIT)) {
                return belowState.getValue(BlockStateProperties.LIT);
            }
            // 检查是否在热源标签中
            return belowState.is(TagMod.HEAT_SOURCE_BLOCKS_WITHOUT_LIT);
        }

        // Contraption 内部没有下方方块，检查世界中 Contraption 实体下方的方块
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
        BlockPos worldPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
        BlockPos worldBelowPos = worldPos.below();

        BlockState worldBelowState = contraptionEntity.level().getBlockState(worldBelowPos);
        if (worldBelowState.hasProperty(BlockStateProperties.LIT)) {
            return worldBelowState.getValue(BlockStateProperties.LIT);
        }
        return worldBelowState.is(TagMod.HEAT_SOURCE_BLOCKS_WITHOUT_LIT);
    }


    /**
     * 检查是否有热源
     */
    public static boolean hasHeatSource(MovementContext context) {
        Contraption contraption = context.contraption;
        BlockPos belowLocalPos = context.localPos.below();

        // 首先检查Contraption内部下方是否有方块
        StructureTemplate.StructureBlockInfo belowInfo = contraption.getBlocks().get(belowLocalPos);
        if (belowInfo != null) {
            BlockState belowState = belowInfo.state();
            // 检查是否有LIT属性
            if (belowState.hasProperty(BlockStateProperties.LIT)) {
                return belowState.getValue(BlockStateProperties.LIT);
            }
            // 检查是否在热源标签中
            return belowState.is(TagMod.HEAT_SOURCE_BLOCKS_WITHOUT_LIT);
        }

        // Contraption内部没有下方方块，检查世界中Contraption实体下方的方块
        if (context.contraption.entity == null) {
            return false;
        }

        Vec3 globalPos = context.contraption.entity.toGlobalVector(Vec3.atCenterOf(context.localPos), 1.0f);
        BlockPos worldPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
        BlockPos worldBelowPos = worldPos.below();

        BlockState worldBelowState = context.world.getBlockState(worldBelowPos);
        if (worldBelowState.hasProperty(BlockStateProperties.LIT)) {
            return worldBelowState.getValue(BlockStateProperties.LIT);
        }
        return worldBelowState.is(TagMod.HEAT_SOURCE_BLOCKS_WITHOUT_LIT);
    }

    /**
     * 播放Contraption中方块的音效
     */
    public static void playSound(AbstractContraptionEntity contraptionEntity, BlockPos localPos, 
                                  SoundEvent soundEvent, SoundSource source, float volume, float pitch) {
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
        contraptionEntity.level().playSound(null, soundPos, soundEvent, source, volume, pitch);
    }

    /**
     * 播放Contraption中方块的音效（使用玩家作为声音源）
     */
    public static void playSound(Player player, AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                  SoundEvent soundEvent, SoundSource source, float volume, float pitch) {
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
        BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
        contraptionEntity.level().playSound(player, soundPos, soundEvent, source, volume, pitch);
    }

    /**
     * 播放Contraption中方块的破坏音效
     */
    public static void playBreakSound(AbstractContraptionEntity contraptionEntity, BlockPos localPos, BlockState state) {
        playSound(contraptionEntity, localPos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /**
     * 播放Contraption中方块的放置音效
     */
    public static void playPlaceSound(AbstractContraptionEntity contraptionEntity, BlockPos localPos, BlockState state) {
        playSound(contraptionEntity, localPos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /**
     * 获取Contraption中方块的全局位置
     */
    public static Vec3 getGlobalPos(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        return contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
    }

    /**
     * 获取Contraption中方块的全局BlockPos（用于音效等）
     */
    public static BlockPos getGlobalBlockPos(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        Vec3 globalPos = getGlobalPos(contraptionEntity, localPos);
        return new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
    }

    /**
     * 更新Contraption的bounds并返回新的bounds
     */
    public static AABB updateBounds(AbstractContraptionEntity contraptionEntity, BlockPos newPos) {
        AABB updatedBounds = contraptionEntity.getContraption().bounds.minmax(new AABB(newPos));
        contraptionEntity.getContraption().bounds = updatedBounds;
        return updatedBounds;
    }

    /**
     * 重新计算Contraption的bounds（用于移除方块后）
     */
    public static AABB recalculateBounds(AbstractContraptionEntity contraptionEntity) {
        return ContraptionBoundsUtil.recalculateBounds(contraptionEntity.getContraption());
    }

    /**
     * 在Contraption位置生成掉落物给玩家
     */
    public static void dropItemToPlayer(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                        Player player, ItemStack stack) {
        if (contraptionEntity.level().isClientSide || stack.isEmpty()) {
            return;
        }

        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
        ItemEntity itemEntity = new ItemEntity(
                contraptionEntity.level(),
                globalPos.x,
                globalPos.y + 0.5,
                globalPos.z,
                stack
        );
        itemEntity.setDefaultPickUpDelay();
        contraptionEntity.level().addFreshEntity(itemEntity);
    }

    /**
     * 在Contraption位置弹出物品（模拟Block.popResource）
     */
    public static void popResource(AbstractContraptionEntity contraptionEntity, BlockPos localPos, ItemStack stack) {
        if (contraptionEntity.level().isClientSide || stack.isEmpty()) {
            return;
        }
        Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
        ItemEntity entity = new ItemEntity(
                contraptionEntity.level(),
                globalPos.x,
                globalPos.y - 0.25,
                globalPos.z,
                stack, 0, 0.1, 0);
        entity.setDefaultPickUpDelay();
        contraptionEntity.level().addFreshEntity(entity);
    }
}
