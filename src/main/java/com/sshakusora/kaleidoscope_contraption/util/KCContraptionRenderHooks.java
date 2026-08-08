package com.sshakusora.kaleidoscope_contraption.util;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.concurrent.CopyOnWriteArrayList;

/** Optional hooks for preserving transient client-side block entity render state. */
public final class KCContraptionRenderHooks {
    private static final CopyOnWriteArrayList<Handler> handlers = new CopyOnWriteArrayList<>();

    private KCContraptionRenderHooks() {
    }

    public static void register(Handler newHandler) {
        if (newHandler != null) {
            handlers.addIfAbsent(newHandler);
        }
    }

    public static void prepare(BlockEntity blockEntity, StructureTemplate.StructureBlockInfo oldInfo,
                               StructureTemplate.StructureBlockInfo newInfo) {
        for (Handler handler : handlers) {
            handler.prepare(blockEntity, oldInfo, newInfo);
        }
    }

    public static void copy(BlockEntity previous, BlockEntity replacement) {
        for (Handler handler : handlers) {
            handler.copy(previous, replacement);
        }
    }

    public static void update(BlockEntity blockEntity, StructureTemplate.StructureBlockInfo oldInfo,
                              StructureTemplate.StructureBlockInfo newInfo) {
        for (Handler handler : handlers) {
            handler.update(blockEntity, oldInfo, newInfo);
        }
    }

    public interface Handler {
        void prepare(BlockEntity blockEntity, StructureTemplate.StructureBlockInfo oldInfo,
                     StructureTemplate.StructureBlockInfo newInfo);

        void copy(BlockEntity previous, BlockEntity replacement);

        default void update(BlockEntity blockEntity, StructureTemplate.StructureBlockInfo oldInfo,
                            StructureTemplate.StructureBlockInfo newInfo) {
        }
    }
}
