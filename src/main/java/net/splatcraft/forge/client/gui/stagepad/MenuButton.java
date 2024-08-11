package net.splatcraft.forge.client.gui.stagepad;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.Mth;

import static net.splatcraft.forge.client.gui.stagepad.AbstractStagePadScreen.WIDGETS;

public class MenuButton extends Button
{
	ButtonColor color;
	final PostDraw draw;

	int relativeX;
	int relativeY;

	public MenuButton(int x, int y, int width, OnPress onPress, OnTooltip onTooltip, PostDraw draw, ButtonColor color)
	{
		this(x, y, width, 12, onPress, onTooltip, draw, color);
	}
	public MenuButton(int x, int y, int width, int height, OnPress onPress, OnTooltip onTooltip, PostDraw draw,  ButtonColor color)
	{
		super(x, y, width, height, TextComponent.EMPTY, onPress, onTooltip);
		this.color = color;
		this.draw = draw;
		relativeX = x;
		relativeY = y;
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
	{
		if (this.visible)
		{
			//this.isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
			this.renderButton(poseStack, mouseX, mouseY, partialTicks);

			//if(isHoveredOrFocused())
			//	this.renderToolTip(poseStack, mouseX, mouseY);
		}
	}

	@Override
	public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		if(!visible)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, WIDGETS);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
		int i = this.getYImage(this.isHoveredOrFocused());
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		this.blit(poseStack, this.x, this.y, 0, getColor().ordinal() * 36 + i * 12, this.width / 2, this.height);
		this.blit(poseStack, this.x + this.width / 2, this.y, 180 - this.width / 2, getColor().ordinal() * 36 + i * 12, this.width / 2, this.height);

		draw.apply(poseStack, this);

	}

	@Override
	protected int getYImage(boolean hovered)
	{
		return active ? (hovered ? 2 : 1) : 0;
	}

	public float getAlpha()
	{
		return alpha;
	}

	@Override
	public int getFGColor() {
		return 0xFFFFFF;
	}

	public boolean isHovered()
	{
		return isHovered;
	}

	public void setHovered(boolean hovered)
	{
		isHovered = hovered;
	}

	public ButtonColor getColor() {
		return color;
	}

	public void setColor(ButtonColor color) {
		this.color = color;
	}

	public interface PostDraw
	{
		void apply(PoseStack poseStack, MenuButton button);
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
}