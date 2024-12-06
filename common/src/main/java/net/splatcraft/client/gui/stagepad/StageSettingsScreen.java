package net.splatcraft.forge.client.gui.stagepad;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.data.Stage;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.network.c2s.CreateOrEditStagePacket;
import net.splatcraft.forge.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import static net.splatcraft.forge.client.gui.stagepad.StageCreationScreen.getShortenedInt;

public class StageSettingsScreen extends AbstractStagePadScreen
{
    private static final ResourceLocation TEXTURES = new ResourceLocation(Splatcraft.MODID, "textures/gui/stage_pad/stage_settings.png");
    private static Boolean setCorner1 = null;
    private Stage stage;
    private MenuTextBox stageName;

    public StageSettingsScreen(Component label, String stageId, Screen mainMenu)
    {
        super(label);
        stage = Stage.getStage(Minecraft.getInstance().level, stageId);
        useAction = (level, player, hand, stack, pos) ->
        {
            if (setCorner1 != null && pos != null)
            {
                if (setCorner1)
                {
                    stage.cornerA = pos;
                    if (!stage.dimID.equals(level.dimension().location()))
                    {
                        stage.cornerB = null;
                        stage.dimID = level.dimension().location();
                    }
                }
                else
                {
                    stage.cornerB = pos;
                    if (!stage.dimID.equals(level.dimension().location()))
                    {
                        stage.cornerA = null;
                        stage.dimID = level.dimension().location();
                    }
                }

                SplatcraftPacketHandler.sendToServer(new CreateOrEditStagePacket(stageId, stage.getStageName(), stage.cornerA, stage.cornerB, stage.dimID));
            }

            Minecraft.getInstance().setScreen(this);
            setCorner1 = null;
        };

        addOptionsTabs(label, stageId, mainMenu);

        addButton(new MenuButton(167, 70, 30, 12, (b) -> clickSetCornerButton(b, true),
            showText(Component.translatable("gui.stage_pad.button.set_from_world"), Component.translatable("gui.stage_pad.button.set_from_clipboard").withStyle(ChatFormatting.YELLOW)), drawText(Component.translatable("gui.stage_pad.button.set_corner"), true), MenuButton.ButtonColor.GREEN));
        addButton(new MenuButton(167, 88, 30, 12, (b) -> clickSetCornerButton(b, false),
            showText(Component.translatable("gui.stage_pad.button.set_from_world"), Component.translatable("gui.stage_pad.button.set_from_clipboard").withStyle(ChatFormatting.YELLOW)), drawText(Component.translatable("gui.stage_pad.button.set_corner"), true), MenuButton.ButtonColor.GREEN));

        addButton(new StageSelectionScreen.HiddenButton(62, 69, 102, 14, copyPos(() -> stage.cornerA), showCopyPos(() -> stage.cornerA), (ps, b) -> {
        }));
        addButton(new StageSelectionScreen.HiddenButton(62, 87, 102, 14, copyPos(() -> stage.cornerB), showCopyPos(() -> stage.cornerB), (ps, b) -> {
        }));

        addTextBox((font) ->
        {
            this.stageName = new MenuTextBox(font, 17, 40, 178, 12, Component.translatable("gui.stage_pad.label.set_stage_name.textbox"), false);
            this.stageName.setValue(stage.getStageName().getString());
            this.stageName.setFocused(true);
            return this.stageName;
        });
    }

    @Override
    public void onStagesUpdate()
    {
        stage = Stage.getStage(Minecraft.getInstance().level, stage.id);
    }

    protected void clickSetCornerButton(Button button, boolean isCorner1)
    {
        if (hasShiftDown())
        {
            String[] coords = getMinecraft().keyboardHandler.getClipboard().replaceAll(",+\\s+|\\s+|,", " ").replaceAll("[^\\.\\d\\s-]", "").split(" ");

            if (coords.length >= 3)
            {
                BlockPos pos = CommonUtils.createBlockPos(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]), Double.parseDouble(coords[2]));
                if (isCorner1)
                    stage.cornerA = pos;
                else
                    stage.cornerB = pos;
                SplatcraftPacketHandler.sendToServer(new CreateOrEditStagePacket(stage.id, stage.getStageName(), stage.cornerA, stage.cornerB, stage.dimID));
            }
        }
        else
        {
            setCorner1 = isCorner1;
            minecraft.setScreen(null);
            getMinecraft().player.displayClientMessage(Component.translatable("status.stage_pad.set_corner." + (isCorner1 ? 'a' : 'b')), true);
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
            SplatcraftPacketHandler.sendToServer(new CreateOrEditStagePacket(stage.id, Component.literal(stageName.getValue()), stage.cornerA, stage.cornerB, stage.dimID));
    }

    @Override
    public void handleWidgets(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics)
    {
        super.renderBackground(guiGraphics);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, TEXTURES);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURES, x, y, 0, 0, imageWidth, imageHeight);

        guiGraphics.drawString(font, Component.translatable("gui.stage_pad.label.set_stage_name"), x + 14, y + 28, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("gui.stage_pad.label.stage_id", stage.id), x + 14, y + 55, 0x808080);

        Component label = Component.translatable("gui.stage_pad.label.corner_1");
        guiGraphics.drawString(font, label, x + 60 - font.width(label), y + 72, 0xFFFFFF);
        label = Component.translatable("gui.stage_pad.label.corner_2");
        guiGraphics.drawString(font, label, x + 60 - font.width(label), y + 90, 0xFFFFFF);

        BlockPos corner1 = stage.cornerA;
        BlockPos corner2 = stage.cornerB;

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