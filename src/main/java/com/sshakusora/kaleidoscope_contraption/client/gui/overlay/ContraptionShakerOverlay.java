package com.sshakusora.kaleidoscope_contraption.client.gui.overlay;

import com.github.ysbbbbbb.kaleidoscopetavern.KaleidoscopeTavern;
import com.github.ysbbbbbb.kaleidoscopetavern.block.mixology.ShakerBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.mixology.ShakerBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopetavern.util.ColorUtils;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.items.ItemStackHandler;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Optional;

/** Renders Kaleidoscope Tavern's shaker contents for a Create Contraption target. */
public final class ContraptionShakerOverlay implements IGuiOverlay {
    private static final int CONTENT_X_OFFSET = 28;
    private static final int CONTENT_Y_OFFSET = 26;
    private static final int SLOT_STEP = 20;
    private static final int ICON_Y_OFFSET = 6;
    private static final ResourceLocation ICON_LOCATION =
            ResourceLocation.fromNamespaceAndPath(KaleidoscopeTavern.MOD_ID, "gui/rhombus");

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick,
                       int screenWidth, int screenHeight) {
        Minecraft minecraft = gui.getMinecraft();
        if (minecraft.gameMode == null || minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return;
        }

        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        Optional<ShakerTarget> target = findTargetedShaker(minecraft, player);
        if (target.isEmpty()) {
            return;
        }

        renderContents(guiGraphics, minecraft.font, target.get().shaker(),
                screenWidth / 2 - CONTENT_X_OFFSET,
                screenHeight / 2 + CONTENT_Y_OFFSET);
    }

    private Optional<ShakerTarget> findTargetedShaker(Minecraft minecraft, LocalPlayer player) {
        if (minecraft.level == null) {
            return Optional.empty();
        }

        var rayInputs = ContraptionHandlerClient.getRayInputs(player);
        Vec3 eyePos = rayInputs.getFirst();
        Vec3 endPos = rayInputs.getSecond();
        AABB searchBounds = new AABB(eyePos, endPos).inflate(16);
        Collection<WeakReference<AbstractContraptionEntity>> contraptions =
                ContraptionHandler.loadedContraptions.get(minecraft.level).values();

        ShakerTarget closestTarget = null;
        double closestDistance = Double.MAX_VALUE;
        for (WeakReference<AbstractContraptionEntity> reference : contraptions) {
            AbstractContraptionEntity contraptionEntity = reference.get();
            if (contraptionEntity == null || !contraptionEntity.getBoundingBox().intersects(searchBounds)) {
                continue;
            }

            BlockHitResult hit = ContraptionHandlerClient.rayTraceContraption(
                    eyePos, endPos, contraptionEntity);
            if (hit == null || contraptionEntity.getContraption() == null) {
                continue;
            }

            BlockPos localPos = hit.getBlockPos();
            StructureTemplate.StructureBlockInfo info =
                    contraptionEntity.getContraption().getBlocks().get(localPos);
            if (info == null || !(info.state().getBlock() instanceof ShakerBlock)
                    || !info.state().is(ModBlocks.SHAKER.get())) {
                continue;
            }

            BlockEntity blockEntity = contraptionEntity.getContraption().getBlockEntityClientSide(localPos);
            if (!(blockEntity instanceof ShakerBlockEntity shaker)) {
                continue;
            }

            double distance = contraptionEntity.toGlobalVector(hit.getLocation(), 1)
                    .distanceTo(eyePos);
            if (distance > closestDistance) {
                continue;
            }

            closestDistance = distance;
            closestTarget = new ShakerTarget(shaker);
        }

        return Optional.ofNullable(closestTarget);
    }

    private static void renderContents(GuiGraphics guiGraphics, Font font,
                                       ShakerBlockEntity shaker, int x, int y) {
        ItemStackHandler storage = shaker.getStorage();
        for (int i = 0; i < storage.getSlots(); i++) {
            ItemStack stack = storage.getStackInSlot(i);
            if (!stack.isEmpty()) {
                ChatFormatting formatting = ColorUtils.ITEM_COLOR_CACHE.apply(stack.getItem());
                if (formatting == ChatFormatting.RESET) {
                    guiGraphics.renderFakeItem(stack, x, y);
                    guiGraphics.renderItemDecorations(font, stack, x, y);
                } else if (formatting.getColor() != null) {
                    renderIcon(guiGraphics, x, y + ICON_Y_OFFSET,
                            formatting.getColor() | 0xFF000000);
                }
            }
            x += SLOT_STEP;
        }
    }

    @SuppressWarnings("deprecation")
    private static void renderIcon(GuiGraphics guiGraphics, int x, int y, int color) {
        float alpha = FastColor.ARGB32.alpha(color) / 255f;
        float red = FastColor.ARGB32.red(color) / 255f;
        float green = FastColor.ARGB32.green(color) / 255f;
        float blue = FastColor.ARGB32.blue(color) / 255f;
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(ICON_LOCATION);
        guiGraphics.blit(x, y, 0, 16, 16, sprite, red, green, blue, alpha);
    }

    private record ShakerTarget(ShakerBlockEntity shaker) {
    }
}
