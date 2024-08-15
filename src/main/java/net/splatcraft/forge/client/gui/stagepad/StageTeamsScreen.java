package net.splatcraft.forge.client.gui.stagepad;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.data.Stage;
import org.jetbrains.annotations.NotNull;

public class StageTeamsScreen extends AbstractStagePadScreen
{
    private static final ResourceLocation TEXTURES = new ResourceLocation(Splatcraft.MODID, "textures/gui/stage_pad/stage_teams.png");
    private Stage stage;

    public StageTeamsScreen(Component label, String stageId, Screen mainMenu)
    {
        super(label);
        stage = Stage.getStage(Minecraft.getInstance().level, stageId);
        addOptionsTabs(label, stageId, mainMenu);
    }

    @Override
    public void handleWidgets(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics)
    {
        super.renderBackground(guiGraphics);
    }

    @Override
    public void onStagesUpdate()
    {
        stage = Stage.getStage(Minecraft.getInstance().level, stage.id);
    }
}
