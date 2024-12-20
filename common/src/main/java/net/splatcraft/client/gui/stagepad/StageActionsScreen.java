package net.splatcraft.client.gui.stagepad;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.Stage;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.RequestClearInkPacket;
import net.splatcraft.network.c2s.RequestTurfScanPacket;
import org.jetbrains.annotations.NotNull;

public class StageActionsScreen extends AbstractStagePadScreen
{
    private static final Identifier TEXTURES = Splatcraft.identifierOf("textures/gui/stage_pad/stage_actions.png");
    private final StageSelectionScreen.ToggleMenuButton scanMode;
    private Stage stage;

    public StageActionsScreen(Text label, String stageId, Screen mainMenu)
    {
        super(label);
        stage = Stage.getStage(client.world, stageId);

        addOptionsTabs(label, stageId, mainMenu);

        scanMode = addButton(new StageSelectionScreen.ToggleMenuButton(136, 50, 24, 12, (b) ->
        {
        }, (b, ps, mx, my, partialTicks) ->
        {
            showText(Text.translatable("gui.stage_pad.button.scan_mode", Text.translatable("item.splatcraft.turf_scanner.mode." + (((StageSelectionScreen.ToggleMenuButton) b).toggle ? "1" : "0")))).onTooltip(b, ps, mx, my, partialTicks);
        }, drawToggleIcon(WIDGETS, 0, 0, 232, 48, 12, 12, true), MenuButton.ButtonColor.GREEN, false).setRenderBackground(false));
        addButton(new MenuButton(50, 50, 86, 12, (b) ->
        {
            SplatcraftPacketHandler.sendToServer(new RequestTurfScanPacket(stageId, !scanMode.toggle));
            client.setScreen(null);
        }, MenuButton.NO_TOOLTIP, drawText(Text.translatable("gui.stage_pad.button.scan_turf"), true), MenuButton.ButtonColor.GREEN));
        addButton(new MenuButton(50, 64, 110, 12, (b) ->
        {
            SplatcraftPacketHandler.sendToServer(new RequestClearInkPacket(stageId));
            client.setScreen(null);
        }, MenuButton.NO_TOOLTIP, drawText(Text.translatable("gui.stage_pad.button.clear_ink"), true), MenuButton.ButtonColor.GREEN));
        addButton(new MenuButton(50, 78, 110, 12, goToScreen(() -> mainMenu), MenuButton.NO_TOOLTIP, drawText(Text.translatable("gui.stage_pad.button.pair_remote"), true), MenuButton.ButtonColor.GREEN));
    }

    @Override
    public void onStagesUpdate()
    {
        stage = Stage.getStage(client.world, stage.id);
    }

    @Override
    public void handleWidgets(DrawContext graphics, int mouseX, int mouseY, float partialTicks)
    {
    }

    @Override
    public void renderBackground(@NotNull DrawContext graphics, int mouseX, int mouseY, float delta)
    {
        super.renderBackground(graphics, mouseX, mouseY, delta);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, TEXTURES);

        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        graphics.drawTexture(TEXTURES, x, y, 0, 0, backgroundWidth, backgroundHeight);
    }
}