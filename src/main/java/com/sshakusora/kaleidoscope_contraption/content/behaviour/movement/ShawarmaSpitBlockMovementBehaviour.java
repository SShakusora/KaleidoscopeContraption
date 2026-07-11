package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.ShawarmaSpitBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModParticles;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionDataUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

public class ShawarmaSpitBlockMovementBehaviour implements MovementBehaviour {

    // NBT键名（与ShawarmaSpitBlockEntity保持一致）
    private static final String COOKING_ITEM = "CookingItem";
    private static final String COOKED_ITEM = "CookedItem";
    private static final String COOK_TIME = "CookTime";

    @Override
    public void tick(MovementContext context) {
        if (context.world.isClientSide) {
            return;
        }

        // 获取当前方块信息
        StructureTemplate.StructureBlockInfo info = context.contraption.getBlocks().get(context.localPos);
        if (info == null || !(info.state().getBlock() instanceof ShawarmaSpitBlock)) {
            return;
        }

        BlockState state = info.state();
        CompoundTag nbt = info.nbt();
        if (nbt == null) {
            return;
        }

        // 检查并更新红石信号状态
        boolean stateUpdated = checkAndUpdateRedstoneState(context, state, nbt, info);
        if (!stateUpdated) {
            return;
        }

        // 重新获取最新的info和state
        info = context.contraption.getBlocks().get(context.localPos);
        state = info.state();
        nbt = info.nbt();

        // 执行tick逻辑
        tickShawarmaSpit(context, state, nbt, info);
    }

    /**
     * 检查并更新红石信号状态
     * 参考 ShawarmaSpitBlock.neighborChanged 实现
     * 优先检查Contraption内部的红石信号，没有再检查世界中的信号
     * @return 是否通电（应该执行tick）
     */
    private boolean checkAndUpdateRedstoneState(MovementContext context, BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        // 计算当前位置和另一个部分的位置（根据HALF属性）
        DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
        Direction otherDirection = half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
        BlockPos otherLocalPos = context.localPos.relative(otherDirection);

        // 首先检查Contraption内部的红石信号
        boolean powered = checkInternalRedstoneSignal(context, context.localPos) 
                       || checkInternalRedstoneSignal(context, otherLocalPos);

        // 如果Contraption内部没有红石信号，且实体存在，则检查世界中的红石信号
        if (!powered && context.contraption.entity != null) {
            Vec3 globalPos = getGlobalPos(context);
            BlockPos worldPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
            BlockPos otherWorldPos = worldPos.relative(otherDirection);
            
            powered = context.world.hasNeighborSignal(worldPos) || context.world.hasNeighborSignal(otherWorldPos);
        }

        boolean currentPowered = state.getValue(BlockStateProperties.POWERED);

        // 如果红石信号状态发生变化，更新BlockState并同步
        if (powered != currentPowered) {
            BlockState newState = state.setValue(BlockStateProperties.POWERED, powered);
            ContraptionDataUtil.updateContraptionData(context, newState, nbt, true);
            // 注意：这里更新了BlockState，调用者需要重新获取最新的info
        }

        // 只有通电时才继续执行tick逻辑
        return powered;
    }

    /**
     * 检查Contraption内部指定位置是否有红石信号
     * 参考 SignalGetter.hasNeighborSignal 实现
     * 检查六个相邻方向的方块是否提供红石信号
     */
    private boolean checkInternalRedstoneSignal(MovementContext context, BlockPos localPos) {
        Contraption contraption = context.contraption;
        
        // 检查下方
        if (getInternalSignal(contraption, localPos.below(), Direction.DOWN) > 0) {
            return true;
        }
        // 检查上方
        if (getInternalSignal(contraption, localPos.above(), Direction.UP) > 0) {
            return true;
        }
        // 检查北方
        if (getInternalSignal(contraption, localPos.north(), Direction.NORTH) > 0) {
            return true;
        }
        // 检查南方
        if (getInternalSignal(contraption, localPos.south(), Direction.SOUTH) > 0) {
            return true;
        }
        // 检查西方
        if (getInternalSignal(contraption, localPos.west(), Direction.WEST) > 0) {
            return true;
        }
        // 检查东方
        return getInternalSignal(contraption, localPos.east(), Direction.EAST) > 0;
    }
    
    /**
     * 获取Contraption内部指定位置在指定方向上的红石信号强度
     * 参考 SignalGetter.getSignal 实现
     */
    private int getInternalSignal(Contraption contraption, BlockPos pos, Direction direction) {
        // TODO 无法检测充能方块
        StructureTemplate.StructureBlockInfo info = contraption.getBlocks().get(pos);
        if (info == null) {
            return 0;
        }
        
        BlockState state = info.state();
        
        // 获取方块在指定方向上提供的信号强度
        int signal = state.getSignal(contraption.getContraptionWorld(), pos, direction);
        
        // 如果方块应该检查弱充能，则获取直接信号
        if (state.shouldCheckWeakPower(contraption.getContraptionWorld(), pos, direction)) {
            signal = Math.max(signal, getInternalDirectSignalTo(contraption, pos));
        }
        
        return signal;
    }
    
    /**
     * 获取Contraption内部指定位置的直接红石信号强度
     * 检查该位置是否为强充能源（如红石块）
     */
    private int getInternalDirectSignalTo(Contraption contraption, BlockPos pos) {
        StructureTemplate.StructureBlockInfo info = contraption.getBlocks().get(pos);
        if (info == null) {
            return 0;
        }
        
        BlockState state = info.state();
        int maxSignal = 0;
        
        // 检查所有方向的直接信号
        for (Direction direction : Direction.values()) {
            int directSignal = state.getDirectSignal(contraption.getContraptionWorld(), pos, direction);
            if (directSignal > maxSignal) {
                maxSignal = directSignal;
            }
        }
        
        return maxSignal;
    }

    /**
     * 执行ShawarmaSpit的tick逻辑（参考ShawarmaSpitBlockEntity.tick()）
     */
    private void tickShawarmaSpit(MovementContext context, BlockState state, CompoundTag nbt, StructureTemplate.StructureBlockInfo info) {
        ItemStack cookingItem = readCookingItem(nbt, context.world);
        ItemStack cookedItem = readCookedItem(nbt, context.world);
        int cookTime = nbt.getInt(COOK_TIME);
        RandomSource random = context.world.random;

        // 如果没有正在烹饪的物品，只产生粒子效果
        if (cookingItem.isEmpty()) {
            if (!cookedItem.isEmpty()) {
                spawnParticles(context);
            }
            return;
        }

        // 产生烹饪粒子效果
        spawnParticles(context);

        // 烹饪计时
        if (cookTime > 0) {
            cookTime--;

            CompoundTag newNbt = nbt.copy();
            newNbt.putInt(COOK_TIME, cookTime);
            ContraptionDataUtil.updateContraptionData(context, state, newNbt, false);
        } else {
            // 烹饪完成
            playExtinguishSound(context);

            // 清空cookingItem
            CompoundTag newNbt = nbt.copy();
            newNbt.put(COOKING_ITEM, ItemStack.EMPTY.saveOptional(context.world.registryAccess()));
            ContraptionDataUtil.updateContraptionData(context, state, newNbt, true);
        }
    }

    /**
     * 产生烹饪粒子效果
     */
    private void spawnParticles(MovementContext context) {
        if (!(context.world instanceof ServerLevel serverLevel)) {
            return;
        }

        RandomSource random = context.world.random;
        Vec3 globalPos = getGlobalPos(context);

        // 产生烹饪粒子（25%概率）
        if (random.nextFloat() < 0.25f) {
            serverLevel.sendParticles(ModParticles.COOKING.get(),
                    globalPos.x + 0.5,
                    globalPos.y + 0.5,
                    globalPos.z + 0.5,
                    1,
                    0.25, 0.2, 0.25,
                    0.1f);
        }

        // 每20tick播放一次营火噼啪声
        if (random.nextInt(20) == 0) {
            serverLevel.playSound(null,
                    globalPos.x + 0.5,
                    globalPos.y + 0.5,
                    globalPos.z + 0.5,
                    SoundEvents.CAMPFIRE_CRACKLE,
                    SoundSource.BLOCKS,
                    0.5F + random.nextFloat(),
                    random.nextFloat() * 0.7F + 0.6F);
        }
    }

    /**
     * 播放熄火音效
     */
    private void playExtinguishSound(MovementContext context) {
        if (context.contraption.entity == null) return;
        Vec3 globalPos = getGlobalPos(context);
        context.world.playSound(null,
                globalPos.x + 0.5,
                globalPos.y + 0.5,
                globalPos.z + 0.5,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                0.5F + context.world.random.nextFloat(),
                context.world.random.nextFloat() * 0.7F + 0.6F);
    }

    /**
     * 获取全局位置
     */
    private Vec3 getGlobalPos(MovementContext context) {
        if (context.contraption.entity == null) {
            return Vec3.atCenterOf(context.localPos);
        }
        return context.contraption.entity.toGlobalVector(Vec3.atCenterOf(context.localPos), 1.0f);
    }

    /**
     * 读取正在烹饪的物品
     */
    private ItemStack readCookingItem(CompoundTag nbt, Level level) {
        if (nbt.contains(COOKING_ITEM)) {
            return ItemStack.parseOptional(level.registryAccess(), nbt.getCompound(COOKING_ITEM));
        }
        return ItemStack.EMPTY;
    }

    /**
     * 读取烹饪完成的物品
     */
    private ItemStack readCookedItem(CompoundTag nbt, Level level) {
        if (nbt.contains(COOKED_ITEM)) {
            return ItemStack.parseOptional(level.registryAccess(), nbt.getCompound(COOKED_ITEM));
        }
        return ItemStack.EMPTY;
    }
}
