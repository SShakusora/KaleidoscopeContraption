package com.sshakusora.kaleidoscope_contraption.client.render;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.ChoppingBoardBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.PotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.misc.TrashCanBlockEntity;
import com.sshakusora.kaleidoscope_contraption.util.KCContraptionRenderHooks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Objects;

/** Cookery-specific transient render state handling, installed only on the client with Cookery. */
public final class KCCookeryClientRenderHooks implements KCContraptionRenderHooks.Handler {
    @Override
    public void copy(BlockEntity previous, BlockEntity replacement) {
        if (previous instanceof PotBlockEntity oldPot && replacement instanceof PotBlockEntity newPot) {
            newPot.animationData = oldPot.animationData;
        } else if (previous instanceof TeapotBlockEntity oldTeapot
                && replacement instanceof TeapotBlockEntity newTeapot) {
            newTeapot.boilingState = oldTeapot.boilingState;
        } else if (previous instanceof TrashCanBlockEntity oldTrashCan
                && replacement instanceof TrashCanBlockEntity newTrashCan) {
            newTrashCan.putState = oldTrashCan.putState;
            newTrashCan.withdrawState = oldTrashCan.withdrawState;
            newTrashCan.player1State = oldTrashCan.player1State;
            newTrashCan.player2State = oldTrashCan.player2State;
            newTrashCan.enterState = oldTrashCan.enterState;
        } else if (previous instanceof StockpotBlockEntity oldStockpot
                && replacement instanceof StockpotBlockEntity newStockpot
                && newStockpot.getStatus() != 0
                && Objects.equals(oldStockpot.getSoupBaseId(), newStockpot.getSoupBaseId())) {
            newStockpot.renderEntity = oldStockpot.renderEntity;
        } else if (previous instanceof ChoppingBoardBlockEntity oldBoard
                && replacement instanceof ChoppingBoardBlockEntity newBoard
                && Objects.equals(oldBoard.getModelId(), newBoard.getModelId())
                && oldBoard.getMaxCutCount() == newBoard.getMaxCutCount()) {
            newBoard.previousModel = oldBoard.previousModel;
            newBoard.cacheModels = oldBoard.cacheModels;
        }
    }

    @Override
    public void prepare(BlockEntity blockEntity, StructureTemplate.StructureBlockInfo oldInfo,
                        StructureTemplate.StructureBlockInfo newInfo) {
        if (blockEntity instanceof ChoppingBoardBlockEntity choppingBoard) {
            String oldModelId = oldInfo == null || oldInfo.nbt() == null
                    ? "" : oldInfo.nbt().getString("ModelId");
            String newModelId = newInfo.nbt().getString("ModelId");
            int oldMaxCutCount = oldInfo == null || oldInfo.nbt() == null
                    ? 0 : oldInfo.nbt().getInt("MaxCutCount");
            if (!oldModelId.equals(newModelId)
                    || oldMaxCutCount != newInfo.nbt().getInt("MaxCutCount")) {
                choppingBoard.previousModel = null;
                choppingBoard.cacheModels = null;
            }
            return;
        }

        if (!(blockEntity instanceof StockpotBlockEntity stockpot)) {
            return;
        }

        CompoundTag newNbt = Objects.requireNonNull(newInfo.nbt());
        stockpot.visuals = null;
        if (!newNbt.contains("LidItem", Tag.TAG_COMPOUND)) {
            stockpot.setLidItem(ItemStack.EMPTY);
        }

        String oldSoupBase = oldInfo == null || oldInfo.nbt() == null
                ? "" : oldInfo.nbt().getString("SoupBaseId");
        String newSoupBase = newNbt.getString("SoupBaseId");
        if (newNbt.getInt("Status") == 0 || !oldSoupBase.equals(newSoupBase)) {
            stockpot.renderEntity = null;
        }
    }
}
