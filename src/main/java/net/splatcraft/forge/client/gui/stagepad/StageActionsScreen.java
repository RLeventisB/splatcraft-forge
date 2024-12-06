package net.splatcraft.forge.client.gui.stagepad;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.data.Stage;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.network.c2s.RequestClearInkPacket;
import net.splatcraft.forge.network.c2s.RequestTurfScanPacket;
import org.jetbrains.annotations.NotNull;

public class StageActionsScreen extends AbstractStagePadScreen
{
    private static final ResourceLocation TEXTURES = new ResourceLocation(Splatcraft.MODID, "textures/gui/stage_pad/stage_actions.png");
    private final StageSelectionScreen.ToggleMenuButton scanMode;
    private Stage stage;

    public StageActionsScreen(Component label, String stageId, Screen mainMenu)
    {
        super(label);
        stage = Stage.getStage(getMinecraft().level, stageId);

        addOptionsTabs(label, stageId, mainMenu);

        scanMode = addButton(new StageSelectionScreen.ToggleMenuButton(136, 50, 24, 12, (b) -> {
        }, (b, ps, mx, my, partialTicks) ->
        {
            showText(Component.translatable("gui.stage_pad.button.scan_mode", Component.translatable("item.splatcraft.turf_scanner.mode." + (((StageSelectionScreen.ToggleMenuButton) b).toggle ? "1" : "0")))).onTooltip(b, ps, mx, my, partialTicks);
        }, drawToggleIcon(WIDGETS, 0, 0, 232, 48, 12, 12, true), MenuButton.ButtonColor.GREEN, false).setRenderBackground(false));
        addButton(new MenuButton(50, 50, 86, 12, (b) ->
        {
            SplatcraftPacketHandler.sendToServer(new RequestTurfScanPacket(stageId, !scanMode.toggle));
            getMinecraft().setScreen(null);
        }, MenuButton.NO_TOOLTIP, drawText(Component.translatable("gui.stage_pad.button.scan_turf"), true), MenuButton.ButtonColor.GREEN));
        addButton(new MenuButton(50, 64, 110, 12, (b) ->
        {
            SplatcraftPacketHandler.sendToServer(new RequestClearInkPacket(stageId));
            getMinecraft().setScreen(null);
        }, MenuButton.NO_TOOLTIP, drawText(Component.translatable("gui.stage_pad.button.clear_ink"), true), MenuButton.ButtonColor.GREEN));
        addButton(new MenuButton(50, 78, 110, 12, goToScreen(() -> mainMenu), MenuButton.NO_TOOLTIP, drawText(Component.translatable("gui.stage_pad.button.pair_remote"), true), MenuButton.ButtonColor.GREEN));
    }

    @Override
    public void onStagesUpdate()
    {
        stage = Stage.getStage(getMinecraft().level, stage.id);
    }

    @Override
    public void handleWidgets(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics)
    {
        super.renderBackground(graphics);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, TEXTURES);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        graphics.blit(TEXTURES, x, y, 0, 0, imageWidth, imageHeight);
    }
}