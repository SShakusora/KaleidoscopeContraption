package com.sshakusora.kaleidoscope_contraption.util;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Optional hooks for preserving transient client-side block entity render state. */
public final class KCContraptionRenderHooks {
    private static final Handler NO_OP = new Handler() {
        @Override
        public void prepare(BlockEntity blockEntity, StructureTemplate.StructureBlockInfo oldInfo,
                            StructureTemplate.StructureBlockInfo newInfo) {
        }

        @Override
        public void copy(BlockEntity previous, BlockEntity replacement) {
        }
    };

    private static volatile Handler handler = NO_OP;

    private KCContraptionRenderHooks() {
    }

    public static void register(Handler newHandler) {
        handler = newHandler == null ? NO_OP : newHandler;
    }

    public static void prepare(BlockEntity blockEntity, StructureTemplate.StructureBlockInfo oldInfo,
                               StructureTemplate.StructureBlockInfo newInfo) {
        handler.prepare(blockEntity, oldInfo, newInfo);
    }

    public static void copy(BlockEntity previous, BlockEntity replacement) {
        handler.copy(previous, replacement);
    }

    public interface Handler {
        void prepare(BlockEntity blockEntity, StructureTemplate.StructureBlockInfo oldInfo,
                     StructureTemplate.StructureBlockInfo newInfo);

        void copy(BlockEntity previous, BlockEntity replacement);
    }
}
