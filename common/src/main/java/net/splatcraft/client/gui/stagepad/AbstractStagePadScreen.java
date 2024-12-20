package net.splatcraft.client.gui.stagepad;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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
    protected static final Identifier COMMON_TEXTURE = Splatcraft.identifierOf("textures/gui/stage_pad/common.png");
    protected static final Identifier CONTROLLERS_TEXTURE = Splatcraft.identifierOf("textures/gui/stage_pad/controllers.png");
    protected static final Identifier WIDGETS = Splatcraft.identifierOf("textures/gui/stage_pad/widgets.png");
    public final StagePadItem.UseAction OPEN_SELF = (level, player, hand, stack, pos) -> client.setScreen(this);
    protected final ArrayList<MenuButton> buttons = new ArrayList<>();
    protected final ArrayList<MenuTextBox> textFields = new ArrayList<>();
    protected final ArrayList<MenuTextBox.Factory> textFieldFactories = new ArrayList<>();
    protected int backgroundWidth = 210;
    protected int backgroundHeight = 130;
    protected StagePadItem.UseAction useAction;

    protected AbstractStagePadScreen(Text label, @Nullable StagePadItem.UseAction useAction)
    {
        super(label);

        this.useAction = useAction == null ? OPEN_SELF : useAction;
        client = MinecraftClient.getInstance();
    }

    protected AbstractStagePadScreen(Text label)
    {
        this(label, null);
    }

    public <B extends MenuButton> B addButton(B button)
    {
        buttons.add(button);

        addSelectableChild(button);
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
            textFieldFactories.forEach(factory -> textFields.add(factory.newInstance(textRenderer)));
    }

    @Override
    public boolean shouldPause()
    {
        return false;
    }

    @Override
    public boolean keyPressed(int mouseX, int mouseY, int key)
    {
        for (EditBoxWidget textField : textFields)
        {
            if (textField.isFocused())
                textField.keyPressed(mouseX, mouseY, key);
        }
        return super.keyPressed(mouseX, mouseY, key);
    }

    @Override
    public boolean charTyped(char c, int p_94684_)
    {
        for (EditBoxWidget textField : textFields)
            if (textField.isFocused())
                textField.charTyped(c, p_94684_);
        return super.charTyped(c, p_94684_);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int clickButton)
    {
        if (canClickButtons())
            for (MenuButton button : buttons)
                if (button.isSelected() && button.isHovered())
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
                if (button.isFocused())
                    button.mouseDragged(mouseX, mouseY, mouseButton, p_94702_, p_94703_);
        return super.mouseDragged(mouseX, mouseY, mouseButton, p_94702_, p_94703_);
    }

    private void renderTooltips(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        for (MenuButton button : buttons)
            if (button.visible && button.isHovered())
                button.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void render(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        MenuButton hoveredButton = null;

        for (MenuButton button : buttons)
        {
            button.setHovered(false);
            if (button.isFocused() && isMouseOver(mouseX, mouseY, button))
                hoveredButton = button;
        }

        if (canClickButtons())
        {
            if (hoveredButton != null)
                hoveredButton.setHovered(true);
        }

        renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        handleWidgets(guiGraphics, mouseX, mouseY, partialTicks);

        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
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
    public void renderBackground(DrawContext guiGraphics, int mouseX, int mouseY, float delta)
    {
        super.renderBackground(guiGraphics, mouseX, mouseY, delta);

        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        RenderSystem.setShaderColor(1, 1, 1, 1);

        ItemStack stagePad = client.player.getStackInHand(client.player.getMainHandStack().getItem() instanceof StagePadItem ? Hand.MAIN_HAND : Hand.OFF_HAND);

        float[] rgb = ColorUtils.getInkColor(stagePad).getRGB();
        RenderSystem.setShaderColor(rgb[0], rgb[1], rgb[2], 1);
        RenderSystem.setShaderTexture(0, CONTROLLERS_TEXTURE);
        guiGraphics.drawTexture(CONTROLLERS_TEXTURE, x - 52, y, 0, 0, 54, 130);
        guiGraphics.drawTexture(CONTROLLERS_TEXTURE, x + backgroundWidth - 2, y, 62, 0, 54, 130);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        guiGraphics.drawTexture(CONTROLLERS_TEXTURE, x - 52, y, 116, 0, 54, 130);
        guiGraphics.drawTexture(CONTROLLERS_TEXTURE, x + backgroundWidth - 2, y, 178, 0, 54, 130);
        RenderSystem.setShaderTexture(0, COMMON_TEXTURE);
        guiGraphics.drawTexture(COMMON_TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);
    }

    public boolean isMouseOver(double mouseX, double mouseY, MenuButton button)
    {
        return isMouseOver(mouseX, mouseY, button.getX(), button.getY(), button.getX() + button.getWidth(), button.getY() + button.getHeight());
    }

    public boolean isMouseOver(double mouseX, double mouseY, double x1, double y1, double x2, double y2)
    {
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
    }

    public MenuButton.OnTooltip showText(Text... lines)
    {
        return (button, guiGraphics, x, y, partialTicks) ->
            guiGraphics.drawTooltip(MinecraftClient.getInstance().textRenderer, Arrays.stream(lines).toList(), Optional.empty(), x, y);
    }

    public MenuButton.OnTooltip showCopyPos(BlockPosGetter pos)
    {
        return (button, guiGraphics, x, y, partialTicks) ->
        {
            if (pos.get() != null)
                guiGraphics.drawTooltip(MinecraftClient.getInstance().textRenderer, Arrays.asList(Text.translatable("gui.stage_pad.button.position", pos.get().getX(), pos.get().getY(), pos.get().getZ()), Text.translatable("gui.stage_pad.button.copy_position").formatted(Formatting.YELLOW)),
                    Optional.empty(), x, y);
        };
    }

    public ButtonWidget.PressAction copyPos(BlockPosGetter pos)
    {
        return (button) ->
        {
            if (pos.get() != null)
                client.keyboard.setClipboard(pos.get().getX() + " " + pos.get().getY() + " " + pos.get().getZ());
        };
    }

    public MenuButton.PostDraw drawIcon(DrawContext guiGraphics, Identifier location, int xOff, int yOff, int texX, int texY, int texWidth, int texHeight)
    {
        return (no, button) ->
        {
            float color = button.active ? 1 : 0.5f;
            RenderSystem.setShaderColor(color, color, color, button.getAlpha());
            RenderSystem.setShaderTexture(0, location);
            guiGraphics.drawTexture(location, button.getX() + xOff, button.getY() + yOff, texX, texY, texWidth, texHeight);
            RenderSystem.setShaderColor(1, 1, 1, 1);
        };
    }

    public MenuButton.PostDraw drawToggleIcon(Identifier location, int xOff, int yOff, int texX, int texY, int texWidth, int texHeight, boolean offsetTx)
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
            guiGraphics.drawTexture(location, button.getX() + xOff + (t.toggle ? button.getWidth() / 2 : 0), button.getY() + yOff, texX + (offsetTx & t.toggle ? texWidth : 0), texY, texWidth, texHeight);
            RenderSystem.setShaderColor(1, 1, 1, 1);
        };
    }

    public MenuButton.PostDraw drawText(boolean centeredText)
    {
        return (guiGraphics, button) -> drawText(button.getMessage(), centeredText).apply(guiGraphics, button);
    }

    public MenuButton.PostDraw drawText(Text label, boolean centeredText)
    {
        return (guiGraphics, button) ->
        {
            int j = button.getFGColor();
            guiGraphics.drawTextWithShadow(textRenderer, label, button.getX() + (centeredText ? (button.getWidth() - textRenderer.getWidth(label)) / 2 : 3), button.getY() + (button.getHeight() - 8) / 2, j | MathHelper.ceil(button.getAlpha() * 255.0F) << 24);
        };
    }

    public ButtonWidget.PressAction goToScreen(ScreenFactory screen)
    {
        return (b) -> client.setScreen(screen.create());
    }

    public boolean canClickButtons()
    {
        return true;
    }

    public abstract void onStagesUpdate();

    public abstract void handleWidgets(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks);

    protected void addOptionsTabs(Text label, String stageId, Screen mainMenu)
    {
        addButton(new MenuButton(10, 12, 14, 12, goToScreen(() -> mainMenu), MenuButton.NO_TOOLTIP, (v, y) -> drawIcon(v, WIDGETS, 1, 0, 244, 24, 12, 12), MenuButton.ButtonColor.GREEN));
        addButton(new MenuButton(24, 12, 44, 12, goToScreen(() -> new StageSettingsScreen(label, stageId, mainMenu)), MenuButton.NO_TOOLTIP, drawText(Text.translatable("gui.stage_pad.tab.settings"), true), MenuButton.ButtonColor.PURPLE));
        addButton(new MenuButton(68, 12, 44, 12, goToScreen(() -> new StageRulesScreen(label, stageId, mainMenu)), MenuButton.NO_TOOLTIP, drawText(Text.translatable("gui.stage_pad.tab.rules"), true), MenuButton.ButtonColor.PURPLE));
        addButton(new MenuButton(112, 12, 44, 12, goToScreen(() -> new StageTeamsScreen(label, stageId, mainMenu)), MenuButton.NO_TOOLTIP, drawText(Text.translatable("gui.stage_pad.tab.teams"), true), MenuButton.ButtonColor.PURPLE));
        addButton(new MenuButton(156, 12, 44, 12, goToScreen(() -> new StageActionsScreen(label, stageId, mainMenu)), MenuButton.NO_TOOLTIP, drawText(Text.translatable("gui.stage_pad.tab.actions"), true), MenuButton.ButtonColor.PURPLE));
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