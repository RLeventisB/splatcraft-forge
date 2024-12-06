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
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.splatcraft.Splatcraft;
import net.splatcraft.tileentities.InkVatTileEntity;
import net.splatcraft.tileentities.container.InkVatContainer;
import net.splatcraft.util.ColorUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class InkVatScreen extends AbstractContainerScreen<InkVatContainer>
{
    private static final ResourceLocation TEXTURES = new ResourceLocation(Splatcraft.MODID, "textures/gui/inkwell_crafting.png");
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
        this.imageHeight = 208;
        this.titleLabelX = 8;
        this.titleLabelY = this.imageHeight - 92;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics pGuiGraphics, int mouseX, int mouseY)
    {
        List<Integer> colorSelection = getMenu().sortRecipeList();

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
    protected void renderLabels(@NotNull GuiGraphics pGuiGraphics, int mouseX, int mouseY)
    {
        pGuiGraphics.drawString(font, title.getString(), imageWidth / 2 - font.width(title.getString()) / 2, 6, 4210752);

        List<Integer> colors = getMenu().sortRecipeList();
        drawAvailableColors(pGuiGraphics, colors, colorSelectionX, colorSelectionY);
        canScroll = colors.size() > 16;
        maxScroll = (float) Math.ceil(colors.size() / 2.0) - 8;

        drawScrollBar(pGuiGraphics, scrollBarX, scrollBarY, 132, mouseX, mouseY);
    }

    protected void drawAvailableColors(GuiGraphics guiGraphics, List<Integer> colorSelection, int x, int y)
    {
        TextureManager textureManager = minecraft.getTextureManager();
        if (textureManager != null)
        {
            RenderSystem.setShaderTexture(0, TEXTURES);
            int sc = (int) Math.ceil(Math.max(0, (colorSelection.size() - 16) * scroll));
            sc += sc % 2;
            for (int i = sc; i < colorSelection.size() && i - sc < 16; i++)
            {
                int color = colorSelection.get(i);

                float r = (float) (color >> 16 & 255) / 255.0F;
                float g = (float) (color >> 8 & 255) / 255.0F;
                float b = (float) (color & 255) / 255.0F;

                int cx = x + (i - sc) / 2 * 19;
                int cy = y + (i - sc) % 2 * 18;

                RenderSystem.setShaderColor(r, g, b, 1);
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

        InkVatTileEntity te = getMenu().te;
        if (te.getItem(0).isEmpty())
        {
            guiGraphics.blit(TEXTURES, leftPos + 26, topPos + 70, 176, 0, 16, 16);
        }
        if (te.getItem(1).isEmpty())
        {
            guiGraphics.blit(TEXTURES, leftPos + 46, topPos + 70, 192, 0, 16, 16);
        }
        if (te.getItem(2).isEmpty())
        {
            guiGraphics.blit(TEXTURES, leftPos + 92, topPos + 82, 208, 0, 16, 16);
        }
        if (te.getItem(3).isEmpty())
        {
            guiGraphics.blit(TEXTURES, leftPos + 36, topPos + 89, 224, 0, 16, 16);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
    {
        List<Integer> colorSelection = getMenu().sortRecipeList();
        scrolling = false;

        int sc = (int) Math.ceil(Math.max(0, (colorSelection.size() - 16) * scroll));
        sc += sc % 2;

        for (int i = sc; i < colorSelection.size() && i - sc < 16; i++)
        {
            int x = colorSelectionX + (i - sc) / 2 * 19;
            int y = colorSelectionY + (i - sc) % 2 * 18;

            if (isHovering(x, y, 19, 18, mouseX, mouseY) && mouseButton == 0 && this.minecraft != null && this.minecraft.player != null && this.getMenu().clickMenuButton(this.minecraft.player, i))
            {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                MultiPlayerGameMode playerController = this.minecraft.gameMode;
                if (playerController != null)
                {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, i);
                }
                getMenu().updateInkVatColor(i, colorSelection.get(i));
            }
        }

        if (isHovering(scrollBarX, scrollBarY, 146, 10, mouseX, mouseY) && canScroll)
        {
            scrolling = true;
            scroll = MathHelper.clamp((float) (mouseX - leftPos - scrollBarX) / 132f, 0f, 1f);
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
            scroll = MathHelper.clamp((float) (x - leftPos - scrollBarX) / 132f, 0f, 1f);
        }

        return super.mouseDragged(x, y, mouseButton, p_231045_6_, p_231045_8_);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount)
    {
        if (canScroll)
        {
            scroll = MathHelper.clamp(scroll + 1 / maxScroll * -Math.signum((float) amount), 0.0f, 1.0f);
        }

        return true;
    }
}
