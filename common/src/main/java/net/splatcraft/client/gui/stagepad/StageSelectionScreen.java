package net.splatcraft.client.gui.stagepad;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.MathHelper;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.Stage;
import net.splatcraft.items.StagePadItem;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.RequestUpdateStageSpawnPadsPacket;
import net.splatcraft.network.c2s.RequestWarpDataPacket;
import net.splatcraft.network.c2s.SuperJumpToStagePacket;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class StageSelectionScreen extends AbstractStagePadScreen
{
    static final TreeMap<Stage, Pair<MenuButton, SuperJumpMenuButton>> stages = new TreeMap<>();
    private static final ResourceLocation TEXTURES = new ResourceLocation(Splatcraft.MODID, "textures/gui/stage_pad/stage_select.png");
    public static StageSelectionScreen instance;
    MenuTextBox searchBar;
    MenuButton createStageButton;
    MenuButton toggleSearchBarButton;
    int buttonListSize = 0;
    String prevSearchBarText = "";
    private double scroll = 0;
    private boolean scrollBarHeld = false;

    public StageSelectionScreen(Component title)
    {
        super(title, StagePadItem.OPEN_MAIN_MENU);

        instance = this;

        createStageButton = addButton(new MenuButton(10, 0, 178, 12,
            goToScreen(() -> new StageCreationScreen(title, this, "", null, null)), MenuButton.NO_TOOLTIP,
            drawText(Component.translatable("gui.stage_pad.button.create_stage"), false), MenuButton.ButtonColor.LIME));
        stages.clear();

        toggleSearchBarButton = addButton(new ToggleMenuButton(176, 12, 24, 12, (b) ->
        {
            searchBar.visible = !searchBar.visible;
            searchBar.setFocused(searchBar.visible);
            if (!searchBar.visible)
                searchBar.setValue("");
        }, showText(Component.translatable("gui.stage_pad.button.search_stage")), drawToggleIcon(WIDGETS, 0, 0, 232, 12, 12, 12, false), MenuButton.ButtonColor.PURPLE, false));

        addTextBox(font ->
        {
            searchBar = new MenuTextBox(font, 11, 13, 175, 10, Component.translatable("gui.stage_pad.textbox.search_stage"), false);
            searchBar.setFocused(true);
            searchBar.visible = false;
            return searchBar;
        });

        onStagesUpdate();
    }

    public static void updateValidSuperJumpsList(List<String> validStages, List<String> outOfReachStages, List<String> needsUpdateStages)
    {
        stages.values().stream().map(Pair::getSecond).forEach(jumpButton ->
        {
            SuperJumpMenuButton.loading = false;
            jumpButton.state = validStages.contains(jumpButton.stage.id) ? SuperJumpMenuButton.ButtonState.VALID :
                needsUpdateStages.contains(jumpButton.stage.id) ? SuperJumpMenuButton.ButtonState.REQUIRES_UPDATE :
                    outOfReachStages.contains(jumpButton.stage.id) ? SuperJumpMenuButton.ButtonState.OUT_OF_RANGE :
                        SuperJumpMenuButton.ButtonState.NO_SPAWN_PADS;
        });
    }

    @Override
    public void onStagesUpdate()
    {
        ArrayList<Stage> stages = Stage.getAllStages(getMinecraft().level);

        if (!stages.equals(new ArrayList<>(StageSelectionScreen.stages.keySet())))
        {
            SplatcraftPacketHandler.sendToServer(new RequestWarpDataPacket());
            stages.forEach(this::addStageButton);
        }
    }

    public void addStageButton(Stage stage)
    {
        MenuButton stageButton = new MenuButton(10, 0, 166, goToScreen(() -> new StageSettingsScreen(getTitle(), stage.id, this)), MenuButton.NO_TOOLTIP, drawText(false), MenuButton.ButtonColor.GREEN)
        {
            @Override
            public @NotNull Component getMessage()
            {
                return Stage.getStage(Minecraft.getInstance().level, stage.id).getStageName();
            }
        };
        SuperJumpMenuButton jumpButton = new SuperJumpMenuButton(176, 0, 12,
            (button, poseStack, mx, my, partialTicks) -> poseStack.renderTooltip(Minecraft.getInstance().font, List.of(((SuperJumpMenuButton) button).state.tooltipText), Optional.empty(), mx, my),
            (guiGraphics, button) -> drawIcon(guiGraphics, WIDGETS, 0, 0, 244, ((SuperJumpMenuButton) button).state == SuperJumpMenuButton.ButtonState.REQUIRES_UPDATE ? 12 : 0, 12, 12).apply(guiGraphics, button), stage);
        jumpButton.active = false;

        if (stages.containsKey(stage))
        {
            Pair<MenuButton, SuperJumpMenuButton> pair = stages.get(stage);
            buttons.remove(pair.getFirst());
            removeWidget(pair.getFirst());
            buttons.remove(pair.getSecond());
            removeWidget(pair.getSecond());
        }

        stages.put(stage, new Pair<>(stageButton, jumpButton));
        addButton(stageButton);
        addButton(jumpButton);
    }

    @Override
    public void handleWidgets(GuiGraphics matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        List<Pair<MenuButton, SuperJumpMenuButton>> stageButtons = stages.keySet().stream().sorted(Comparator.comparing(s -> s.getStageName().getString()))
            .filter(s -> s.getStageName().getString().toLowerCase().contains(searchBar.getValue().toLowerCase()))
            .map(stages::get).toList();

        buttonListSize = stageButtons.size() + 1;

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        buttons.forEach(b -> b.visible = false);

        for (int i = 0; i < Math.min(8, stageButtons.size() + 1); i++)
        {
            MenuButton stageButton;
            SuperJumpMenuButton jumpButton = null;

            int index = (int) (scroll * Math.max(0, stageButtons.size() - 7) + i);

            if (index == 0)
                stageButton = createStageButton;
            else
            {
                index--;
                if (index >= stageButtons.size()) break;
                stageButton = stageButtons.get(index).getFirst();
                jumpButton = stageButtons.get(index).getSecond();
            }

            stageButton.relativeY = (i + 2) * 12;
            stageButton.visible = true;

            if (jumpButton != null)
            {
                jumpButton.relativeY = (i + 2) * 12;
                jumpButton.active = jumpButton.getState().valid;
                jumpButton.visible = true;
            }
        }

        toggleSearchBarButton.visible = true;
    }

    @Override
    public boolean canClickButtons()
    {
        return !scrollBarHeld;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int clickButton)
    {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (buttonListSize > 7 && isMouseOver(mouseX, mouseY, x + 190, y + 24, x + 200, y + 120))
            scrollBarHeld = true;

        return super.mouseClicked(mouseX, mouseY, clickButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int clickButton)
    {
        scrollBarHeld = false;
        return super.mouseReleased(mouseX, mouseY, clickButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double p_94702_, double p_94703_)
    {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (scrollBarHeld)
            scroll = MathHelper.clamp((mouseY - (y + 24)) / 96f, 0, 1);

        return super.mouseDragged(mouseX, mouseY, mouseButton, p_94702_, p_94703_);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount)
    {
        if (stages.size() > 7)
            scroll = MathHelper.clamp(scroll - Math.signum(amount) / (buttonListSize - 7), 0.0f, 1.0f);

        return true;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics)
    {
        if (!searchBar.getValue().equals(prevSearchBarText))
        {
            scroll = 0;
            prevSearchBarText = searchBar.getValue();
        }

        super.renderBackground(guiGraphics);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, TEXTURES);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURES, x, y, 0, 0, imageWidth, imageHeight);

        if (searchBar.visible)
            guiGraphics.blit(TEXTURES, x + 10, y + 12, 0, 244, 178, 12);
        else
        {
            Component label = Component.translatable("gui.stage_pad.label.stage_select");
            guiGraphics.drawString(font, label, x + 105 - font.width(label) / 2, y + 14, 0xFFFFFF);
        }

        RenderSystem.setShaderTexture(0, WIDGETS);
        guiGraphics.blit(WIDGETS, x + 188, y + 24 + (int) (scroll * 81), 196 + (buttonListSize > 7 ? (scrollBarHeld ? 2 : 1) * 12 : 0), 0, 12, 15);
    }

    public static class ToggleMenuButton extends MenuButton
    {
        boolean toggle;
        boolean renderBackground = true;

        public ToggleMenuButton(int x, int y, int width, OnPress onPress, OnTooltip onTooltip, PostDraw draw, ButtonColor color, boolean defaultState)
        {
            super(x, y, width, onPress, onTooltip, draw, color);
            toggle = defaultState;
        }

        public ToggleMenuButton(int x, int y, int width, int height, OnPress onPress, OnTooltip onTooltip, PostDraw draw, ButtonColor color, boolean defaultState)
        {
            super(x, y, width, height, onPress, onTooltip, draw, color);
            toggle = defaultState;
        }

        @Override
        public void onPress()
        {
            toggle = !toggle;
            super.onPress();
        }

        public ToggleMenuButton setRenderBackground(boolean v)
        {
            renderBackground = v;
            return this;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
        {
            if (!visible)
                return;

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, WIDGETS);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();

            if (renderBackground)
            {
                guiGraphics.blit(WIDGETS, this.getX(), this.relativeY, 0, getColor().ordinal() * 36, this.width / 2, this.height);
                guiGraphics.blit(WIDGETS, this.getX() + this.width / 2, this.getY(), 180 - this.width / 2, getColor().ordinal() * 36, this.width / 2, this.height);
            }

            if (active)
            {
                int i = this.getYImage(this.isHoveredOrFocused());
                guiGraphics.blit(WIDGETS, this.getX() + (toggle ? width / 2 : 0), this.getY(), 0, getColor().ordinal() * 36 + i * 12, this.width / 4, this.height);
                guiGraphics.blit(WIDGETS, this.getX() + (toggle ? width / 2 : 0) + this.width / 4, this.getY(), 180 - this.width / 4, getColor().ordinal() * 36 + i * 12, this.width / 2, this.height);
            }
            draw.apply(guiGraphics, this);
        }
    }

    public static class HiddenButton extends MenuButton
    {
        public HiddenButton(int x, int y, int width, int height, OnPress onPress, OnTooltip onTooltip, PostDraw draw)
        {
            super(x, y, width, height, onPress, onTooltip, draw, ButtonColor.GREEN);
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
        {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, WIDGETS);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();

            draw.apply(guiGraphics, this);
        }
    }

    public static class SuperJumpMenuButton extends MenuButton
    {
        public static boolean loading = false;
        final Stage stage;
        private ButtonState state = ButtonState.REQUESTING;

        public SuperJumpMenuButton(int x, int y, int width, OnTooltip onTooltip, PostDraw draw, Stage stage)
        {
            super(x, y, width, (b) ->
            {
                SuperJumpMenuButton jumpButton = ((SuperJumpMenuButton) b);

                if (Objects.requireNonNull(jumpButton.getState()) == ButtonState.REQUIRES_UPDATE)
                {
                    jumpButton.state = ButtonState.REQUESTING;
                    loading = true;
                    SplatcraftPacketHandler.sendToServer(new RequestUpdateStageSpawnPadsPacket(jumpButton.stage));
                }
                else
                {
                    SplatcraftPacketHandler.sendToServer(new SuperJumpToStagePacket(stage.id));
                    Minecraft.getInstance().setScreen(null);
                }
            }, onTooltip, draw, ButtonColor.CYAN);

            this.stage = stage;
        }

        public ButtonState getState()
        {
            return loading ? ButtonState.REQUESTING : state;
        }

        @Override
        public ButtonColor getColor()
        {
            return state.color;
        }

        public enum ButtonState
        {
            REQUESTING(false, ButtonColor.YELLOW, Component.translatable("gui.stage_pad.button.superjump_to.requesting")),
            OUT_OF_RANGE(false, ButtonColor.RED, Component.translatable("gui.stage_pad.button.superjump_to.out_of_range").withStyle(ChatFormatting.RED)),
            NO_SPAWN_PADS(false, ButtonColor.RED, Component.translatable("gui.stage_pad.button.superjump_to.no_pads_found").withStyle(ChatFormatting.RED)),
            VALID(true, ButtonColor.CYAN, Component.translatable("gui.stage_pad.button.superjump_to")),
            REQUIRES_UPDATE(true, ButtonColor.YELLOW, Component.translatable("gui.stage_pad.button.superjump_to.requires_update").withStyle(ChatFormatting.YELLOW));
            final boolean valid;
            final Component tooltipText;
            final ButtonColor color;

            ButtonState(boolean valid, ButtonColor color, Component tooltipText)
            {
                this.valid = valid;
                this.tooltipText = tooltipText;
                this.color = color;
            }
        }
    }
}