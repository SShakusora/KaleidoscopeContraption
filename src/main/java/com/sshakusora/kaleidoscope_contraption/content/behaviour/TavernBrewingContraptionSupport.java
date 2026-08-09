package com.sshakusora.kaleidoscope_contraption.content.behaviour;

import com.github.ysbbbbbb.kaleidoscopetavern.api.blockentity.IBarrel;
import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.BarrelBlock;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public final class TavernBrewingContraptionSupport {
    public static final String INGREDIENT = "ingredient";
    public static final String OUTPUT = "output";
    public static final String FLUID = "fluid";
    public static final String OPEN = "open";
    public static final String BREW_LEVEL = "brew_level";
    public static final String BREW_TIME = "brew_time";
    public static final String RECIPE_ID = "recipe_id";

    private TavernBrewingContraptionSupport() {
    }

    public static CompoundTag copyNbt(@Nullable StructureTemplate.StructureBlockInfo info) {
        return info == null || info.nbt() == null ? new CompoundTag() : info.nbt().copy();
    }

    public static ItemStackHandler readItems(CompoundTag nbt, String key, int slots, int slotLimit) {
        ItemStackHandler items = new ItemStackHandler(slots) {
            @Override
            public int getSlotLimit(int slot) {
                return slotLimit;
            }
        };
        if (nbt.contains(key)) {
            items.deserializeNBT(nbt.getCompound(key));
        }
        return items;
    }

    public static FluidTank readFluid(CompoundTag nbt, int capacity) {
        FluidTank tank = new FluidTank(capacity);
        if (nbt.contains(FLUID)) {
            tank.readFromNBT(nbt.getCompound(FLUID));
        }
        return tank;
    }

    public static void writeItems(CompoundTag nbt, String key, ItemStackHandler items) {
        nbt.put(key, items.serializeNBT());
    }

    public static void writeFluid(CompoundTag nbt, FluidTank fluid) {
        nbt.put(FLUID, fluid.writeToNBT(new CompoundTag()));
    }

    public static void update(AbstractContraptionEntity entity, BlockPos localPos,
                              BlockState state, CompoundTag nbt) {
        StructureTemplate.StructureBlockInfo info =
                new StructureTemplate.StructureBlockInfo(localPos, state, nbt);
        ContraptionInteractionUtil.updateContraptionData(entity, localPos, info);
    }

    public static Vec3 globalCenter(AbstractContraptionEntity entity, BlockPos localPos) {
        return entity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0F);
    }

    @Nullable
    public static BarrelData findBarrel(Contraption contraption, BlockPos clickedLocalPos,
                                        BlockState clickedState) {
        if (!(clickedState.getBlock() instanceof BarrelBlock)) {
            return null;
        }
        BlockPos origin = BarrelBlock.getOriginPos(clickedLocalPos, clickedState);
        StructureTemplate.StructureBlockInfo originInfo = contraption.getBlocks().get(origin);
        if (originInfo == null || !(originInfo.state().getBlock() instanceof BarrelBlock)
                || originInfo.state().getValue(BarrelBlock.LAYER)
                != net.minecraft.world.level.block.state.properties.AttachFace.FLOOR
                || originInfo.state().getValue(BarrelBlock.INDEX) != 4
                || originInfo.state().getValue(BarrelBlock.FACING)
                != clickedState.getValue(BarrelBlock.FACING)) {
            return null;
        }
        return new BarrelData(origin, originInfo, copyNbt(originInfo));
    }

    public static boolean isBrewing(CompoundTag nbt) {
        return nbt.getInt(BREW_LEVEL) >= IBarrel.BREWING_STARTED;
    }

    public static ItemStack getBarrelOutput(CompoundTag nbt) {
        return readItems(nbt, OUTPUT, 1, 64).getStackInSlot(0);
    }

    public record BarrelData(BlockPos origin,
                             StructureTemplate.StructureBlockInfo info,
                             CompoundTag nbt) {
    }
}
