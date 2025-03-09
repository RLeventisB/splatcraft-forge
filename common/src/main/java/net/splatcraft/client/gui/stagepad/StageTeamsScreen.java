package net.splatcraft.client.gui.stagepad;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.Stage;

public class StageTeamsScreen extends AbstractStagePadScreen
{
	private static final ResourceLocation TEXTURES = Splatcraft.identifierOf("textures/gui/stage_pad/stage_teams.png");
	private Stage stage;
	public StageTeamsScreen(Component label, String stageId, Screen mainMenu)
	{
		super(label);
		stage = Stage.getStage(stageId);
		addOptionsTabs(label, stageId, mainMenu);
	}
	@Override
	public void handleWidgets(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
	{
	}
	@Override
	public void onStagesUpdate()
	{
		stage = Stage.getStage(stage.id);
	}
}
