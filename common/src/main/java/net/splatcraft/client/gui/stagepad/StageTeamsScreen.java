package net.splatcraft.client.gui.stagepad;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.Stage;

public class StageTeamsScreen extends AbstractStagePadScreen
{
    private static final Identifier TEXTURES = Splatcraft.identifierOf("textures/gui/stage_pad/stage_teams.png");
    private Stage stage;

    public StageTeamsScreen(Text label, String stageId, Screen mainMenu)
    {
        super(label);
        stage = Stage.getStage(MinecraftClient.getInstance().world, stageId);
        addOptionsTabs(label, stageId, mainMenu);
    }

    @Override
    public void handleWidgets(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
    }

    @Override
    public void onStagesUpdate()
    {
        stage = Stage.getStage(MinecraftClient.getInstance().world, stage.id);
    }
}
