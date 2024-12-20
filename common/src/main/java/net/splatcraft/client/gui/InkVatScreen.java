package net.splatcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.splatcraft.Splatcraft;
import net.splatcraft.tileentities.container.InkVatContainer;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Environment(EnvType.CLIENT)
public class InkVatScreen extends HandledScreen<InkVatContainer>
{
    private static final Identifier TEXTURES = Splatcraft.identifierOf("textures/gui/inkwell_crafting.png");
    private static final int colorSelectionX = 12;
    private static final int colorSelectionY = 16;
    private static final int scrollBarX = 15;
    private static final int scrollBarY = 55;
    private boolean scrolling = false;
    private boolean canScroll = false;
    private float maxScroll = 0;
    private float scroll = 0.0f;

    public InkVatScreen(InkVatContainer screenContainer, PlayerInventory inv, Text titleIn)
    {
        super(screenContainer, inv, titleIn);
        backgroundHeight = 208;
        titleX = 8;
        titleY = backgroundHeight - 92;
    }

    @Override
    public void render(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        drawMouseoverTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void drawMouseoverTooltip(@NotNull DrawContext pGuiGraphics, int mouseX, int mouseY)
    {
        List<InkColor> colorSelection = getScreenHandler().sortRecipeList();

        super.drawMouseoverTooltip(pGuiGraphics, mouseX, mouseY);
        int sc = (int) Math.ceil(Math.max(0, (colorSelection.size() - 16) * scroll));
        sc += sc % 2;

        for (int i = sc; i < colorSelection.size() && i - sc < 16; i++)
        {
            int x = colorSelectionX + (i - sc) / 2 * 19;
            int y = colorSelectionY + (i - sc) % 2 * 18;

            if (isPointWithinBounds(x, y, 17, 16, mouseX, mouseY))
            {
                pGuiGraphics.drawTooltip(textRenderer, ColorUtils.getFormatedColorName(colorSelection.get(i), false), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void drawForeground(@NotNull DrawContext drawContext, int mouseX, int mouseY)
    {
        drawContext.drawTextWithShadow(textRenderer, title.getString(), backgroundWidth / 2 - textRenderer.getWidth(title.getString()) / 2, 6, 4210752);

        List<InkColor> colors = getScreenHandler().sortRecipeList();
        drawAvailableColors(drawContext, colors, colorSelectionX, colorSelectionY);
        canScroll = colors.size() > 16;
        maxScroll = (float) Math.ceil(colors.size() / 2.0) - 8;

        drawScrollBar(drawContext, scrollBarX, scrollBarY, 132, mouseX, mouseY);
    }

    protected void drawAvailableColors(DrawContext guiGraphics, List<InkColor> colorSelection, int x, int y)
    {
        TextureManager textureManager = client.getTextureManager();
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
                guiGraphics.drawTexture(TEXTURES, cx, cy, 34, 220, 19, 18);
                RenderSystem.setShaderColor(1, 1, 1, 1);

                if (getScreenHandler().getSelectedRecipe() == i)
                {
                    guiGraphics.drawTexture(TEXTURES, cx, cy, 34, 238, 19, 18);
                }
            }
        }
    }

    protected void drawScrollBar(DrawContext guiGraphics, int x, int y, int width, int mouseX, int mouseY)
    {
        TextureManager textureManager = client.getTextureManager();
        if (textureManager != null)
        {
            RenderSystem.setShaderTexture(0, TEXTURES);
            if (canScroll)
            {
                guiGraphics.drawTexture(TEXTURES, (int) (x + width * scroll), y, 241, isPointWithinBounds(15, 55, 146, 10, mouseX, mouseY) || scrolling ? 20 : 0, 15, 10);
            }
            else
            {
                guiGraphics.drawTexture(TEXTURES, x, y, 241, 10, 15, 10);
            }
        }
    }

    @Override
    protected void drawBackground(@NotNull DrawContext guiGraphics, float partialTicks, int mouseX, int mouseY)
    {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, TEXTURES);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        guiGraphics.drawTexture(TEXTURES, x, y, 0, 0, backgroundWidth, backgroundHeight);

        InkVatContainer container = getScreenHandler();
        if (!container.getSlot(0).hasStack())
        {
            guiGraphics.drawTexture(TEXTURES, x + 26, y + 70, 176, 0, 16, 16);
        }
        if (!container.getSlot(1).hasStack())
        {
            guiGraphics.drawTexture(TEXTURES, x + 46, y + 70, 192, 0, 16, 16);
        }
        if (!container.getSlot(2).hasStack())
        {
            guiGraphics.drawTexture(TEXTURES, x + 92, y + 82, 208, 0, 16, 16);
        }
        if (!container.getSlot(3).hasStack())
        {
            guiGraphics.drawTexture(TEXTURES, x + 36, y + 89, 224, 0, 16, 16);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
    {
        List<InkColor> colorSelection = getScreenHandler().sortRecipeList();
        scrolling = false;

        int sc = (int) Math.ceil(Math.max(0, (colorSelection.size() - 16) * scroll));
        sc += sc % 2;

        for (int i = sc; i < colorSelection.size() && i - sc < 16; i++)
        {
            int x = colorSelectionX + (i - sc) / 2 * 19;
            int y = colorSelectionY + (i - sc) % 2 * 18;

            if (isPointWithinBounds(x, y, 19, 18, mouseX, mouseY) && mouseButton == 0 && client != null && client.player != null && getScreenHandler().onButtonClick(client.player, i))
            {
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                ClientPlayerInteractionManager playerController = client.interactionManager;
                if (playerController != null)
                {
                    client.interactionManager.clickButton(getScreenHandler().syncId, i);
                }
                getScreenHandler().updateInkVatColor(i, colorSelection.get(i));
            }
        }

        if (isPointWithinBounds(scrollBarX, scrollBarY, 146, 10, mouseX, mouseY) && canScroll)
        {
            scrolling = true;
            scroll = MathHelper.clamp((float) (mouseX - x - scrollBarX) / 132f, 0f, 1f);
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
            scroll = MathHelper.clamp((float) (x - x - scrollBarX) / 132f, 0f, 1f);
        }

        return super.mouseDragged(x, y, mouseButton, p_231045_6_, p_231045_8_);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)
    {
        if (canScroll)
        {
            scroll = MathHelper.clamp(scroll + 1 / maxScroll * -Math.signum((float) verticalAmount), 0.0f, 1.0f);
        }

        return true;
    }
}
