package com.sshakusora.kaleidoscope_contraption.content.behaviour.placement;

import com.github.ysbbbbbb.kaleidoscopecookery.api.blockentity.ITeapot;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.TeapotBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.serializer.TeapotRecipeSerializer;
import com.sshakusora.kaleidoscope_contraption.api.placement.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;
import java.util.Optional;

public class StoveTeapotPlacementRule implements ContraptionPlacementRule {
    @Override
    public boolean matches(ContraptionPlacementContext context) {
        return Block.byItem(context.heldItem().getItem()) instanceof TeapotBlock;
    }

    @Override
    public ContraptionPlacementResult createPlacement(ContraptionPlacementContext context) {
        TeapotBlock block = (TeapotBlock) Block.byItem(context.heldItem().getItem());
        BlockState state = block.defaultBlockState()
                .setValue(TeapotBlock.FACING, context.player().getDirection().getOpposite())
                .setValue(TeapotBlock.WATERLOGGED, false)
                .setValue(TeapotBlock.VARIANT, TeapotBlock.COMMON);
        return single(context, state, createTeapotNbt(context.heldItem(), context.contraptionEntity().level().registryAccess()));
    }

    @Override
    public Optional<ContraptionRemovalResult> createRemoval(ContraptionRemovalContext context) {
        StructureTemplate.StructureBlockInfo info = context.targetInfo();
        if (!(info.state().getBlock() instanceof TeapotBlock)) {
            return Optional.empty();
        }
        CompoundTag nbt = info.nbt() == null ? createEmptyTeapotNbt(context.contraptionEntity().level().registryAccess()) : info.nbt();
        TeapotBlockEntity blockEntity = new TeapotBlockEntity(info.pos(), info.state());
        blockEntity.setLevel(context.contraptionEntity().level());
        blockEntity.loadWithComponents(nbt, context.contraptionEntity().level().registryAccess());
        return Optional.of(new ContraptionRemovalResult(
                List.of(context.targetPos()), List.of(), blockEntity.getDrops(), true));
    }

    private ContraptionPlacementResult single(ContraptionPlacementContext context, BlockState state,
                                              CompoundTag nbt) {
        return ContraptionPlacementResult.single(new StructureTemplate.StructureBlockInfo(
                context.targetPos(), state, nbt));
    }

    private CompoundTag createTeapotNbt(ItemStack heldItem, HolderLookup.Provider registries) {
        CompoundTag nbt = createEmptyTeapotNbt(registries);
        var customData = heldItem.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag handData = customData == null ? null : customData.copyTag();
        if (handData != null) {
            nbt.merge(handData.copy());
        }
        nbt.putString("id", "kaleidoscope_cookery:teapot");
        return nbt;
    }

    private CompoundTag createEmptyTeapotNbt(HolderLookup.Provider registries) {
        CompoundTag nbt = new CompoundTag();
        nbt.put("Input", ItemStack.EMPTY.saveOptional(registries));
        nbt.putString("TeaFluidId", TeapotRecipeSerializer.EMPTY_TEA_FLUID.toString());
        nbt.put("Result", ItemStack.EMPTY.saveOptional(registries));
        nbt.putInt("Status", ITeapot.PUT_INGREDIENT);
        nbt.putInt("CurrentTick", -1);
        nbt.putString("id", "kaleidoscope_cookery:teapot");
        return nbt;
    }
}
