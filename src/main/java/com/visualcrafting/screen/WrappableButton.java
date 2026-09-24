package com.visualcrafting.screen;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class WrappableButton extends Button {
    public WrappableButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    public void renderString(GuiGraphics graphics, Font font, int color) {
        Component message = this.getMessage();
        int maxTextWidth = this.getWidth() - 6;
        if (font.width(message) <= maxTextWidth) {
            graphics.drawCenteredString(font, message, this.getX() + this.getWidth() / 2,
                    this.getY() + (this.getHeight() - 8) / 2, color);
            return;
        }
        List<FormattedCharSequence> lines = font.split(message, maxTextWidth);
        int lineHeight = 9;
        int totalTextHeight = lines.size() * lineHeight;
        int startY = this.getY() + (this.getHeight() - totalTextHeight) / 2;
        for (int i = 0; i < lines.size(); ++i) {
            graphics.drawCenteredString(font, lines.get(i), this.getX() + this.getWidth() / 2,
                    startY + i * lineHeight, color);
        }
    }
}
