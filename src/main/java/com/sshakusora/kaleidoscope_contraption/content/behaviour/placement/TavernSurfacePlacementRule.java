package com.sshakusora.kaleidoscope_contraption.content.behaviour.placement;

import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.BottleBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.DrinkBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.MolotovBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.PotionBottleBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.IncenseBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.StringLightsBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.mixology.CocktailBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.mixology.GlasswareBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.mixology.ShakerBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.mixology.SignatureCocktailBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.brew.DrinkBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.brew.PotionBottleBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.mixology.ShakerBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.mixology.SignatureCocktailBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopetavern.item.ShakerItem;
import com.github.ysbbbbbb.kaleidoscopetavern.item.SignatureCocktailBlockItem;
import com.sshakusora.kaleidoscope_contraption.api.placement.*;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction.TavernBottleBlockMovingInteraction;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.ysbbbbbb.kaleidoscopetavern.config.GeneralConfig.*;

/**
 * Places Tavern's small drink and mixology blocks on a contraption surface.
 */
public class TavernSurfacePlacementRule implements ContraptionPlacementRule {
    enum PlacementKind {
        DRINK,
        SIGNATURE_COCKTAIL,
        COCKTAIL,
        MOLOTOV,
        GLASSWARE,
        SHAKER,
        POTION_BOTTLE,
        BOTTLE,
        INCENSE,
        STRING_LIGHTS
    }

    @Override
    public boolean matches(ContraptionPlacementContext context) {
        return context.player().isShiftKeyDown()
                && getPlacementKind(context.heldItem()) != null;
    }

    @Override
    public ContraptionPlacementResult createPlacement(ContraptionPlacementContext context) {
        PlacementKind kind = getPlacementKind(context.heldItem());
        if (kind == null) {
            throw new IllegalStateException("Tavern placement rule matched an unsupported item");
        }

        BlockState state;
        CompoundTag nbt;
        switch (kind) {
            case DRINK -> {
                DrinkBlock block = (DrinkBlock) Block.byItem(context.heldItem().getItem());
                state = orient(block.defaultBlockState(), context);
                DrinkBlockEntity blockEntity = new DrinkBlockEntity(context.targetPos(), state);
                if (!blockEntity.addItem(context.heldItem())) {
                    throw new IllegalStateException("Unable to initialize Tavern drink block");
                }
                nbt = saveBlockEntity(blockEntity);
            }
            case SIGNATURE_COCKTAIL -> {
                SignatureCocktailBlock block = (SignatureCocktailBlock) Block.byItem(
                        context.heldItem().getItem());
                state = orient(block.defaultBlockState(), context);
                SignatureCocktailBlockEntity blockEntity = new SignatureCocktailBlockEntity(
                        context.targetPos(), state);
                blockEntity.setEffects(SignatureCocktailBlockItem.getEffects(context.heldItem()));
                blockEntity.setColor(SignatureCocktailBlockItem.getColor(context.heldItem()));
                nbt = saveBlockEntity(blockEntity);
            }
            case COCKTAIL, MOLOTOV, GLASSWARE -> {
                Block block = Block.byItem(context.heldItem().getItem());
                state = orient(block.defaultBlockState(), context);
                nbt = new CompoundTag();
            }
            case SHAKER -> {
                ShakerBlock block = (ShakerBlock) Block.byItem(context.heldItem().getItem());
                state = block.defaultBlockState();
                ShakerBlockEntity blockEntity = new ShakerBlockEntity(context.targetPos(), state);
                blockEntity.setStorage(ShakerItem.getStorage(context.heldItem()));
                if (ShakerItem.hasResult(context.heldItem())) {
                    blockEntity.setResult(ShakerItem.getResult(context.heldItem()));
                }
                nbt = saveBlockEntity(blockEntity);
            }
            case POTION_BOTTLE -> {
                state = orient(ModBlocks.POTION_BOTTLE.get().defaultBlockState(), context);
                PotionBottleBlockEntity blockEntity = new PotionBottleBlockEntity(context.targetPos(), state);
                blockEntity.setPotionStack(context.heldItem());
                nbt = saveBlockEntity(blockEntity);
            }
            case BOTTLE -> {
                Block block = getBottleBlockForPlacement(context.heldItem());
                if (!(block instanceof BottleBlock)) {
                    throw new IllegalStateException("Unable to resolve Tavern bottle placement");
                }
                state = orient(block.defaultBlockState(), context);
                nbt = new CompoundTag();
            }
            case INCENSE, STRING_LIGHTS -> {
                Block block = Block.byItem(context.heldItem().getItem());
                state = orient(block.defaultBlockState(), context);
                nbt = new CompoundTag();
            }
            default -> throw new IllegalStateException("Unhandled Tavern placement kind: " + kind);
        }

        return ContraptionPlacementResult.single(new StructureTemplate.StructureBlockInfo(
                context.targetPos(), state, nbt));
    }

    @Override
    public Optional<ContraptionRemovalResult> createRemoval(ContraptionRemovalContext context) {
        BlockState state = context.targetInfo().state();
        Block block = state.getBlock();

        if (block instanceof DrinkBlock drinkBlock) {
            DrinkBlockEntity blockEntity = load(new DrinkBlockEntity(context.targetPos(), state),
                    context.targetInfo().nbt());
            List<ItemStack> returnedItems = new ArrayList<>();
            blockEntity.getItems().stream()
                    .filter(stack -> !stack.isEmpty())
                    .map(ItemStack::copy)
                    .forEach(returnedItems::add);
            returnedItems.add(new ItemStack(drinkBlock.asItem()));
            return Optional.of(new ContraptionRemovalResult(
                    List.of(context.targetPos()), List.of(), returnedItems, true));
        }

        if (block instanceof SignatureCocktailBlock) {
            SignatureCocktailBlockEntity blockEntity = load(
                    new SignatureCocktailBlockEntity(context.targetPos(), state),
                    context.targetInfo().nbt());
            ItemStack cocktail = ModItems.SIGNATURE_COCKTAIL.get().getDefaultInstance();
            SignatureCocktailBlockItem.setEffects(cocktail, blockEntity.getEffects());
            SignatureCocktailBlockItem.setColor(cocktail, blockEntity.getColor());
            return Optional.of(ContraptionRemovalResult.single(context.targetPos(), cocktail));
        }

        if (block instanceof MolotovBlock
                || block instanceof GlasswareBlock) {
            return Optional.of(ContraptionRemovalResult.single(
                    context.targetPos(), new ItemStack(block.asItem())));
        }

        if (block instanceof ShakerBlock) {
            ShakerBlockEntity blockEntity = load(new ShakerBlockEntity(context.targetPos(), state),
                    context.targetInfo().nbt());
            CompoundTag nbt = context.targetInfo().nbt();
            if (nbt != null && nbt.contains("result", Tag.TAG_COMPOUND)) {
                blockEntity.setResult(ItemStack.of(nbt.getCompound("result")));
            }

            ItemStack shaker = ModItems.SHAKER.get().getDefaultInstance();
            ShakerItem.setStorage(shaker, blockEntity.getStorage());
            if (!blockEntity.getResult().isEmpty()) {
                ShakerItem.setResult(shaker, blockEntity.getResult());
            }
            return Optional.of(ContraptionRemovalResult.single(context.targetPos(), shaker));
        }

        if (block instanceof PotionBottleBlock) {
            PotionBottleBlockEntity blockEntity = load(
                    new PotionBottleBlockEntity(context.targetPos(), state),
                    context.targetInfo().nbt());
            List<ItemStack> returnedItems = new ArrayList<>();
            ItemStack potion = blockEntity.getPotionStack().copyWithCount(1);
            if (!potion.isEmpty()) {
                returnedItems.add(potion);
            }

            ItemStack blockItem = new ItemStack(block.asItem());
            if (!blockItem.isEmpty() && (potion.isEmpty() || !blockItem.is(potion.getItem()))) {
                returnedItems.add(blockItem);
            }
            return Optional.of(new ContraptionRemovalResult(
                    List.of(context.targetPos()), List.of(), returnedItems, true));
        }

        if (block instanceof BottleBlock) {
            return Optional.of(ContraptionRemovalResult.single(context.targetPos(),
                    TavernBottleBlockMovingInteraction.getReturnedBottle(block)));
        }

        if (block instanceof IncenseBlock || block instanceof StringLightsBlock) {
            return Optional.of(ContraptionRemovalResult.single(
                    context.targetPos(), new ItemStack(block.asItem())));
        }

        return Optional.empty();
    }

    private PlacementKind getPlacementKind(ItemStack stack) {
        Block block = Block.byItem(stack.getItem());
        PlacementKind directBlockKind = getDirectBlockPlacementKind(block);
        if (directBlockKind != null) {
            return directBlockKind;
        }
        Block bottleBlock = getBottleBlockForPlacement(stack);
        if (bottleBlock instanceof PotionBottleBlock) {
            return PlacementKind.POTION_BOTTLE;
        }
        if (bottleBlock instanceof BottleBlock) {
            return PlacementKind.BOTTLE;
        }
        return null;
    }

    static PlacementKind getDirectBlockPlacementKind(Block block) {
        return getDirectBlockPlacementKind(block.getClass());
    }

    static PlacementKind getDirectBlockPlacementKind(Class<?> blockClass) {
        if (DrinkBlock.class.isAssignableFrom(blockClass)) {
            return PlacementKind.DRINK;
        }
        if (SignatureCocktailBlock.class.isAssignableFrom(blockClass)) {
            return PlacementKind.SIGNATURE_COCKTAIL;
        }
        if (CocktailBlock.class.isAssignableFrom(blockClass)) {
            return PlacementKind.COCKTAIL;
        }
        if (MolotovBlock.class.isAssignableFrom(blockClass)) {
            return PlacementKind.MOLOTOV;
        }
        if (GlasswareBlock.class.isAssignableFrom(blockClass)) {
            return PlacementKind.GLASSWARE;
        }
        if (PotionBottleBlock.class.isAssignableFrom(blockClass)) {
            return PlacementKind.POTION_BOTTLE;
        }
        if (BottleBlock.class.isAssignableFrom(blockClass)) {
            return PlacementKind.BOTTLE;
        }
        if (IncenseBlock.class.isAssignableFrom(blockClass)) {
            return PlacementKind.INCENSE;
        }
        if (StringLightsBlock.class.isAssignableFrom(blockClass)) {
            return PlacementKind.STRING_LIGHTS;
        }
        if (ShakerBlock.class.isAssignableFrom(blockClass)) {
            return PlacementKind.SHAKER;
        }
        return null;
    }

    private Block getBottleBlockForPlacement(ItemStack stack) {
        Block block = Block.byItem(stack.getItem());
        if (block instanceof BottleBlock || block instanceof PotionBottleBlock) {
            return block;
        }
        if (stack.is(Items.POTION)) {
            Potion potion = PotionUtils.getPotion(stack);
            if (potion == Potions.EMPTY || potion == Potions.WATER) {
                return WATER_BOTTLE_PLACEMENT.get() ? ModBlocks.WATER_BOTTLE.get() : null;
            }
            return POTION_BOTTLE_PLACEMENT.get() ? ModBlocks.POTION_BOTTLE.get() : null;
        }
        if (stack.is(Items.HONEY_BOTTLE)) {
            return HONEY_BOTTLE_PLACEMENT.get() ? ModBlocks.HONEY_BOTTLE.get() : null;
        }
        if (stack.is(Items.DRAGON_BREATH)) {
            return DRAGON_BREATH_BOTTLE_PLACEMENT.get()
                    ? ModBlocks.DRAGON_BREATH_BOTTLE.get() : null;
        }
        if (stack.is(Items.EXPERIENCE_BOTTLE)) {
            return EXPERIENCE_BOTTLE_PLACEMENT.get() ? ModBlocks.XP_BOTTLE.get() : null;
        }
        return null;
    }

    private BlockState orient(BlockState state, ContraptionPlacementContext context) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction facing = context.player().getDirection().getOpposite();
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        }
        return state;
    }

    private CompoundTag saveBlockEntity(BlockEntity blockEntity) {
        CompoundTag tag = blockEntity.saveWithFullMetadata();
        tag.remove("x");
        tag.remove("y");
        tag.remove("z");
        return tag;
    }

    private <T extends BlockEntity> T load(T blockEntity, CompoundTag tag) {
        blockEntity.load(tag == null ? new CompoundTag() : tag.copy());
        return blockEntity;
    }

}
