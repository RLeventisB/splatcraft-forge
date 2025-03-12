package net.splatcraft.client.gui.stagepad;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static net.splatcraft.client.gui.stagepad.AbstractStagePadScreen.WIDGETS;

public class MenuButton extends Button
{
	public static final OnTooltip NO_TOOLTIP = (h, e, l, o, partialTicks) ->
	{
	};
	public static OnPress DO_NOTHING = (v) ->
	{
	};
	final PostDraw draw;
	final OnTooltip onTooltip;
	ButtonColor color;
	int relativeX;
	int relativeY;
	public MenuButton(int x, int y, int width, OnPress onPress, OnTooltip onTooltip, PostDraw draw, ButtonColor color)
	{
		this(x, y, width, 12, onPress, onTooltip, draw, color);
	}
	public MenuButton(int x, int y, int width, int height, OnPress onPress, OnTooltip onTooltip, PostDraw draw, ButtonColor color)
	{
		super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
		this.onTooltip = onTooltip;
		this.color = color;
		this.draw = draw;
		relativeX = x;
		relativeY = y;
	}
	@Override
	public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
	{
		if (!visible)
			return;
		
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, WIDGETS);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
		int i = getYImage(isHoveredOrFocused());
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		guiGraphics.blit(WIDGETS, getX(), getY(), 0, getColor().ordinal() * 36 + i * 12, width / 2, height);
		guiGraphics.blit(WIDGETS, getX() + width / 2, getY(), 180 - width / 2, getColor().ordinal() * 36 + i * 12, width / 2, height);
		draw.apply(guiGraphics, this);
		
		if (isHoveredOrFocused())
			drawTooltip(guiGraphics, mouseX, mouseY, partialTicks);
	}
	public void drawTooltip(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
	{
		onTooltip.onTooltip(this, guiGraphics, mouseX, mouseY, partialTicks);
	}
	@Override
	protected void defaultButtonNarrationText(NarrationElementOutput builder)
	{
		super.defaultButtonNarrationText(builder);
		onTooltip.narrateTooltip(v -> builder.add(NarratedElementType.HINT, v));
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
		this.isHovered = hovered;
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
		void apply(GuiGraphics guiGraphics, MenuButton button);
	}
	@OnlyIn(Dist.CLIENT)
	public interface OnTooltip
	{
		void onTooltip(MenuButton button, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks);
		default void narrateTooltip(Consumer<Component> components)
		{
		}
	}
}