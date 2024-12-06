package net.splatcraft.client.gui.stagepad;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.MathHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.Splatcraft;
import net.splatcraft.items.StagePadItem;
import net.splatcraft.util.ColorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public abstract class AbstractStagePadScreen extends Screen
{
    protected static final ResourceLocation COMMON_TEXTURE = new ResourceLocation(Splatcraft.MODID, "textures/gui/stage_pad/common.png");
    protected static final ResourceLocation CONTROLLERS_TEXTURE = new ResourceLocation(Splatcraft.MODID, "textures/gui/stage_pad/controllers.png");
    protected static final ResourceLocation WIDGETS = new ResourceLocation(Splatcraft.MODID, "textures/gui/stage_pad/widgets.png");
    public final StagePadItem.UseAction OPEN_SELF = (level, player, hand, stack, pos) -> getMinecraft().setScreen(this);
    protected final ArrayList<MenuButton> buttons = new ArrayList<>();
    protected final ArrayList<MenuTextBox> textFields = new ArrayList<>();
    protected final ArrayList<MenuTextBox.Factory> textFieldFactories = new ArrayList<>();
    protected int imageWidth = 210;
    protected int imageHeight = 130;
    protected StagePadItem.UseAction useAction;

    protected AbstractStagePadScreen(Component label, @Nullable StagePadItem.UseAction useAction)
    {
        super(label);

        this.useAction = useAction == null ? OPEN_SELF : useAction;
        minecraft = Minecraft.getInstance();
    }

    protected AbstractStagePadScreen(Component label)
    {
        this(label, null);
    }

    public <B extends MenuButton> B addButton(B button)
    {
        buttons.add(button);
        addWidget(button);
        return button;
    }

    public void addTextBox(MenuTextBox.Factory factory)
    {
        textFieldFactories.add(factory);
    }

    @Override
    protected void init()
    {
        super.init();

        StagePadItem.clientUseAction = useAction;

        if (textFields.isEmpty())
            textFieldFactories.forEach(factory -> textFields.add(factory.newInstance(font)));
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    public boolean keyPressed(int mouseX, int mouseY, int key)
    {
        for (EditBox textField : textFields)
        {
            if (textField.isFocused())
                textField.keyPressed(mouseX, mouseY, key);
        }
        return super.keyPressed(mouseX, mouseY, key);
    }

    @Override
    public boolean charTyped(char c, int p_94684_)
    {
        for (EditBox textField : textFields)
            if (textField.isFocused())
                textField.charTyped(c, p_94684_);
        return super.charTyped(c, p_94684_);
    }

    @Override
    public void tick()
    {
        super.tick();
        for (EditBox textField : textFields)
            textField.tick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int clickButton)
    {
        if (canClickButtons())
            for (MenuButton button : buttons)
                if (button.isActive() && button.isHovered())
                    button.mouseClicked(mouseX, mouseY, clickButton);
        return super.mouseClicked(mouseX, mouseY, clickButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int clickButton)
    {
        if (canClickButtons())
            for (MenuButton button : buttons)
                button.mouseReleased(mouseX, mouseY, clickButton);
        return super.mouseClicked(mouseX, mouseY, clickButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double p_94702_, double p_94703_)
    {
        if (canClickButtons())
            for (MenuButton button : buttons)
                if (button.isActive())
                    button.mouseDragged(mouseX, mouseY, mouseButton, p_94702_, p_94703_);
        return super.mouseDragged(mouseX, mouseY, mouseButton, p_94702_, p_94703_);
    }

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        for (MenuButton button : buttons)
            if (button.visible && button.isHovered())
                button.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        MenuButton hoveredButton = null;

        for (MenuButton button : buttons)
        {
            button.setHovered(false);
            if (button.isActive() && isMouseOver(mouseX, mouseY, button))
                hoveredButton = button;
        }

        if (canClickButtons())
        {
            if (hoveredButton != null)
                hoveredButton.setHovered(true);
        }

        renderBackground(guiGraphics);
        handleWidgets(guiGraphics, mouseX, mouseY, partialTicks);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        for (MenuButton button : buttons)
        {
            button.setX(button.relativeX + x);
            button.setY(button.relativeY + y);
        }
        for (MenuTextBox button : textFields)
        {
            button.setX(button.relativeX + x);
            button.setY(button.relativeY + y);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        renderTooltips(guiGraphics, mouseX, mouseY, partialTicks);

        buttons.forEach(b -> b.render(guiGraphics, mouseX, mouseY, partialTicks));
        textFields.forEach(t -> t.render(guiGraphics, mouseX, mouseY, partialTicks));
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics)
    {
        super.renderBackground(guiGraphics);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        RenderSystem.setShaderColor(1, 1, 1, 1);

        ItemStack stagePad = getMinecraft().player.getItemInHand(getMinecraft().player.getMainHandItem().getItem() instanceof StagePadItem ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);

        float[] rgb = ColorUtils.hexToRGB(ColorUtils.getInkColor(stagePad));
        RenderSystem.setShaderColor(rgb[0], rgb[1], rgb[2], 1);
        RenderSystem.setShaderTexture(0, CONTROLLERS_TEXTURE);
        guiGraphics.blit(CONTROLLERS_TEXTURE, x - 52, y, 0, 0, 54, 130);
        guiGraphics.blit(CONTROLLERS_TEXTURE, x + imageWidth - 2, y, 62, 0, 54, 130);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        guiGraphics.blit(CONTROLLERS_TEXTURE, x - 52, y, 116, 0, 54, 130);
        guiGraphics.blit(CONTROLLERS_TEXTURE, x + imageWidth - 2, y, 178, 0, 54, 130);
        RenderSystem.setShaderTexture(0, COMMON_TEXTURE);
        guiGraphics.blit(COMMON_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    public boolean isMouseOver(double mouseX, double mouseY, MenuButton button)
    {
        return isMouseOver(mouseX, mouseY, button.getX(), button.getY(), button.getX() + button.getWidth(), button.getY() + button.getHeight());
    }

    public boolean isMouseOver(double mouseX, double mouseY, double x1, double y1, double x2, double y2)
    {
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
    }

    public MenuButton.OnTooltip showText(Component... lines)
    {
        return (button, guiGraphics, x, y, partialTicks) ->
            guiGraphics.renderTooltip(Minecraft.getInstance().font, Arrays.stream(lines).toList(), Optional.empty(), x, y);
    }

    public MenuButton.OnTooltip showCopyPos(BlockPosGetter pos)
    {
        return (button, guiGraphics, x, y, partialTicks) ->
        {
            if (pos.get() != null)
                guiGraphics.renderTooltip(Minecraft.getInstance().font, Arrays.asList(Component.translatable("gui.stage_pad.button.position", pos.get().getX(), pos.get().getY(), pos.get().getZ()), Component.translatable("gui.stage_pad.button.copy_position").withStyle(ChatFormatting.YELLOW)),
                    Optional.empty(), x, y);
        };
    }

    public Button.OnPress copyPos(BlockPosGetter pos)
    {
        return (button) ->
        {
            if (pos.get() != null)
                getMinecraft().keyboardHandler.setClipboard(pos.get().getX() + " " + pos.get().getY() + " " + pos.get().getZ());
        };
    }

    public MenuButton.PostDraw drawIcon(GuiGraphics guiGraphics, ResourceLocation location, int xOff, int yOff, int texX, int texY, int texWidth, int texHeight)
    {
        return (no, button) ->
        {
            float color = button.active ? 1 : 0.5f;
            RenderSystem.setShaderColor(color, color, color, button.getAlpha());
            RenderSystem.setShaderTexture(0, location);
            guiGraphics.blit(location, button.getX() + xOff, button.getY() + yOff, texX, texY, texWidth, texHeight);
            RenderSystem.setShaderColor(1, 1, 1, 1);
        };
    }

    public MenuButton.PostDraw drawToggleIcon(ResourceLocation location, int xOff, int yOff, int texX, int texY, int texWidth, int texHeight, boolean offsetTx)
    {
        return (guiGraphics, button) ->
        {
            if (!(button instanceof StageSelectionScreen.ToggleMenuButton t))
            {
                drawIcon(guiGraphics, location, xOff, yOff, texX, texY, texWidth, texHeight).apply(guiGraphics, button);
                return;
            }
            float color = button.active ? 1 : 0.5f;
            RenderSystem.setShaderColor(color, color, color, button.getAlpha());
            RenderSystem.setShaderTexture(0, location);
            guiGraphics.blit(location, button.getX() + xOff + (t.toggle ? button.getWidth() / 2 : 0), button.getY() + yOff, texX + (offsetTx & t.toggle ? texWidth : 0), texY, texWidth, texHeight);
            RenderSystem.setShaderColor(1, 1, 1, 1);
        };
    }

    public MenuButton.PostDraw drawText(boolean centeredText)
    {
        return (guiGraphics, button) -> drawText(button.getMessage(), centeredText).apply(guiGraphics, button);
    }

    public MenuButton.PostDraw drawText(Component label, boolean centeredText)
    {
        return (guiGraphics, button) ->
        {
            int j = button.getFGColor();
            guiGraphics.drawString(font, label, button.getX() + (centeredText ? (button.getWidth() - font.width(label)) / 2 : 3), button.getY() + (button.getHeight() - 8) / 2, j | MathHelper.ceil(button.getAlpha() * 255.0F) << 24);
        };
    }

    public Button.OnPress goToScreen(ScreenFactory screen)
    {
        return (b) -> getMinecraft().setScreen(screen.create());
    }

    public boolean canClickButtons()
    {
        return true;
    }

    public abstract void onStagesUpdate();

    public abstract void handleWidgets(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks);

    protected void addOptionsTabs(Component label, String stageId, Screen mainMenu)
    {
        addButton(new MenuButton(10, 12, 14, 12, goToScreen(() -> mainMenu), MenuButton.NO_TOOLTIP, (v, y) -> drawIcon(v, WIDGETS, 1, 0, 244, 24, 12, 12), MenuButton.ButtonColor.GREEN));
        addButton(new MenuButton(24, 12, 44, 12, goToScreen(() -> new StageSettingsScreen(label, stageId, mainMenu)), MenuButton.NO_TOOLTIP, drawText(Component.translatable("gui.stage_pad.tab.settings"), true), MenuButton.ButtonColor.PURPLE));
        addButton(new MenuButton(68, 12, 44, 12, goToScreen(() -> new StageRulesScreen(label, stageId, mainMenu)), MenuButton.NO_TOOLTIP, drawText(Component.translatable("gui.stage_pad.tab.rules"), true), MenuButton.ButtonColor.PURPLE));
        addButton(new MenuButton(112, 12, 44, 12, goToScreen(() -> new StageTeamsScreen(label, stageId, mainMenu)), MenuButton.NO_TOOLTIP, drawText(Component.translatable("gui.stage_pad.tab.teams"), true), MenuButton.ButtonColor.PURPLE));
        addButton(new MenuButton(156, 12, 44, 12, goToScreen(() -> new StageActionsScreen(label, stageId, mainMenu)), MenuButton.NO_TOOLTIP, drawText(Component.translatable("gui.stage_pad.tab.actions"), true), MenuButton.ButtonColor.PURPLE));
    }

    public interface BlockPosGetter
    {
        BlockPos get();
    }

    public interface ScreenFactory
    {
        Screen create();
    }
}