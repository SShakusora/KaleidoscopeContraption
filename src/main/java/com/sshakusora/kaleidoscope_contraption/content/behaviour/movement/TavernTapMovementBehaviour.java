package com.sshakusora.kaleidoscope_contraption.content.behaviour.movement;

import com.github.ysbbbbbb.kaleidoscopetavern.api.blockentity.IBarrel;
import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.BarrelBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.TapBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.brew.DrinkBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.crafting.recipe.BarrelRecipe;
import com.github.ysbbbbbb.kaleidoscopetavern.crafting.serializer.BarrelRecipeSerializer;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModParticles;
import com.github.ysbbbbbb.kaleidoscopetavern.item.BottleBlockItem;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionWorld;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.TavernBrewingContraptionSupport;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionDataUtil;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class TavernTapMovementBehaviour implements MovementBehaviour {
    public static final String TAP_STATE = "KCTapState";
    public static final String TAP_TICKS = "KCTapTicks";
    public static final String TAP_LAVA_PARTICLE = "KCTapLavaParticle";

    public static final int DEFAULT_STATE = 0;
    public static final int TAKE_DRINK_STATE = 1;
    public static final int EMPTY_OPEN_STATE = 2;
    public static final int TAKE_DRINK_TICKS = 30;
    public static final int TAKE_DRINK_PARTICLE_TICKS = 5;
    public static final int EMPTY_OPEN_TICKS = 5;

    @Override
    public void tick(MovementContext context) {
        if (context.world.isClientSide || context.contraption.entity == null) {
            return;
        }
        AbstractContraptionEntity entity = context.contraption.entity;
        StructureTemplate.StructureBlockInfo info =
                context.contraption.getBlocks().get(context.localPos);
        if (info == null || !(info.state().getBlock() instanceof TapBlock)) {
            return;
        }

        BlockState state = info.state();
        boolean signal = hasSignal(context);
        boolean triggered = state.getValue(TapBlock.TRIGGERED);
        if (signal != triggered) {
            BlockState edgeState = state.setValue(TapBlock.TRIGGERED, signal);
            if (signal && !state.getValue(TapBlock.OPEN)) {
                start(entity, context.localPos, edgeState, info.nbt(), null);
            } else {
                TavernBrewingContraptionSupport.update(entity, context.localPos,
                        edgeState, TavernBrewingContraptionSupport.copyNbt(info));
            }
            return;
        }
        if (!state.getValue(TapBlock.OPEN)) {
            return;
        }

        CompoundTag nbt = TavernBrewingContraptionSupport.copyNbt(info);
        int tapState = nbt.getInt(TAP_STATE);
        int ticks = nbt.getInt(TAP_TICKS) + 1;
        nbt.putInt(TAP_TICKS, ticks);

        if (tapState == TAKE_DRINK_STATE) {
            if (ticks <= TAKE_DRINK_PARTICLE_TICKS) {
                spawnDrip(entity, context.localPos, nbt.getBoolean(TAP_LAVA_PARTICLE));
            }
            if (ticks >= TAKE_DRINK_TICKS) {
                TapTarget target = findTarget(entity, context.localPos, state, null);
                if (target != null) {
                    extract(entity, context.localPos, target);
                }
                close(entity, context.localPos, state, nbt, true);
                return;
            }
        } else if (tapState == EMPTY_OPEN_STATE) {
            if (ticks % 2 == 0) {
                spawnCloud(entity, context.localPos);
            }
            if (ticks >= EMPTY_OPEN_TICKS) {
                close(entity, context.localPos, state, nbt, true);
                return;
            }
        } else {
            close(entity, context.localPos, state, nbt, false);
            return;
        }
        ContraptionDataUtil.updateContraptionData(context, state, nbt, false);
    }

    public static boolean start(AbstractContraptionEntity entity, BlockPos localPos,
                                BlockState state, @Nullable CompoundTag originalNbt,
                                @Nullable Player player) {
        if (entity.level().isClientSide) {
            return true;
        }
        TapTarget target = findTarget(entity, localPos, state, player);
        CompoundTag nbt = originalNbt == null ? new CompoundTag() : originalNbt.copy();
        nbt.putInt(TAP_TICKS, 0);
        nbt.putInt(TAP_STATE, target == null ? EMPTY_OPEN_STATE : TAKE_DRINK_STATE);
        nbt.putBoolean(TAP_LAVA_PARTICLE,
                target != null && target.output().is(ModItems.MOLOTOV.get()));
        TavernBrewingContraptionSupport.update(
                entity, localPos, state.setValue(TapBlock.OPEN, true), nbt);
        ContraptionInteractionUtil.playSound(entity, localPos,
                SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.8F);
        return true;
    }

    public static void close(AbstractContraptionEntity entity, BlockPos localPos,
                             BlockState state, @Nullable CompoundTag originalNbt,
                             boolean playSound) {
        if (entity.level().isClientSide) {
            return;
        }
        CompoundTag nbt = originalNbt == null ? new CompoundTag() : originalNbt.copy();
        nbt.putInt(TAP_STATE, DEFAULT_STATE);
        nbt.putInt(TAP_TICKS, 0);
        nbt.remove(TAP_LAVA_PARTICLE);
        TavernBrewingContraptionSupport.update(
                entity, localPos, state.setValue(TapBlock.OPEN, false), nbt);
        if (playSound) {
            ContraptionInteractionUtil.playSound(entity, localPos,
                    SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
    }

    private boolean hasSignal(MovementContext context) {
        ContraptionWorld localWorld = new ContraptionWorld(context.world, context.contraption);
        boolean localSignal = localWorld.hasNeighborSignal(context.localPos)
                || localWorld.hasNeighborSignal(context.localPos.above());
        if (localSignal || context.contraption.entity == null) {
            return localSignal;
        }
        Vec3 global = TavernBrewingContraptionSupport.globalCenter(
                context.contraption.entity, context.localPos);
        BlockPos worldPos = BlockPos.containing(global);
        return context.world.hasNeighborSignal(worldPos)
                || context.world.hasNeighborSignal(worldPos.above());
    }

    @Nullable
    private static TapTarget findTarget(AbstractContraptionEntity entity, BlockPos tapPos,
                                        BlockState tapState, @Nullable Player player) {
        Direction tapFacing = tapState.getValue(TapBlock.FACING);
        BlockPos sourcePos = tapPos.relative(tapFacing.getOpposite());
        StructureTemplate.StructureBlockInfo sourceInfo =
                entity.getContraption().getBlocks().get(sourcePos);
        if (sourceInfo == null || !(sourceInfo.state().getBlock() instanceof BarrelBlock)
                || !isValidConnection(sourceInfo.state(), tapFacing)) {
            return null;
        }

        TavernBrewingContraptionSupport.BarrelData barrel =
                TavernBrewingContraptionSupport.findBarrel(
                        entity.getContraption(), sourcePos, sourceInfo.state());
        if (barrel == null || !TavernBrewingContraptionSupport.isBrewing(barrel.nbt())) {
            tip(player, "tap_extract_not_brewing");
            return null;
        }
        ItemStack output = TavernBrewingContraptionSupport.getBarrelOutput(barrel.nbt());
        if (output.isEmpty()) {
            tip(player, "tap_extract_empty");
            return null;
        }

        RecipeOutput recipeOutput = getRecipeOutput(entity, barrel.nbt());
        if (recipeOutput == null) {
            tip(player, "tap_extract_invalid_container");
            return null;
        }

        BlockPos belowPos = tapPos.below();
        StructureTemplate.StructureBlockInfo belowInfo =
                entity.getContraption().getBlocks().get(belowPos);
        if (belowInfo != null) {
            ItemStack placed = belowInfo.state().getBlock().asItem().getDefaultInstance();
            if (!placed.isEmpty() && recipeOutput.carrier().test(placed)) {
                return new TapTarget(barrel, output, recipeOutput.result(),
                        belowPos, belowInfo, null);
            }
        }

        ItemEntity carrierEntity = findCarrierEntity(
                entity, belowPos, recipeOutput.carrier());
        if (carrierEntity != null) {
            return new TapTarget(barrel, output, recipeOutput.result(),
                    belowPos, belowInfo, carrierEntity);
        }
        tip(player, belowInfo == null
                ? "tap_extract_empty_container" : "tap_extract_invalid_container");
        return null;
    }

    @Nullable
    private static RecipeOutput getRecipeOutput(AbstractContraptionEntity entity,
                                                CompoundTag barrelNbt) {
        ResourceLocation recipeId = ResourceLocation.tryParse(
                barrelNbt.getString(TavernBrewingContraptionSupport.RECIPE_ID));
        if (recipeId == null || recipeId.equals(BarrelRecipeSerializer.EMPTY_RECIPE_ID)) {
            return new RecipeOutput(
                    Ingredient.of(ModItems.EMPTY_BOTTLE.get()),
                    ModItems.VINEGAR.get().getDefaultInstance());
        }
        return entity.level().getRecipeManager().byKey(recipeId)
                .filter(BarrelRecipe.class::isInstance)
                .map(BarrelRecipe.class::cast)
                .map(recipe -> new RecipeOutput(recipe.carrier(), recipe.result()))
                .orElse(null);
    }

    private static boolean isValidConnection(BlockState barrelState, Direction tapFacing) {
        if (barrelState.getValue(BarrelBlock.LAYER) != AttachFace.WALL
                || barrelState.getValue(BarrelBlock.FACING) != tapFacing) {
            return false;
        }
        int index = barrelState.getValue(BarrelBlock.INDEX);
        return switch (tapFacing) {
            case NORTH -> index == 1;
            case SOUTH -> index == 7;
            case WEST -> index == 3;
            case EAST -> index == 5;
            default -> false;
        };
    }

    @Nullable
    private static ItemEntity findCarrierEntity(AbstractContraptionEntity entity,
                                                BlockPos belowLocalPos,
                                                Ingredient carrier) {
        Vec3 center = TavernBrewingContraptionSupport.globalCenter(entity, belowLocalPos);
        AABB box = new AABB(center.x - 0.5, center.y - 0.5, center.z - 0.5,
                center.x + 0.5, center.y + 0.5, center.z + 0.5);
        List<ItemEntity> entities = entity.level().getEntitiesOfClass(
                ItemEntity.class, box, item -> carrier.test(item.getItem()));
        return entities.isEmpty() ? null : entities.get(0);
    }

    private static void extract(AbstractContraptionEntity entity, BlockPos tapPos,
                                TapTarget target) {
        CompoundTag barrelNbt = target.barrel().nbt().copy();
        ItemStackHandler output = TavernBrewingContraptionSupport.readItems(
                barrelNbt, TavernBrewingContraptionSupport.OUTPUT, 1, 64);
        ItemStack counter = output.extractItem(0, 1, false);
        if (counter.isEmpty()) {
            return;
        }

        int brewLevel = BottleBlockItem.clampBrewLevel(
                barrelNbt.getInt(TavernBrewingContraptionSupport.BREW_LEVEL));
        ItemStack result = target.result().copyWithCount(1);
        if (result.getItem() instanceof BottleBlockItem bottle) {
            result = bottle.getFilledStack(brewLevel);
        }
        transformCarrier(entity, target, result);

        TavernBrewingContraptionSupport.writeItems(
                barrelNbt, TavernBrewingContraptionSupport.OUTPUT, output);
        if (output.getStackInSlot(0).isEmpty()) {
            barrelNbt.remove(TavernBrewingContraptionSupport.RECIPE_ID);
            barrelNbt.putInt(TavernBrewingContraptionSupport.BREW_LEVEL,
                    IBarrel.BREWING_NOT_STARTED);
            barrelNbt.putInt(TavernBrewingContraptionSupport.BREW_TIME, -1);
        }
        TavernBrewingContraptionSupport.update(entity, target.barrel().origin(),
                target.barrel().info().state(), barrelNbt);

        ContraptionInteractionUtil.playSound(entity, tapPos.below(),
                SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (entity.level() instanceof ServerLevel level) {
            Vec3 pos = TavernBrewingContraptionSupport.globalCenter(entity, tapPos.below());
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.x, pos.y, pos.z, 6, 0.2, 0.2, 0.2, 0.02);
        }
    }

    private static void transformCarrier(AbstractContraptionEntity entity,
                                         TapTarget target, ItemStack result) {
        if (target.itemEntity() != null) {
            ItemStack carrier = target.itemEntity().getItem();
            carrier.shrink(1);
            if (carrier.isEmpty()) {
                target.itemEntity().discard();
            } else {
                target.itemEntity().setItem(carrier);
            }
        }

        if (result.getItem() instanceof BlockItem blockItem
                && (target.belowInfo() == null || target.itemEntity() == null)) {
            placeResultBlock(entity, target.belowPos(), target.belowInfo(), blockItem, result);
            return;
        }
        dropResult(entity, target.belowPos(), result);
        if (target.belowInfo() != null && target.itemEntity() == null) {
            removeLocalCarrier(entity, target.belowPos());
        }
    }

    private static void placeResultBlock(AbstractContraptionEntity entity, BlockPos localPos,
                                         @Nullable StructureTemplate.StructureBlockInfo previous,
                                         BlockItem blockItem, ItemStack result) {
        BlockState state = blockItem.getBlock().defaultBlockState();
        if (previous != null && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                && previous.state().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING,
                    previous.state().getValue(BlockStateProperties.HORIZONTAL_FACING));
        }

        CompoundTag nbt = null;
        if (blockItem instanceof BottleBlockItem) {
            DrinkBlockEntity drink = new DrinkBlockEntity(localPos, state);
            drink.addItem(result);
            nbt = drink.saveWithFullMetadata();
            nbt.remove("x");
            nbt.remove("y");
            nbt.remove("z");
        }
        StructureTemplate.StructureBlockInfo resultInfo =
                new StructureTemplate.StructureBlockInfo(localPos, state, nbt);
        if (previous == null) {
            ContraptionInteractionUtil.updateContraptionDataLocally(entity, localPos, resultInfo);
            var bounds = ContraptionInteractionUtil.recalculateBounds(entity);
            ContraptionInteractionUtil.updateContraptionDataWithBound(
                    entity, localPos, resultInfo, bounds);
            entity.getContraption().invalidateColliders();
        } else {
            ContraptionInteractionUtil.updateContraptionData(entity, localPos, resultInfo);
        }
    }

    private static void removeLocalCarrier(AbstractContraptionEntity entity, BlockPos localPos) {
        ContraptionInteractionUtil.removeBlockFromContraption(entity, localPos);
        var bounds = ContraptionInteractionUtil.recalculateBounds(entity);
        entity.getContraption().invalidateColliders();
        ContraptionInteractionUtil.syncBlockRemoval(entity, localPos, bounds);
    }

    private static void dropResult(AbstractContraptionEntity entity, BlockPos localPos,
                                   ItemStack result) {
        Vec3 pos = TavernBrewingContraptionSupport.globalCenter(entity, localPos);
        ItemEntity item = new ItemEntity(entity.level(), pos.x, pos.y, pos.z, result);
        item.setDefaultPickUpDelay();
        entity.level().addFreshEntity(item);
    }

    private static void spawnDrip(AbstractContraptionEntity entity, BlockPos localPos,
                                  boolean lava) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pos = TavernBrewingContraptionSupport.globalCenter(entity, localPos);
        ParticleOptions particle = lava
                ? ModParticles.LAVA_TAP_DRIP.get() : ModParticles.WATER_TAP_DRIP.get();
        level.sendParticles(particle, pos.x, pos.y - 0.25, pos.z,
                1, 0, 0, 0, 0);
    }

    private static void spawnCloud(AbstractContraptionEntity entity, BlockPos localPos) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pos = TavernBrewingContraptionSupport.globalCenter(entity, localPos);
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y - 0.25, pos.z,
                1, 0.1, 0.1, 0.1, 0.01);
    }

    private static void tip(@Nullable Player player, String key) {
        if (player != null) {
            player.displayClientMessage(Component.translatable(
                    "message.kaleidoscope_tavern.barrel.%s".formatted(key)), true);
        }
    }

    private record RecipeOutput(Ingredient carrier, ItemStack result) {
    }

    private record TapTarget(TavernBrewingContraptionSupport.BarrelData barrel,
                             ItemStack output,
                             ItemStack result,
                             BlockPos belowPos,
                             @Nullable StructureTemplate.StructureBlockInfo belowInfo,
                             @Nullable ItemEntity itemEntity) {
    }
}
