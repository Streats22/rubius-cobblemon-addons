package nl.streats1.rubiusaddons.client.screen.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Button that only renders its label (no background). Screen draws the button texture behind it.
 */
public class TextureOnlyButton extends Button {

    public TextureOnlyButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!getMessage().getString().isEmpty()) {
            int color = active ? (isHovered ? 0xFFF0F0F0 : 0xFFE0E0E0) : 0xFFA0A0A0;
            guiGraphics.drawCenteredString(
                net.minecraft.client.Minecraft.getInstance().font,
                getMessage(),
                getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2,
                color
            );
        }
    }
}
