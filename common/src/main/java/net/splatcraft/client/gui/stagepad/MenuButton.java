package net.splatcraft.client.gui.stagepad;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static net.splatcraft.client.gui.stagepad.AbstractStagePadScreen.WIDGETS;

public class MenuButton extends ButtonWidget
{
    public static final OnTooltip NO_TOOLTIP = (h, e, l, o, partialTicks) ->
    {
    };
    public static PressAction DO_NOTHING = (v) ->
    {
    };
    final PostDraw draw;
    final OnTooltip onTooltip;
    ButtonColor color;
    int relativeX;
    int relativeY;

    public MenuButton(int x, int y, int width, PressAction onPress, OnTooltip onTooltip, PostDraw draw, ButtonColor color)
    {
        this(x, y, width, 12, onPress, onTooltip, draw, color);
    }

    public MenuButton(int x, int y, int width, int height, PressAction onPress, OnTooltip onTooltip, PostDraw draw, ButtonColor color)
    {
        super(x, y, width, height, Text.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);
        this.onTooltip = onTooltip;
        this.color = color;
        this.draw = draw;
        relativeX = x;
        relativeY = y;
    }

    @Override
    public void renderWidget(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        if (!visible)
            return;

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, WIDGETS);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        int i = getYImage(isSelected());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        guiGraphics.drawTexture(WIDGETS, getX(), getY(), 0, getColor().ordinal() * 36 + i * 12, width / 2, height);
        guiGraphics.drawTexture(WIDGETS, getX() + width / 2, getY(), 180 - width / 2, getColor().ordinal() * 36 + i * 12, width / 2, height);
        draw.apply(guiGraphics, this);

        if (isSelected())
            drawTooltip(guiGraphics, mouseX, mouseY, partialTicks);
    }

    public void drawTooltip(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        onTooltip.onTooltip(this, guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void appendDefaultNarrations(NarrationMessageBuilder builder)
    {
        super.appendDefaultNarrations(builder);
        onTooltip.narrateTooltip(v -> builder.put(NarrationPart.HINT, v));
    }

    protected int getYImage(boolean hovered)
    {
        return active ? (hovered ? 2 : 1) : 0;
    }

    public float getAlpha()
    {
        return alpha;
    }

    public int getFGColor()
    {
        return 0xFFFFFF;
    }

    public void setHovered(boolean hovered)
    {
        this.hovered = hovered;
    }

    public ButtonColor getColor()
    {
        return color;
    }

    public void setColor(ButtonColor color)
    {
        this.color = color;
    }

    public enum ButtonColor
    {
        GREEN,
        PURPLE,
        LIME,
        CYAN,
        RED,
        YELLOW
    }

    public interface PostDraw
    {
        void apply(DrawContext guiGraphics, MenuButton button);
    }

    @Environment(EnvType.CLIENT)
    public interface OnTooltip
    {
        void onTooltip(MenuButton button, DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks);

        default void narrateTooltip(Consumer<Text> components)
        {
        }
    }
}