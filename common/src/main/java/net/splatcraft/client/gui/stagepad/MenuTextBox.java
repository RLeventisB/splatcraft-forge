package net.splatcraft.client.gui.stagepad;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.text.Text;

public class MenuTextBox extends EditBoxWidget
{
    int relativeX;
    int relativeY;

    public MenuTextBox(TextRenderer textRenderer, int x, int y, int width, int height, Text unfocusedText, boolean bordered)
    {
        super(textRenderer, x, y, width, height, Text.empty(), unfocusedText);
        relativeX = x;
        relativeY = y;
    }

    public interface Factory
    {
        MenuTextBox newInstance(TextRenderer textRenderer);
    }

    public interface Setter
    {
        void apply();
    }
}
