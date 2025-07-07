package net.splatcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.Splatcraft;
import net.splatcraft.tileentities.container.InkVatContainer;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class InkVatScreen extends AbstractContainerScreen<InkVatContainer>
{
	private static final ResourceLocation TEXTURES = Splatcraft.identifierOf("textures/gui/inkwell_crafting.png");
	private static final int colorSelectionX = 12;
	private static final int colorSelectionY = 16;
	private static final int scrollBarX = 15;
	private static final int scrollBarY = 55;
	private boolean scrolling = false;
	private boolean canScroll = false;
	private float maxScroll = 0;
	private float scroll = 0.0f;
	public InkVatScreen(InkVatContainer screenContainer, Inventory inv, Component titleIn)
	{
		super(screenContainer, inv, titleIn);
		imageHeight = 208;
		titleLabelX = 8;
		titleLabelY = imageHeight - 92;
	}
	@Override
	public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
	{
		renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		renderTooltip(guiGraphics, mouseX, mouseY);
	}
	@Override
	protected void renderTooltip(@NotNull GuiGraphics pGuiGraphics, int mouseX, int mouseY)
	{
		List<InkColor> colorSelection = getMenu().sortRecipeList();
		
		super.renderTooltip(pGuiGraphics, mouseX, mouseY);
		int sc = (int) Math.ceil(Math.max(0, (colorSelection.size() - 16) * scroll));
		sc += sc % 2;
		
		for (int i = sc; i < colorSelection.size() && i - sc < 16; i++)
		{
			int x = colorSelectionX + (i - sc) / 2 * 19;
			int y = colorSelectionY + (i - sc) % 2 * 18;
			
			if (isHovering(x, y, 17, 16, mouseX, mouseY))
			{
				pGuiGraphics.renderTooltip(font, ColorUtils.getFormatedColorName(colorSelection.get(i), false), mouseX, mouseY);
			}
		}
	}
	@Override
	protected void renderLabels(@NotNull GuiGraphics drawContext, int mouseX, int mouseY)
	{
		drawContext.drawString(font, title.getString(), imageWidth / 2 - font.width(title.getString()) / 2, 6, 4210752);
		
		List<InkColor> colors = getMenu().sortRecipeList();
		drawAvailableColors(drawContext, colors, colorSelectionX, colorSelectionY);
		canScroll = colors.size() > 16;
		maxScroll = (float) Math.ceil(colors.size() / 2.0) - 8;
		
		drawScrollBar(drawContext, scrollBarX, scrollBarY, 132, mouseX, mouseY);
	}
	protected void drawAvailableColors(GuiGraphics guiGraphics, List<InkColor> colorSelection, int x, int y)
	{
		TextureManager textureManager = minecraft.getTextureManager();
		if (textureManager != null)
		{
			RenderSystem.setShaderTexture(0, TEXTURES);
			int sc = (int) Math.ceil(Math.max(0, (colorSelection.size() - 16) * scroll));
			sc += sc % 2;
			for (int i = sc; i < colorSelection.size() && i - sc < 16; i++)
			{
				InkColor color = colorSelection.get(i);
				float[] rgb = color.getRGB();
				
				int cx = x + (i - sc) / 2 * 19;
				int cy = y + (i - sc) % 2 * 18;
				
				RenderSystem.setShaderColor(rgb[0], rgb[2], rgb[2], 1);
				guiGraphics.blit(TEXTURES, cx, cy, 34, 220, 19, 18);
				RenderSystem.setShaderColor(1, 1, 1, 1);
				
				if (getMenu().getSelectedRecipe() == i)
				{
					guiGraphics.blit(TEXTURES, cx, cy, 34, 238, 19, 18);
				}
			}
		}
	}
	protected void drawScrollBar(GuiGraphics guiGraphics, int x, int y, int width, int mouseX, int mouseY)
	{
		TextureManager textureManager = minecraft.getTextureManager();
		if (textureManager != null)
		{
			RenderSystem.setShaderTexture(0, TEXTURES);
			if (canScroll)
			{
				guiGraphics.blit(TEXTURES, (int) (x + width * scroll), y, 241, isHovering(15, 55, 146, 10, mouseX, mouseY) || scrolling ? 20 : 0, 15, 10);
			}
			else
			{
				guiGraphics.blit(TEXTURES, x, y, 241, 10, 15, 10);
			}
		}
	}
	@Override
	protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY)
	{
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.setShaderTexture(0, TEXTURES);
		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;
		
		guiGraphics.blit(TEXTURES, x, y, 0, 0, imageWidth, imageHeight);
		
		InkVatContainer container = getMenu();
		if (!container.getSlot(0).hasItem())
		{
			guiGraphics.blit(TEXTURES, x + 26, y + 70, 176, 0, 16, 16);
		}
		if (!container.getSlot(1).hasItem())
		{
			guiGraphics.blit(TEXTURES, x + 46, y + 70, 192, 0, 16, 16);
		}
		if (!container.getSlot(2).hasItem())
		{
			guiGraphics.blit(TEXTURES, x + 92, y + 82, 208, 0, 16, 16);
		}
		if (!container.getSlot(3).hasItem())
		{
			guiGraphics.blit(TEXTURES, x + 36, y + 89, 224, 0, 16, 16);
		}
	}
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
	{
		List<InkColor> colorSelection = getMenu().sortRecipeList();
		scrolling = false;
		
		int sc = (int) Math.ceil(Math.max(0, (colorSelection.size() - 16) * scroll));
		sc += sc % 2;
		
		for (int i = sc; i < colorSelection.size() && i - sc < 16; i++)
		{
			int x = colorSelectionX + (i - sc) / 2 * 19;
			int y = colorSelectionY + (i - sc) % 2 * 18;
			
			if (isHovering(x, y, 19, 18, mouseX, mouseY) && mouseButton == 0 && minecraft != null && minecraft.player != null && getMenu().clickMenuButton(minecraft.player, i))
			{
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
				MultiPlayerGameMode playerController = minecraft.gameMode;
				if (playerController != null)
				{
					minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, i);
				}
				getMenu().updateInkVatColor(i, colorSelection.get(i));
			}
		}
		
		if (isHovering(scrollBarX, scrollBarY, 146, 10, mouseX, mouseY) && canScroll)
		{
			scrolling = true;
			scroll = Mth.clamp((float) (mouseX - leftPos - scrollBarX) / 132f, 0f, 1f);
		}
		
		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button)
	{
		if (button == 0)
		{
			scrolling = false;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}
	@Override
	public boolean mouseDragged(double x, double y, int mouseButton, double p_231045_6_, double p_231045_8_)
	{
		if (scrolling && canScroll)
		{
			scroll = Mth.clamp((float) (x - x - scrollBarX) / 132f, 0f, 1f);
		}
		
		return super.mouseDragged(x, y, mouseButton, p_231045_6_, p_231045_8_);
	}
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)
	{
		if (canScroll)
		{
			scroll = Mth.clamp(scroll + 1 / maxScroll * -Math.signum((float) verticalAmount), 0.0f, 1.0f);
		}
		
		return true;
	}
}
