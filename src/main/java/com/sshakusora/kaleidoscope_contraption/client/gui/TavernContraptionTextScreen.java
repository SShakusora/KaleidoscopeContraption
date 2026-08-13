package com.sshakusora.kaleidoscope_contraption.client.gui;

import com.github.ysbbbbbb.kaleidoscopetavern.util.TextAlignment;
import com.sshakusora.kaleidoscope_contraption.network.KCTavernPacketHandler;
import com.sshakusora.kaleidoscope_contraption.network.KCTavernTextUpdatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Text editor counterpart for boards whose data is stored in a moving contraption. */
public class TavernContraptionTextScreen extends Screen {
    private final int entityId;
    private final BlockPos localPos;
    private final int maxTextLength;
    private String text;
    private TextAlignment textAlignment;
    private MultiLineEditBox textBox;

    public TavernContraptionTextScreen(int entityId, BlockPos localPos, String text,
                                       int maxTextLength, TextAlignment textAlignment) {
        super(Component.literal("Contraption Text"));
        this.entityId = entityId;
        this.localPos = localPos;
        this.text = text;
        this.maxTextLength = maxTextLength;
        this.textAlignment = textAlignment;
    }

    @Override
    protected void init() {
        clearWidgets();
        int posX = width / 2 - 165;
        int posY = height / 2 - 80;
        int boxWidth = 256;
        textBox = addRenderableWidget(new MultiLineEditBox(font, posX, posY, boxWidth, 120,
                Component.translatable("gui.kaleidoscope_tavern.text.edit.placeholder"),
                Component.literal("Text")));
        textBox.setValue(text);
        textBox.setCharacterLimit(maxTextLength);
        textBox.setValueListener(value -> text = value);

        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(posX, posY + 135, 126, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onDone())
                .bounds(posX + 130, posY + 135, 126, 20).build());

        Button left = Button.builder(Component.literal("Left"), button -> setAlignment(TextAlignment.LEFT))
                .bounds(posX + boxWidth + 5, posY + 25, 80, 20).build();
        Button center = Button.builder(Component.literal("Center"), button -> setAlignment(TextAlignment.CENTER))
                .bounds(posX + boxWidth + 5, posY + 50, 80, 20).build();
        Button right = Button.builder(Component.literal("Right"), button -> setAlignment(TextAlignment.RIGHT))
                .bounds(posX + boxWidth + 5, posY + 75, 80, 20).build();
        if (textAlignment == TextAlignment.LEFT) {
            left.active = false;
        } else if (textAlignment == TextAlignment.CENTER) {
            center.active = false;
        } else {
            right.active = false;
        }
        addRenderableWidget(left);
        addRenderableWidget(center);
        addRenderableWidget(right);
    }

    private void setAlignment(TextAlignment alignment) {
        textAlignment = alignment;
        init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        if (textBox != null) {
            // Child widgets are ticked by Screen's managed widget list.
        }
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String currentText = textBox == null ? text : textBox.getValue();
        super.resize(minecraft, width, height);
        if (textBox != null) {
            textBox.setValue(currentText);
        }
    }

    private void onDone() {
        KCTavernPacketHandler.sendToServer(new KCTavernTextUpdatePacket(
                entityId, localPos, text, textAlignment));
        onClose();
    }
}
