package net.splatcraft.client.gui.stagepad;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.Stage;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.CreateOrEditStagePacket;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import static net.splatcraft.client.gui.stagepad.StageCreationScreen.getShortenedInt;

public class StageSettingsScreen extends AbstractStagePadScreen
{
    private static final Identifier TEXTURES = Splatcraft.identifierOf("textures/gui/stage_pad/stage_settings.png");
    private static Boolean setCorner1 = null;
    private Stage stage;
    private MenuTextBox stageName;

    public StageSettingsScreen(Text label, String stageId, Screen mainMenu)
    {
        super(label);
        stage = Stage.getStage(MinecraftClient.getInstance().world, stageId);
        useAction = (world, player, hand, stack, pos) ->
        {
            if (setCorner1 != null && pos != null)
            {
                if (setCorner1)
                {
                    stage.cornerA = pos;
                    if (!stage.dimID.equals(world.getDimension().effects()))
                    {
                        stage.cornerB = null;
                        stage.dimID = world.getDimension().effects();
                    }
                }
                else
                {
                    stage.cornerB = pos;
                    if (!stage.dimID.equals(world.getDimension().effects()))
                    {
                        stage.cornerA = null;
                        stage.dimID = world.getDimension().effects();
                    }
                }

                SplatcraftPacketHandler.sendToServer(new CreateOrEditStagePacket(stageId, stage.getStageName(), stage.cornerA, stage.cornerB, stage.dimID));
            }

            MinecraftClient.getInstance().setScreen(this);
            setCorner1 = null;
        };

        addOptionsTabs(label, stageId, mainMenu);

        addButton(new MenuButton(167, 70, 30, 12, (b) -> clickSetCornerButton(b, true),
            showText(Text.translatable("gui.stage_pad.button.set_from_world"), Text.translatable("gui.stage_pad.button.set_from_clipboard").formatted(Formatting.YELLOW)), drawText(Text.translatable("gui.stage_pad.button.set_corner"), true), MenuButton.ButtonColor.GREEN));
        addButton(new MenuButton(167, 88, 30, 12, (b) -> clickSetCornerButton(b, false),
            showText(Text.translatable("gui.stage_pad.button.set_from_world"), Text.translatable("gui.stage_pad.button.set_from_clipboard").formatted(Formatting.YELLOW)), drawText(Text.translatable("gui.stage_pad.button.set_corner"), true), MenuButton.ButtonColor.GREEN));

        addButton(new StageSelectionScreen.HiddenButton(62, 69, 102, 14, copyPos(() -> stage.cornerA), showCopyPos(() -> stage.cornerA), (ps, b) ->
        {
        }));
        addButton(new StageSelectionScreen.HiddenButton(62, 87, 102, 14, copyPos(() -> stage.cornerB), showCopyPos(() -> stage.cornerB), (ps, b) ->
        {
        }));

        addTextBox((textRenderer) ->
        {
            stageName = new MenuTextBox(textRenderer, 17, 40, 178, 12, Text.translatable("gui.stage_pad.label.set_stage_name.textbox"), false);
            stageName.setText(stage.getStageName().getString());
            stageName.setFocused(true);
            return stageName;
        });
    }

    @Override
    public void onStagesUpdate()
    {
        stage = Stage.getStage(MinecraftClient.getInstance().world, stage.id);
    }

    protected void clickSetCornerButton(ButtonWidget button, boolean isCorner1)
    {
        if (hasShiftDown())
        {
            String[] coords = client.keyboard.getClipboard().replaceAll(",+\\s+|\\s+|,", " ").replaceAll("[^\\.\\d\\s-]", "").split(" ");

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
            client.setScreen(null);
            client.player.sendMessage(Text.translatable("status.stage_pad.set_corner." + (isCorner1 ? 'a' : 'b')), true);
        }
    }

    @Override
    public void close()
    {
        saveChanges();
        super.close();
    }

    @Override
    public void removed()
    {
        saveChanges();
        super.removed();
    }

    private void saveChanges()
    {
        if (!stage.getStageName().toString().equals(stageName.getText()))
            SplatcraftPacketHandler.sendToServer(new CreateOrEditStagePacket(stage.id, Text.literal(stageName.getText()), stage.cornerA, stage.cornerB, stage.dimID));
    }

    @Override
    public void handleWidgets(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
    }

    @Override
    public void renderBackground(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float delta)
    {
        super.renderBackground(guiGraphics, mouseX, mouseY, delta);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, TEXTURES);

        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        guiGraphics.drawTexture(TEXTURES, x, y, 0, 0, backgroundWidth, backgroundHeight);

        guiGraphics.drawTextWithShadow(textRenderer, Text.translatable("gui.stage_pad.label.set_stage_name"), x + 14, y + 28, 0xFFFFFF);
        guiGraphics.drawTextWithShadow(textRenderer, Text.translatable("gui.stage_pad.label.stage_id", stage.id), x + 14, y + 55, 0x808080);

        Text label = Text.translatable("gui.stage_pad.label.corner_1");
        guiGraphics.drawTextWithShadow(textRenderer, label, x + 60 - textRenderer.getWidth(label), y + 72, 0xFFFFFF);
        label = Text.translatable("gui.stage_pad.label.corner_2");
        guiGraphics.drawTextWithShadow(textRenderer, label, x + 60 - textRenderer.getWidth(label), y + 90, 0xFFFFFF);

        BlockPos corner1 = stage.cornerA;
        BlockPos corner2 = stage.cornerB;

        if (corner1 != null)
        {
            guiGraphics.drawTextWithShadow(textRenderer, getShortenedInt(corner1.getX()), x + 64, y + 73, 0xFFFFFF);
            guiGraphics.drawTextWithShadow(textRenderer, getShortenedInt(corner1.getY()), x + 98, y + 73, 0xFFFFFF);
            guiGraphics.drawTextWithShadow(textRenderer, getShortenedInt(corner1.getZ()), x + 132, y + 73, 0xFFFFFF);
        }
        if (corner2 != null)
        {
            guiGraphics.drawTextWithShadow(textRenderer, getShortenedInt(corner2.getX()), x + 64, y + 91, 0xFFFFFF);
            guiGraphics.drawTextWithShadow(textRenderer, getShortenedInt(corner2.getY()), x + 98, y + 91, 0xFFFFFF);
            guiGraphics.drawTextWithShadow(textRenderer, getShortenedInt(corner2.getZ()), x + 132, y + 91, 0xFFFFFF);
        }
    }
}