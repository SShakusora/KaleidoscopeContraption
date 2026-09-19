package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.BambooTrayBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.BambooTrayRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionDataUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static net.minecraftforge.common.Tags.Blocks.GLASS_COLORLESS;
import static net.minecraftforge.common.Tags.Blocks.GLASS_PANES_COLORLESS;

/** Runs Cookery's weather-driven bamboo tray recipes while the tray is mounted. */
public final class BambooTrayBlockMovementBehaviour implements MovementBehaviour {
    private static final String ITEMS_TAG = "Items";
    private static final String PROGRESS_TAG = "ProcessingProgress";
    private static final String COMPLETION_STATES_TAG = "CompletionStates";
    private static final int SLOT_COUNT = 4;
    private static final int TICK_INTERVAL = 19;
    private static final int DRIPSTONE_SEARCH_LENGTH = 11;

    @Override
    public void tick(MovementContext context) {
        if (context.world.isClientSide) {
            return;
        }

        StructureTemplate.StructureBlockInfo info = context.contraption.getBlocks().get(context.localPos);
        if (info == null || !(info.state().getBlock() instanceof BambooTrayBlock) || info.nbt() == null) {
            return;
        }

        BlockPos worldPos = getWorldPos(context);
        if ((context.world.getGameTime() + worldPos.getX() + worldPos.getY() + worldPos.getZ()) % TICK_INTERVAL != 0) {
            return;
        }

        boolean wetting = context.world.isRainingAt(worldPos.above())
                || hasWaterDripstone(context.world, worldPos)
                || hasVirtualWaterDripstone(context);
        boolean drying = !context.world.isRaining() && hasDryingExposure(context.world, worldPos);
        if (!wetting && !drying) {
            return;
        }

        BambooTrayRecipe.Subtype subtype = wetting
                ? BambooTrayRecipe.Subtype.WETTING
                : BambooTrayRecipe.Subtype.DRYING;
        MountedItemStorage mountedStorage = context.getItemStorage();
        NonNullList<ItemStack> items = readItems(info.nbt(), mountedStorage);
        int[] progress = normalizeProgress(info.nbt().getIntArray(PROGRESS_TAG));
        byte[] completion = normalizeCompletion(info.nbt().getByteArray(COMPLETION_STATES_TAG));
        List<BambooTrayRecipe> recipes = context.world.getRecipeManager()
                .getAllRecipesFor(ModRecipes.BAMBOO_TRAY_RECIPE);

        boolean changed = false;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack input = items.get(slot);
            byte completionState = completion[slot];
            if (input.isEmpty() || matches(completionState, subtype)) {
                continue;
            }

            BambooTrayRecipe recipe = recipes.stream()
                    .filter(candidate -> candidate.getSubtype() == subtype)
                    .filter(candidate -> candidate.getIngredient().test(input))
                    .findFirst()
                    .orElse(null);
            if (recipe == null) {
                if (progress[slot] != 0) {
                    progress[slot] = 0;
                    changed = true;
                }
                continue;
            }

            if (completionState != 0) {
                progress[slot] = 0;
                completion[slot] = 0;
            }
            progress[slot] += TICK_INTERVAL;
            changed = true;

            if (progress[slot] >= recipe.getDuration()) {
                ItemStack result = recipe.getResult().copy();
                result.setCount(Math.min(result.getMaxStackSize(), result.getCount() * input.getCount()));
                items.set(slot, result);
                progress[slot] = recipe.getDuration();
                completion[slot] = completionValue(subtype);
            }
        }

        if (!changed) {
            return;
        }

        CompoundTag newNbt = info.nbt().copy();
        ContainerHelper.saveAllItems(newNbt, items, true);
        newNbt.putIntArray(PROGRESS_TAG, progress);
        newNbt.putByteArray(COMPLETION_STATES_TAG, completion);
        if (mountedStorage != null && mountedStorage.getSlots() >= SLOT_COUNT) {
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                mountedStorage.setStackInSlot(slot, items.get(slot).copy());
            }
        }
        ContraptionDataUtil.updateContraptionData(context, info.state(), newNbt, true);
    }

    private BlockPos getWorldPos(MovementContext context) {
        if (context.contraption.entity == null) {
            return context.localPos;
        }
        Vec3 global = context.contraption.entity.toGlobalVector(Vec3.atCenterOf(context.localPos), 1.0F);
        return BlockPos.containing(global);
    }

    private boolean matches(byte completionState, BambooTrayRecipe.Subtype subtype) {
        return (completionState == 1 && subtype == BambooTrayRecipe.Subtype.DRYING)
                || (completionState == 2 && subtype == BambooTrayRecipe.Subtype.WETTING);
    }

    private byte completionValue(BambooTrayRecipe.Subtype subtype) {
        return subtype == BambooTrayRecipe.Subtype.DRYING ? (byte) 1 : (byte) 2;
    }

    private int[] normalizeProgress(int[] source) {
        int[] result = new int[SLOT_COUNT];
        System.arraycopy(source, 0, result, 0, Math.min(source.length, result.length));
        return result;
    }

    private byte[] normalizeCompletion(byte[] source) {
        byte[] result = new byte[SLOT_COUNT];
        System.arraycopy(source, 0, result, 0, Math.min(source.length, result.length));
        return result;
    }

    private NonNullList<ItemStack> readItems(CompoundTag nbt, MountedItemStorage mountedStorage) {
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        if (mountedStorage != null && mountedStorage.getSlots() >= SLOT_COUNT) {
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                items.set(slot, mountedStorage.getStackInSlot(slot).copy());
            }
            return items;
        }
        if (nbt.contains(ITEMS_TAG, Tag.TAG_LIST)) {
            ContainerHelper.loadAllItems(nbt, items);
        }
        return items;
    }

    private boolean hasDryingExposure(Level level, BlockPos pos) {
        BlockPos above = pos.above();
        if (level.canSeeSky(above)) {
            return true;
        }
        for (int y = above.getY(); y < level.getMaxBuildHeight(); y++) {
            BlockState state = level.getBlockState(new BlockPos(pos.getX(), y, pos.getZ()));
            if (state.isAir() || state.is(GLASS_COLORLESS) || state.is(GLASS_PANES_COLORLESS)
                    || state.is(ModBlocks.BAMBOO_TRAY.get())) {
                continue;
            }
            return false;
        }
        return true;
    }

    private boolean hasWaterDripstone(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        BlockPos tipPos = PointedDripstoneBlock.findStalactiteTipAboveCauldron(level, pos);
        return tipPos != null
                && PointedDripstoneBlock.getCauldronFillFluidType(serverLevel, tipPos) == Fluids.WATER;
    }

    private boolean hasVirtualWaterDripstone(MovementContext context) {
        BlockPos tip = null;
        for (int distance = 1; distance < DRIPSTONE_SEARCH_LENGTH; distance++) {
            BlockPos candidate = context.localPos.above(distance);
            BlockState state = getVirtualState(context, candidate);
            if (PointedDripstoneBlock.canDrip(state)) {
                tip = candidate;
                break;
            }
            if (!state.isAir() && !state.is(Blocks.POINTED_DRIPSTONE)) {
                return false;
            }
        }
        if (tip == null) {
            return false;
        }

        for (int distance = 0; distance < DRIPSTONE_SEARCH_LENGTH; distance++) {
            BlockPos above = tip.above();
            BlockState state = getVirtualState(context, above);
            if (state.is(Blocks.POINTED_DRIPSTONE)
                    && state.hasProperty(PointedDripstoneBlock.TIP_DIRECTION)
                    && state.getValue(PointedDripstoneBlock.TIP_DIRECTION) == Direction.DOWN) {
                tip = above;
                continue;
            }
            if (state.is(Blocks.MUD) && !context.world.dimensionType().ultraWarm()) {
                return true;
            }
            return state.getFluidState().isSource() && state.getFluidState().getType() == Fluids.WATER;
        }
        return false;
    }

    private BlockState getVirtualState(MovementContext context, BlockPos localPos) {
        StructureTemplate.StructureBlockInfo info = context.contraption.getBlocks().get(localPos);
        return info == null ? Blocks.AIR.defaultBlockState() : info.state();
    }
}
