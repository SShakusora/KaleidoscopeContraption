package com.sshakusora.kaleidoscope_contraption.mixin.accessor;

import com.simibubi.create.content.contraptions.render.ClientContraption;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ClientContraption.class)
public interface ClientContraptionAccessor {
    @Accessor("renderedBlockEntities")
    List<BlockEntity> getRenderedBlockEntities();

    @Accessor("structureVersion")
    void setStructureVersion(int structureVersion);

    @Accessor("childrenVersion")
    void setChildrenVersion(int childrenVersion);
}
