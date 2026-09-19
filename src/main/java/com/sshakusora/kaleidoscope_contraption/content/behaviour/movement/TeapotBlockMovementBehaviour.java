package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.github.ysbbbbbb.kaleidoscopecookery.api.blockentity.ITeapot;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.TeapotBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.container.TeapotContainer;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.serializer.TeapotRecipeSerializer;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModParticles;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModSounds;
import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.TeacupRegistry;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionDataUtil;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/** Runs Cookery's teapot logic while the teapot is mounted on a contraption. */
public final class TeapotBlockMovementBehaviour implements MovementBehaviour {
    private static final String INPUT = "Input";
    private static final String TEA_FLUID_ID = "TeaFluidId";
    private static final String RESULT = "Result";
    private static final String STATUS = "Status";
    private static final String CURRENT_TICK = "CurrentTick";
    private static final int DRIPSTONE_SEARCH_LENGTH = 11;

    @Override
    public void tick(MovementContext context) {
        StructureTemplate.StructureBlockInfo info = context.contraption.getBlocks().get(context.localPos);
        if (info == null || !(info.state().getBlock() instanceof TeapotBlock) || info.nbt() == null) {
            return;
        }

        if (context.world.isClientSide) {
            if (context.contraption.getBlockEntityClientSide(context.localPos)
                    instanceof TeapotBlockEntity blockEntity) {
                boolean boiling = info.nbt().getInt(STATUS) == ITeapot.FINISHED
                        && ContraptionInteractionUtil.hasHeatSource(context);
                blockEntity.boilingState.animateWhen(boiling, (int) context.world.getGameTime());
            }
            return;
        }

        CompoundTag nbt = info.nbt().copy();
        MountedItemStorage mountedStorage = context.getItemStorage();
        int status = nbt.getInt(STATUS);
        boolean changed = false;
        boolean ingredientInserted = false;

        if (status == ITeapot.PUT_INGREDIENT) {
            ResourceLocation fluidId = ResourceLocation.tryParse(nbt.getString(TEA_FLUID_ID));
            if (isEmptyTeaFluid(fluidId)) {
                Fluid dripstoneFluid = findDripstoneFluid(context);
                ResourceLocation dripstoneId = dripstoneFluid == null
                        ? null : ForgeRegistries.FLUIDS.getKey(dripstoneFluid);
                if (dripstoneId != null) {
                    nbt.putString(TEA_FLUID_ID, dripstoneId.toString());
                    changed = true;
                    playDripstoneEffect(context, dripstoneFluid);
                }
            }

            ingredientInserted = syncMountedIngredient(context, nbt, mountedStorage);
            changed |= ingredientInserted;
        } else {
            // Once processing begins the mounted copy must not retain a second input item.
            clearMountedStorage(mountedStorage);
        }

        status = nbt.getInt(STATUS);
        long offset = context.world.getGameTime() + context.localPos.hashCode();
        if (status == ITeapot.FINISHED) {
            if (Math.floorMod(offset, 11) == 0 && ContraptionInteractionUtil.hasHeatSource(context)) {
                playEffects(context, true);
            }
            if (changed) {
                ContraptionDataUtil.updateContraptionData(context, info.state(), nbt, true);
            }
            return;
        }

        if (Math.floorMod(offset, 23) == 0 && ContraptionInteractionUtil.hasHeatSource(context)
                && !ingredientInserted) {
            ResourceLocation fluidId = ResourceLocation.tryParse(nbt.getString(TEA_FLUID_ID));
            if (!isEmptyTeaFluid(fluidId)) {
                playEffects(context, false);
                ItemStack input = ItemStack.of(nbt.getCompound(INPUT));
                int currentTick = nbt.getInt(CURRENT_TICK);

                if (status == ITeapot.PUT_INGREDIENT && !input.isEmpty()) {
                    if (currentTick > 0) {
                        nbt.putInt(CURRENT_TICK, Math.max(-1, currentTick - 23));
                        changed = true;
                    } else {
                        TeapotContainer recipeInput = new TeapotContainer(input, fluidId);
                        var recipe = context.world.getRecipeManager().getRecipeFor(
                                ModRecipes.TEAPOT_RECIPE, recipeInput, context.world);
                        if (recipe.isPresent()) {
                            nbt.put(RESULT, recipe.get().assemble(
                                    recipeInput, context.world.registryAccess()).serializeNBT());
                            nbt.putInt(CURRENT_TICK, recipe.get().time());
                        } else {
                            ItemStack mysteryTea = new ItemStack(
                                    TeacupRegistry.getItem(TeacupRegistry.MYSTERY_TEA), 4);
                            nbt.put(RESULT, mysteryTea.serializeNBT());
                            nbt.putInt(CURRENT_TICK, TeapotRecipeSerializer.DEFAULT_TIME);
                        }
                        nbt.putInt(STATUS, ITeapot.PROCESSING);
                        clearMountedStorage(mountedStorage);
                        changed = true;
                    }
                } else if (status == ITeapot.PROCESSING) {
                    if (currentTick > 0) {
                        nbt.putInt(CURRENT_TICK, Math.max(-1, currentTick - 23));
                    } else {
                        nbt.putInt(STATUS, ITeapot.FINISHED);
                        nbt.putInt(CURRENT_TICK, -1);
                    }
                    changed = true;
                }
            }
        }

        if (changed) {
            ContraptionDataUtil.updateContraptionData(context, info.state(), nbt, true);
        }
    }

    /** Mirrors Create's mutable mounted slot into Cookery's one-item input. */
    private boolean syncMountedIngredient(MovementContext context, CompoundTag nbt,
                                          MountedItemStorage mountedStorage) {
        if (mountedStorage == null || mountedStorage.getSlots() == 0) {
            return false;
        }

        ItemStack input = ItemStack.of(nbt.getCompound(INPUT));
        ResourceLocation fluidId = ResourceLocation.tryParse(nbt.getString(TEA_FLUID_ID));
        if (input.isEmpty()) {
            if (isEmptyTeaFluid(fluidId)) {
                return false;
            }

            ItemStack mounted = mountedStorage.getStackInSlot(0);
            if (mounted.isEmpty()) {
                return false;
            }

            ItemStack accepted = mounted.copyWithCount(1);
            nbt.put(INPUT, accepted.serializeNBT());
            nbt.putInt(CURRENT_TICK, TeapotBlockEntity.INGREDIENT_TIME);

            ItemStack remainder = mounted.copy();
            remainder.shrink(1);
            mountedStorage.setStackInSlot(0, remainder);
            return true;
        }

        // Keep the mounted copy at the same one-item shape as the real teapot.
        ItemStack expected = input.copyWithCount(1);
        ItemStack mounted = mountedStorage.getStackInSlot(0);
        if (!ItemStack.isSameItemSameTags(expected, mounted) || mounted.getCount() != 1) {
            mountedStorage.setStackInSlot(0, expected);
        }
        return false;
    }

    private void clearMountedStorage(MountedItemStorage mountedStorage) {
        if (mountedStorage != null && mountedStorage.getSlots() > 0
                && !mountedStorage.getStackInSlot(0).isEmpty()) {
            mountedStorage.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    /** Searches the virtual contraption blocks for a water or lava source above the teapot. */
    private Fluid findDripstoneFluid(MovementContext context) {
        BlockPos tip = null;
        for (int distance = 1; distance < DRIPSTONE_SEARCH_LENGTH; distance++) {
            BlockPos candidate = context.localPos.above(distance);
            BlockState state = getVirtualState(context, candidate);
            if (PointedDripstoneBlock.canDrip(state)) {
                tip = candidate;
                break;
            }
            if (!state.isAir() && !state.is(Blocks.POINTED_DRIPSTONE)) {
                return null;
            }
        }
        if (tip == null) {
            return null;
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
                return Fluids.WATER;
            }
            FluidState fluidState = state.getFluidState();
            Fluid fluid = fluidState.getType();
            return fluidState.isSource() && (fluid == Fluids.WATER || fluid == Fluids.LAVA) ? fluid : null;
        }
        return null;
    }

    private BlockState getVirtualState(MovementContext context, BlockPos localPos) {
        StructureTemplate.StructureBlockInfo info = context.contraption.getBlocks().get(localPos);
        return info == null ? Blocks.AIR.defaultBlockState() : info.state();
    }

    private boolean isEmptyTeaFluid(ResourceLocation fluidId) {
        return fluidId == null || fluidId.equals(TeapotRecipeSerializer.EMPTY_TEA_FLUID);
    }

    private void playDripstoneEffect(MovementContext context, Fluid fluid) {
        BlockPos pos = BlockPos.containing(globalPos(context));
        context.world.levelEvent(
                fluid == Fluids.LAVA
                        ? LevelEvent.SOUND_DRIP_LAVA_INTO_CAULDRON
                        : LevelEvent.SOUND_DRIP_WATER_INTO_CAULDRON,
                pos, 0);
    }

    private void playEffects(MovementContext context, boolean boiling) {
        Vec3 pos = globalPos(context);
        context.world.playSound(null, BlockPos.containing(pos),
                boiling ? SoundEvents.FIRE_EXTINGUISH : ModSounds.BLOCK_TEAPOT_PROCESSING.get(),
                SoundSource.BLOCKS, boiling ? 0.4f : 0.6f, 0.8f + context.world.random.nextFloat() * 0.2f);
        if (context.world instanceof ServerLevel level) {
            level.sendParticles(ModParticles.COOKING.get(), pos.x, pos.y + 0.3, pos.z,
                    boiling ? 3 : 1, 0.15, 0.1, 0.15, 0.02);
        }
    }

    private Vec3 globalPos(MovementContext context) {
        return context.contraption.entity == null ? Vec3.atCenterOf(context.localPos)
                : context.contraption.entity.toGlobalVector(Vec3.atCenterOf(context.localPos), 1);
    }
}
