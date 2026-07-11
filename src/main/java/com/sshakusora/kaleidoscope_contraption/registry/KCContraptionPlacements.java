package com.sshakusora.kaleidoscope_contraption.registry;

import com.github.ysbbbbbb.kaleidoscopecookery.advancements.critereon.ModEventTriggerType;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.PotBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.SteamerBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.StockpotBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.PotRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.StockpotRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModSoupBases;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModTrigger;
import com.sshakusora.kaleidoscope_contraption.KaleidoscopeContraption;
import com.sshakusora.kaleidoscope_contraption.api.placement.*;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;
import java.util.Optional;

public final class KCContraptionPlacements {
    public static final ResourceLocation STOVE_TOP = KaleidoscopeContraption.asResource("stove_top");
    public static final ResourceLocation STOVE_TOP_POT = KaleidoscopeContraption.asResource("stove_top/pot");
    public static final ResourceLocation STOVE_TOP_STOCKPOT = KaleidoscopeContraption.asResource("stove_top/stockpot");
    public static final ResourceLocation STOVE_TOP_STEAMER = KaleidoscopeContraption.asResource("stove_top/steamer");

    private KCContraptionPlacements() {
    }

    public static void registerDefaults() {
        ContraptionPlacementRegistry.register(STOVE_TOP, STOVE_TOP_POT, new ContraptionPlacementRule() {
            @Override
            public boolean matches(ContraptionPlacementContext context) {
                return Block.byItem(context.heldItem().getItem()) instanceof PotBlock;
            }

            @Override
            public ContraptionPlacementResult createPlacement(ContraptionPlacementContext context) {
                PotBlock block = (PotBlock) Block.byItem(context.heldItem().getItem());
                BlockState state = block.defaultBlockState()
                        .setValue(PotBlock.FACING, context.player().getDirection().getOpposite())
                        .setValue(PotBlock.HAS_BASE, false);

                CompoundTag nbt = new CompoundTag();
                nbt.put("Inputs", ContainerHelper.saveAllItems(new CompoundTag(),
                        NonNullList.withSize(PotRecipe.RECIPES_SIZE, ItemStack.EMPTY)));
                nbt.putString("Carrier", Ingredient.EMPTY.toJson().toString());
                nbt.put("Result", ItemStack.EMPTY.serializeNBT());
                nbt.putInt("Status", 0);
                nbt.putInt("CurrentTick", 0);
                nbt.putInt("StirFryCount", 0);
                nbt.putLong("Seed", System.currentTimeMillis());
                nbt.putString("id", "kaleidoscope_cookery:pot");
                return single(context, state, nbt);
            }

            @Override
            public void afterPlaced(ContraptionPlacementContext context, ContraptionPlacementResult result) {
                if (isLit(context)) {
                    ModTrigger.EVENT.trigger(context.player(), ModEventTriggerType.PLACE_POT_ON_HEAT_SOURCE);
                }
            }

            @Override
            public Optional<ContraptionRemovalResult> createRemoval(ContraptionRemovalContext context) {
                BlockState state = context.targetInfo().state();
                CompoundTag nbt = context.targetInfo().nbt();
                if (!(state.getBlock() instanceof PotBlock)
                        || state.getValue(PotBlock.HAS_OIL)
                        || (nbt != null && nbt.getInt("Status") != 0)
                        || !isContainerEmpty(nbt, "Inputs", PotRecipe.RECIPES_SIZE)
                        || (nbt != null && !ItemStack.of(nbt.getCompound("Result")).isEmpty())) {
                    return Optional.empty();
                }
                return Optional.of(ContraptionRemovalResult.single(
                        context.targetPos(), new ItemStack(ModBlocks.POT.get())));
            }
        });

        ContraptionPlacementRegistry.register(STOVE_TOP, STOVE_TOP_STOCKPOT, new ContraptionPlacementRule() {
            @Override
            public boolean matches(ContraptionPlacementContext context) {
                return Block.byItem(context.heldItem().getItem()) instanceof StockpotBlock;
            }

            @Override
            public ContraptionPlacementResult createPlacement(ContraptionPlacementContext context) {
                StockpotBlock block = (StockpotBlock) Block.byItem(context.heldItem().getItem());
                BlockState state = block.defaultBlockState()
                        .setValue(StockpotBlock.FACING, context.player().getDirection().getOpposite())
                        .setValue(StockpotBlock.HAS_BASE, false);

                CompoundTag nbt = new CompoundTag();
                nbt.put("Inputs", ContainerHelper.saveAllItems(new CompoundTag(),
                        NonNullList.withSize(StockpotRecipe.RECIPES_SIZE, ItemStack.EMPTY)));
                nbt.putString("RecipeId", "kaleidoscope_cookery:stockpot/empty");
                nbt.putString("SoupBaseId", ModSoupBases.WATER.toString());
                nbt.put("Result", ItemStack.EMPTY.serializeNBT());
                nbt.putInt("Status", 0);
                nbt.putInt("CurrentTick", -1);
                nbt.putInt("TakeoutCount", 0);
                nbt.putString("id", "kaleidoscope_cookery:stockpot");
                return single(context, state, nbt);
            }

            @Override
            public void afterPlaced(ContraptionPlacementContext context, ContraptionPlacementResult result) {
                if (isLit(context)) {
                    ModTrigger.EVENT.trigger(context.player(), ModEventTriggerType.PLACE_STOCKPOT_ON_HEAT_SOURCE);
                }
            }

            @Override
            public Optional<ContraptionRemovalResult> createRemoval(ContraptionRemovalContext context) {
                BlockState state = context.targetInfo().state();
                if (!(state.getBlock() instanceof StockpotBlock) || state.getValue(StockpotBlock.HAS_LID)) {
                    return Optional.empty();
                }

                CompoundTag nbt = context.targetInfo().nbt();
                int status = nbt == null ? 0 : nbt.getInt("Status");
                if (status != 0 && status != 1) {
                    return Optional.empty();
                }
                if (status == 1 && !isContainerEmpty(nbt, "Inputs", StockpotRecipe.RECIPES_SIZE)) {
                    return Optional.empty();
                }
                return Optional.of(ContraptionRemovalResult.single(
                        context.targetPos(), new ItemStack(ModBlocks.STOCKPOT.get())));
            }
        });

        ContraptionPlacementRegistry.register(STOVE_TOP, STOVE_TOP_STEAMER, new ContraptionPlacementRule() {
            @Override
            public boolean matches(ContraptionPlacementContext context) {
                return Block.byItem(context.heldItem().getItem()) instanceof SteamerBlock;
            }

            @Override
            public ContraptionPlacementResult createPlacement(ContraptionPlacementContext context) {
                SteamerBlock block = (SteamerBlock) Block.byItem(context.heldItem().getItem());
                BlockState state = block.defaultBlockState()
                        .setValue(SteamerBlock.FACING, context.player().getDirection().getOpposite())
                        .setValue(SteamerBlock.HALF, true)
                        .setValue(SteamerBlock.HAS_LID, false)
                        .setValue(SteamerBlock.HAS_BASE, false)
                        .setValue(SteamerBlock.WATERLOGGED, false);
                return single(context, state, createSteamerNbt(context.heldItem()));
            }

            @Override
            public Optional<ContraptionRemovalResult> createRemoval(ContraptionRemovalContext context) {
                StructureTemplate.StructureBlockInfo info = context.targetInfo();
                if (!(info.state().getBlock() instanceof SteamerBlock)) {
                    return Optional.empty();
                }
                ItemStack returnedItem = createSteamerItem(info);
                if (info.state().getValue(SteamerBlock.HALF)) {
                    return Optional.of(ContraptionRemovalResult.single(context.targetPos(), returnedItem));
                }
                return Optional.of(new ContraptionRemovalResult(
                        List.of(), List.of(reduceSteamerToSingleLayer(info)), List.of(returnedItem), true));
            }
        });
    }

    private static ContraptionPlacementResult single(ContraptionPlacementContext context, BlockState state,
                                                     CompoundTag nbt) {
        return ContraptionPlacementResult.single(new StructureTemplate.StructureBlockInfo(
                context.targetPos(), state, nbt));
    }

    private static boolean isLit(ContraptionPlacementContext context) {
        BlockState state = context.supportInfo().state();
        return state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
    }

    private static CompoundTag createSteamerNbt(ItemStack heldItem) {
        CompoundTag nbt = new CompoundTag();
        CompoundTag handData = BlockItem.getBlockEntityData(heldItem);
        NonNullList<ItemStack> items = NonNullList.withSize(8, ItemStack.EMPTY);
        int[] progress = new int[8];
        int[] time = new int[8];

        if (handData != null) {
            NonNullList<ItemStack> handItems = NonNullList.withSize(4, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(handData, handItems);
            for (int i = 0; i < handItems.size(); i++) {
                items.set(i, handItems.get(i));
            }

            int[] handProgress = handData.getIntArray("CookingProgress");
            int[] handTime = handData.getIntArray("CookingTime");
            System.arraycopy(handProgress, 0, progress, 0, Math.min(4, handProgress.length));
            System.arraycopy(handTime, 0, time, 0, Math.min(4, handTime.length));
        }

        ContainerHelper.saveAllItems(nbt, items, true);
        nbt.putIntArray("CookingProgress", progress);
        nbt.putIntArray("CookingTime", time);
        nbt.putString("id", "kaleidoscope_cookery:steamer");
        return nbt;
    }

    private static boolean isContainerEmpty(CompoundTag nbt, String tagName, int size) {
        if (nbt == null || !nbt.contains(tagName, Tag.TAG_COMPOUND)) {
            return true;
        }
        NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbt.getCompound(tagName), items);
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    private static ItemStack createSteamerItem(StructureTemplate.StructureBlockInfo info) {
        ItemStack stack = ModItems.STEAMER.get().getDefaultInstance();
        CompoundTag nbt = info.nbt() == null ? new CompoundTag() : info.nbt();
        NonNullList<ItemStack> items = NonNullList.withSize(8, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbt, items);

        int[] cookingProgress = normalizeArray(nbt.getIntArray("CookingProgress"), 8);
        int[] cookingTime = normalizeArray(nbt.getIntArray("CookingTime"), 8);
        boolean half = info.state().getValue(SteamerBlock.HALF);
        int startIndex = half ? 0 : 4;

        NonNullList<ItemStack> savedItems = NonNullList.withSize(4, ItemStack.EMPTY);
        int[] savedProgress = new int[4];
        int[] savedTime = new int[4];
        for (int i = 0; i < 4; i++) {
            savedItems.set(i, items.get(startIndex + i));
            savedProgress[i] = cookingProgress[startIndex + i];
            savedTime[i] = cookingTime[startIndex + i];
        }

        CompoundTag blockEntityData = new CompoundTag();
        ContainerHelper.saveAllItems(blockEntityData, savedItems, false);
        blockEntityData.putIntArray("CookingProgress", savedProgress);
        blockEntityData.putIntArray("CookingTime", savedTime);
        BlockItem.setBlockEntityData(stack, ModBlocks.STEAMER_BE.get(), blockEntityData);
        return stack;
    }

    private static StructureTemplate.StructureBlockInfo reduceSteamerToSingleLayer(
            StructureTemplate.StructureBlockInfo info) {
        CompoundTag nbt = info.nbt() == null ? new CompoundTag() : info.nbt().copy();
        NonNullList<ItemStack> items = NonNullList.withSize(8, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbt, items);
        int[] cookingProgress = normalizeArray(nbt.getIntArray("CookingProgress"), 8);
        int[] cookingTime = normalizeArray(nbt.getIntArray("CookingTime"), 8);
        for (int i = 4; i < 8; i++) {
            items.set(i, ItemStack.EMPTY);
            cookingProgress[i] = 0;
            cookingTime[i] = 0;
        }
        ContainerHelper.saveAllItems(nbt, items, true);
        nbt.putIntArray("CookingProgress", cookingProgress);
        nbt.putIntArray("CookingTime", cookingTime);
        BlockState state = info.state().setValue(SteamerBlock.HALF, true);
        return new StructureTemplate.StructureBlockInfo(info.pos(), state, nbt);
    }

    private static int[] normalizeArray(int[] source, int size) {
        if (source.length >= size) {
            return source;
        }
        int[] result = new int[size];
        System.arraycopy(source, 0, result, 0, source.length);
        return result;
    }
}
