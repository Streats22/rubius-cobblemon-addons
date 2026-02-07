package nl.streats1.rubiusaddons.client.screen.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Button that renders nothing (invisible hit area). Used for ± quantity so our arrow textures show.
 */
public class InvisibleButton extends Button {

    public InvisibleButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Draw nothing; screen draws arrow textures behind
    }
}
