package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.github.ysbbbbbb.kaleidoscopecookery.api.blockentity.ITeapot;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.TeapotBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.container.TeapotInput;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.serializer.TeapotRecipeSerializer;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModParticles;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModSounds;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionDataUtil;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

public class TeapotBlockMovementBehaviour implements MovementBehaviour {
    private static final String INPUT = "Input";
    private static final String TEA_FLUID_ID = "TeaFluidId";
    private static final String RESULT = "Result";
    private static final String STATUS = "Status";
    private static final String CURRENT_TICK = "CurrentTick";

    @Override
    public void tick(MovementContext context) {
        StructureTemplate.StructureBlockInfo info = context.contraption.getBlocks().get(context.localPos);
        if (info == null || !(info.state().getBlock() instanceof TeapotBlock) || info.nbt() == null) return;

        if (context.world.isClientSide) {
            if (context.contraption.getBlockEntityClientSide(context.localPos)
                    instanceof TeapotBlockEntity blockEntity) {
                boolean boiling = info.nbt().getInt(STATUS) == ITeapot.FINISHED
                        && ContraptionInteractionUtil.hasHeatSource(context);
                blockEntity.boilingState.animateWhen(boiling, (int) context.world.getGameTime());
            }
            return;
        }

        CompoundTag nbt = info.nbt();
        int status = nbt.getInt(STATUS);
        long offset = context.world.getGameTime() + context.localPos.hashCode();
        if (status == ITeapot.FINISHED) {
            if (Math.floorMod(offset, 11) == 0 && ContraptionInteractionUtil.hasHeatSource(context)) {
                playEffects(context, true);
            }
            return;
        }
        if (Math.floorMod(offset, 23) != 0 || !ContraptionInteractionUtil.hasHeatSource(context)) return;

        var fluidId = ResourceLocation.tryParse(nbt.getString(TEA_FLUID_ID));
        if (fluidId == null || fluidId.equals(TeapotRecipeSerializer.EMPTY_TEA_FLUID)) return;

        playEffects(context, false);
        ItemStack input = ItemStack.parseOptional(context.world.registryAccess(), nbt.getCompound(INPUT));
        int currentTick = nbt.getInt(CURRENT_TICK);
        CompoundTag updated = nbt.copy();
        boolean sync = false;

        if (status == ITeapot.PUT_INGREDIENT) {
            if (input.isEmpty()) return;
            if (currentTick > 0) {
                updated.putInt(CURRENT_TICK, Math.max(-1, currentTick - 23));
            } else {
                TeapotInput recipeInput = new TeapotInput(input, fluidId);
                var recipe = context.world.getRecipeManager().getRecipeFor(
                        ModRecipes.TEAPOT_RECIPE, recipeInput, context.world);
                if (recipe.isPresent()) {
                    var value = recipe.get().value();
                    updated.put(RESULT, value.assemble(recipeInput, context.world.registryAccess())
                            .save(context.world.registryAccess(), new CompoundTag()));
                    updated.putInt(CURRENT_TICK, value.time());
                    updated.putInt(STATUS, ITeapot.PROCESSING);
                } else {
                    popResource(context, input);
                    updated.put(INPUT, ItemStack.EMPTY.saveOptional(context.world.registryAccess()));
                    updated.put(RESULT, ItemStack.EMPTY.saveOptional(context.world.registryAccess()));
                    updated.putInt(CURRENT_TICK, -1);
                }
                sync = true;
            }
        } else if (status == ITeapot.PROCESSING) {
            if (currentTick > 0) {
                updated.putInt(CURRENT_TICK, Math.max(-1, currentTick - 23));
            } else {
                updated.putInt(STATUS, ITeapot.FINISHED);
                updated.putInt(CURRENT_TICK, -1);
                sync = true;
            }
        }
        ContraptionDataUtil.updateContraptionData(context, info.state(), updated, sync);
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

    private void popResource(MovementContext context, ItemStack stack) {
        Block.popResource(context.world, BlockPos.containing(globalPos(context)), stack.copy());
    }

    private Vec3 globalPos(MovementContext context) {
        return context.contraption.entity == null ? Vec3.atCenterOf(context.localPos)
                : context.contraption.entity.toGlobalVector(Vec3.atCenterOf(context.localPos), 1);
    }
}
