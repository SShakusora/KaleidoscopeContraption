package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.github.ysbbbbbb.kaleidoscopetavern.api.blockentity.IBarrel;
import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.BarrelBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.crafting.container.BarrelRecipeContainer;
import com.github.ysbbbbbb.kaleidoscopetavern.crafting.recipe.BarrelRecipe;
import com.github.ysbbbbbb.kaleidoscopetavern.crafting.serializer.BarrelRecipeSerializer;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModRecipes;
import com.github.ysbbbbbb.kaleidoscopetavern.item.BottleBlockItem;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.TavernBrewingContraptionSupport;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionDataUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class TavernBarrelMovementBehaviour implements MovementBehaviour {
    private static final int CHECK_INTERVAL = 97;

    @Override
    public void tick(MovementContext context) {
        if (context.world.isClientSide) {
            return;
        }
        StructureTemplate.StructureBlockInfo info =
                context.contraption.getBlocks().get(context.localPos);
        if (info == null || !(info.state().getBlock() instanceof BarrelBlock)
                || info.state().getValue(BarrelBlock.LAYER) != AttachFace.FLOOR
                || info.state().getValue(BarrelBlock.INDEX) != 4) {
            return;
        }

        CompoundTag nbt = TavernBrewingContraptionSupport.copyNbt(info);
        boolean open = nbt.contains(TavernBrewingContraptionSupport.OPEN)
                ? nbt.getBoolean(TavernBrewingContraptionSupport.OPEN) : true;
        int brewLevel = BottleBlockItem.clampBrewLevel(
                nbt.getInt(TavernBrewingContraptionSupport.BREW_LEVEL));
        if (open || brewLevel >= IBarrel.BREWING_FINISHED) {
            return;
        }

        long tick = context.world.getGameTime() + context.localPos.hashCode();
        if (Math.floorMod(tick, CHECK_INTERVAL) != 0) {
            return;
        }

        if (brewLevel >= IBarrel.BREWING_STARTED) {
            tickBrewing(context, info, nbt, brewLevel);
            return;
        }

        FluidTank fluid = TavernBrewingContraptionSupport.readFluid(
                nbt, IBarrel.MAX_FLUID_AMOUNT, context.world.registryAccess());
        if (fluid.getFluidAmount() < IBarrel.MAX_FLUID_AMOUNT) {
            return;
        }
        startBrewing(context, info, nbt, fluid);
    }

    private void tickBrewing(MovementContext context,
                             StructureTemplate.StructureBlockInfo info,
                             CompoundTag nbt, int brewLevel) {
        int brewTime = nbt.getInt(TavernBrewingContraptionSupport.BREW_TIME);
        CompoundTag updated = nbt.copy();
        if (brewTime > 0) {
            updated.putInt(TavernBrewingContraptionSupport.BREW_TIME,
                    brewTime - CHECK_INTERVAL);
            ContraptionDataUtil.updateContraptionData(
                    context, info.state(), updated, false);
            return;
        }

        int nextLevel = Math.min(brewLevel + 1, IBarrel.BREWING_FINISHED);
        updated.putInt(TavernBrewingContraptionSupport.BREW_LEVEL, nextLevel);
        updated.putInt(TavernBrewingContraptionSupport.BREW_TIME,
                getBrewTime(context, updated, nextLevel));
        ContraptionDataUtil.updateContraptionData(context, info.state(), updated, true);
    }

    private void startBrewing(MovementContext context,
                              StructureTemplate.StructureBlockInfo info,
                              CompoundTag nbt, FluidTank fluid) {
        ItemStackHandler ingredient = TavernBrewingContraptionSupport.readItems(
                nbt, TavernBrewingContraptionSupport.INGREDIENT,
                IBarrel.MAX_ITEM_SLOTS, 16, context.world.registryAccess());
        BarrelRecipeContainer container = new BarrelRecipeContainer(ingredient, fluid);
        RecipeHolder<BarrelRecipe> recipeHolder = context.world.getRecipeManager()
                .getRecipeFor(ModRecipes.BARREL_RECIPE, container, context.world)
                .orElse(null);

        ItemStack output;
        ResourceLocation recipeId;
        int unitTime;
        if (recipeHolder == null) {
            output = new ItemStack(ModItems.VINEGAR.get(), 16);
            recipeId = BarrelRecipeSerializer.EMPTY_RECIPE_ID;
            unitTime = BarrelRecipeSerializer.DEFAULT_UNIT_TIME;
        } else {
            BarrelRecipe recipe = recipeHolder.value();
            output = recipe.assemble(container, context.world.registryAccess());
            recipeId = recipeHolder.id();
            unitTime = recipe.unitTime();
        }

        for (int slot = 0; slot < ingredient.getSlots(); slot++) {
            ingredient.setStackInSlot(slot, ItemStack.EMPTY);
        }
        fluid.drain(fluid.getFluidAmount(), IFluidHandler.FluidAction.EXECUTE);
        ItemStackHandler outputHandler = new ItemStackHandler(1);
        outputHandler.setStackInSlot(0, output);

        CompoundTag updated = nbt.copy();
        TavernBrewingContraptionSupport.writeItems(
                updated, TavernBrewingContraptionSupport.INGREDIENT, ingredient,
                context.world.registryAccess());
        TavernBrewingContraptionSupport.writeItems(
                updated, TavernBrewingContraptionSupport.OUTPUT, outputHandler,
                context.world.registryAccess());
        TavernBrewingContraptionSupport.writeFluid(updated, fluid, context.world.registryAccess());
        updated.putString(TavernBrewingContraptionSupport.RECIPE_ID, recipeId.toString());
        updated.putInt(TavernBrewingContraptionSupport.BREW_LEVEL,
                IBarrel.BREWING_STARTED);
        updated.putInt(TavernBrewingContraptionSupport.BREW_TIME, unitTime);
        ContraptionDataUtil.updateContraptionData(context, info.state(), updated, true);
    }

    private int getBrewTime(MovementContext context, CompoundTag nbt, int brewLevel) {
        if (brewLevel >= IBarrel.BREWING_FINISHED) {
            return -1;
        }
        ResourceLocation id = ResourceLocation.tryParse(
                nbt.getString(TavernBrewingContraptionSupport.RECIPE_ID));
        if (id == null || id.equals(BarrelRecipeSerializer.EMPTY_RECIPE_ID)) {
            return BarrelRecipeSerializer.DEFAULT_UNIT_TIME * brewLevel;
        }
        return context.world.getRecipeManager().byKey(id)
                .map(RecipeHolder::value)
                .filter(BarrelRecipe.class::isInstance)
                .map(BarrelRecipe.class::cast)
                .map(recipe -> recipe.unitTime() * brewLevel)
                .orElse(BarrelRecipeSerializer.DEFAULT_UNIT_TIME * brewLevel);
    }
}
