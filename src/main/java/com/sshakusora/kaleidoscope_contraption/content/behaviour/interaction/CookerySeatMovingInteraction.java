package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.ChairBlock;
import com.sshakusora.kaleidoscope_contraption.api.seat.ContraptionSeatBackends;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Handles Cookery-specific drops on top of the unified seat interaction. */
public class CookerySeatMovingInteraction extends ContraptionSeatMovingInteraction {
    public CookerySeatMovingInteraction() {
        super(ContraptionSeatBackends.COOKERY);
    }

    @Override
    protected void giveAdditionalDrops(Player player, StructureTemplate.StructureBlockInfo info) {
        if (!(info.state().getBlock() instanceof ChairBlock)
                || !info.state().getValue(ChairBlock.HAS_CARPET)
                || info.nbt() == null) {
            return;
        }
        DyeColor color = DyeColor.byId(info.nbt().getInt("CarpetColor"));
        ItemStack carpet = com.github.ysbbbbbb.kaleidoscopecookery.util.CarpetColor
                .getCarpetByColor(color).getDefaultInstance();
        ItemHandlerHelper.giveItemToPlayer(player, carpet);
    }
}
