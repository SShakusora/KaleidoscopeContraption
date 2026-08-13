package com.sshakusora.kaleidoscope_contraption.client.render;

import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.mixology.ShakerBlockEntity;
import com.sshakusora.kaleidoscope_contraption.util.KCContraptionRenderHooks;
import com.sshakusora.kaleidoscope_contraption.util.TavernContraptionRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Client-side transient render state handling for Kaleidoscope Tavern. */
public final class KCTavernClientRenderHooks implements KCContraptionRenderHooks.Handler {
    @Override
    public void prepare(BlockEntity blockEntity, StructureTemplate.StructureBlockInfo oldInfo,
                        StructureTemplate.StructureBlockInfo newInfo) {
    }

    @Override
    public void copy(BlockEntity previous, BlockEntity replacement) {
        if (previous instanceof ShakerBlockEntity oldShaker
                && replacement instanceof ShakerBlockEntity newShaker) {
            newShaker.putState = oldShaker.putState;
        }
    }

    @Override
    public void update(BlockEntity blockEntity, StructureTemplate.StructureBlockInfo oldInfo,
                       StructureTemplate.StructureBlockInfo newInfo) {
        if (!(blockEntity instanceof ShakerBlockEntity shaker)
                || newInfo == null || newInfo.nbt() == null) {
            return;
        }

        int oldAnimation = getAnimationId(oldInfo);
        int newAnimation = getAnimationId(newInfo);
        if (newAnimation == 0 || newAnimation == oldAnimation || shaker.getLevel() == null) {
            return;
        }

        shaker.putState.start((int) shaker.getLevel().getGameTime());
    }

    private static int getAnimationId(StructureTemplate.StructureBlockInfo info) {
        if (info == null || info.nbt() == null) {
            return 0;
        }
        return info.nbt().getInt(TavernContraptionRenderState.SHAKER_PUT_ANIMATION);
    }
}

