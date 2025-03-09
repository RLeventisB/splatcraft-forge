package net.splatcraft.client.gui.stagepad;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;

public class MenuTextBox extends MultiLineEditBox
{
    int relativeX;
    int relativeY;

    public MenuTextBox(Font textRenderer, int x, int y, int width, int height, Component unfocusedText, boolean bordered)
    {
        super(textRenderer, x, y, width, height, Component.empty(), unfocusedText);
        relativeX = x;
        relativeY = y;
    }

    public interface Factory
    {
        MenuTextBox newInstance(Font textRenderer);
    }

    public interface Setter
    {
        void apply();
    }
}
