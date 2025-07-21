package net.splatcraft.client.gui.stagepad;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.Stage;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.CreateOrEditStagePacket;
import org.jetbrains.annotations.NotNull;

import static net.splatcraft.client.gui.stagepad.StageCreationScreen.getShortenedInt;

public class StageSettingsScreen extends AbstractStagePadScreen
{
	private static final ResourceLocation TEXTURES = Splatcraft.identifierOf("textures/gui/stage_pad/stage_settings.png");
	private static Boolean setCorner1 = null;
	private Stage stage;
	private MenuTextBox stageName;
	public StageSettingsScreen(Component label, String stageId, Screen mainMenu)
	{
		super(label);
		stage = Stage.getStage(stageId);
		useAction = (world, player, hand, stack, pos) ->
		{
			if (setCorner1 != null && pos != null)
			{
				if (setCorner1)
				{
					stage.minCorner = pos;
					if (!stage.worldKey.equals(world.dimension()))
					{
						stage.maxCorner = null;
						stage.worldKey = world.dimension();
					}
				}
				else
				{
					stage.maxCorner = pos;
					if (!stage.worldKey.equals(world.dimension()))
					{
						stage.minCorner = null;
						stage.worldKey = world.dimension();
					}
				}
				
				SplatcraftPacketHandler.sendToServer(new CreateOrEditStagePacket(stageId, stage.getStageName(), stage.getMinCorner(), stage.getMaxCorner(), stage.worldKey));
			}
			
			Minecraft.getInstance().setScreen(this);
			buttons.forEach(this::addWidget);
			textFields.forEach(this::addWidget);
			
			setCorner1 = null;
		};
		
		addOptionsTabs(label, stageId, mainMenu);
		
		addButton(new MenuButton(167, 70, 30, 12, (b) -> clickSetCornerButton(b, true),
			showText(Component.translatable("gui.stage_pad.button.set_from_world"), Component.translatable("gui.stage_pad.button.set_from_clipboard").withStyle(ChatFormatting.YELLOW)), drawText(Component.translatable("gui.stage_pad.button.set_corner"), true), MenuButton.ButtonColor.GREEN));
		addButton(new MenuButton(167, 88, 30, 12, (b) -> clickSetCornerButton(b, false),
			showText(Component.translatable("gui.stage_pad.button.set_from_world"), Component.translatable("gui.stage_pad.button.set_from_clipboard").withStyle(ChatFormatting.YELLOW)), drawText(Component.translatable("gui.stage_pad.button.set_corner"), true), MenuButton.ButtonColor.GREEN));
		
		addButton(new StageSelectionScreen.HiddenButton(62, 69, 102, 14, copyPos(() -> stage.minCorner), showCopyPos(() -> stage.minCorner), (ps, b) ->
		{
		}));
		addButton(new StageSelectionScreen.HiddenButton(62, 87, 102, 14, copyPos(() -> stage.maxCorner), showCopyPos(() -> stage.maxCorner), (ps, b) ->
		{
		}));
		
		addTextBox((textRenderer) ->
		{
			stageName = new MenuTextBox(textRenderer, 17, 40, 178, 12, Component.translatable("gui.stage_pad.label.set_stage_name.textbox"), false);
			stageName.setValue(stage.getStageName().getString());
			stageName.setFocused(true);
			return stageName;
		});
	}
	@Override
	public void onStagesUpdate()
	{
		stage = Stage.getStage(stage.id);
	}
	protected void clickSetCornerButton(Button button, boolean isCorner1)
	{
		if (hasShiftDown())
		{
			String[] coords = minecraft.keyboardHandler.getClipboard().replaceAll(",+\\s+|\\s+|,", " ").replaceAll("[^\\.\\d\\s-]", "").split(" ");
			
			if (coords.length >= 3)
			{
				BlockPos pos = BlockPos.containing(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]), Double.parseDouble(coords[2]));
				if (isCorner1)
					stage.minCorner = pos;
				else
					stage.maxCorner = pos;
				SplatcraftPacketHandler.sendToServer(new CreateOrEditStagePacket(stage.id, stage.getStageName(), stage.minCorner, stage.maxCorner, stage.worldKey));
			}
		}
		else
		{
			setCorner1 = isCorner1;
			minecraft.setScreen(null);
			minecraft.player.displayClientMessage(Component.translatable("status.stage_pad.set_corner." + (isCorner1 ? 'a' : 'b')), true);
		}
	}
	@Override
	public void onClose()
	{
		saveChanges();
		super.onClose();
	}
	@Override
	public void removed()
	{
		saveChanges();
		super.removed();
	}
	private void saveChanges()
	{
		if (!stage.getStageName().toString().equals(stageName.getValue()))
			SplatcraftPacketHandler.sendToServer(new CreateOrEditStagePacket(stage.id, Component.literal(stageName.getValue()), stage.minCorner, stage.maxCorner, stage.worldKey));
	}
	@Override
	public void handleWidgets(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
	{
	}
	@Override
	public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta)
	{
		super.renderBackground(guiGraphics, mouseX, mouseY, delta);
		
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.setShaderTexture(0, TEXTURES);
		
		int x = (width - backgroundWidth) / 2;
		int y = (height - backgroundHeight) / 2;
		
		guiGraphics.blit(TEXTURES, x, y, 0, 0, backgroundWidth, backgroundHeight);
		
		guiGraphics.drawString(font, Component.translatable("gui.stage_pad.label.set_stage_name"), x + 14, y + 28, 0xFFFFFF);
		guiGraphics.drawString(font, Component.translatable("gui.stage_pad.label.stage_id", stage.id), x + 14, y + 55, 0x808080);
		
		Component label = Component.translatable("gui.stage_pad.label.corner_1");
		guiGraphics.drawString(font, label, x + 60 - font.width(label), y + 72, 0xFFFFFF);
		label = Component.translatable("gui.stage_pad.label.corner_2");
		guiGraphics.drawString(font, label, x + 60 - font.width(label), y + 90, 0xFFFFFF);
		
		BlockPos corner1 = stage.minCorner;
		BlockPos corner2 = stage.maxCorner;
		
		if (corner1 != null)
		{
			guiGraphics.drawString(font, getShortenedInt(corner1.getX()), x + 64, y + 73, 0xFFFFFF);
			guiGraphics.drawString(font, getShortenedInt(corner1.getY()), x + 98, y + 73, 0xFFFFFF);
			guiGraphics.drawString(font, getShortenedInt(corner1.getZ()), x + 132, y + 73, 0xFFFFFF);
		}
		if (corner2 != null)
		{
			guiGraphics.drawString(font, getShortenedInt(corner2.getX()), x + 64, y + 91, 0xFFFFFF);
			guiGraphics.drawString(font, getShortenedInt(corner2.getY()), x + 98, y + 91, 0xFFFFFF);
			guiGraphics.drawString(font, getShortenedInt(corner2.getZ()), x + 132, y + 91, 0xFFFFFF);
		}
	}
}