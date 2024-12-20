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
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.CreateOrEditStagePacket;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class StageCreationScreen extends AbstractStagePadScreen
{
    private static final Identifier TEXTURES = Splatcraft.identifierOf("textures/gui/stage_pad/stage_create.png");
    public static BlockPos corner1;
    public static BlockPos corner2;
    public static Identifier dimension;
    static String savedName = "";
    @Nullable
    //null when stage creation screen was closed via escape key, opens creation menu back up without setting a corner pos
    static Boolean setCorner1 = null;
    private final MenuButton createButton;
    protected String stageId = "";
    private MenuTextBox stageName;
    private boolean pendingCreation = false;

    public StageCreationScreen(Text label, @Nullable Screen parent, String savedStageName, @Nullable BlockPos cornerA, @Nullable BlockPos cornerB)
    {
        super(label, ((level, player, hand, stack, pos) ->
        {
            if (setCorner1 != null && pos != null)
            {
                if (setCorner1)
                {
                    corner1 = pos;
                    if (!level.getDimension().effects().equals(dimension))
                    {
                        dimension = level.getDimension().effects();
                        corner2 = null;
                    }
                }
                else
                {
                    corner2 = pos;
                    if (!level.getDimension().effects().equals(dimension))
                    {
                        dimension = level.getDimension().effects();
                        corner1 = null;
                    }
                }
            }

            MinecraftClient.getInstance().setScreen(new StageCreationScreen(stack.getName(), parent));
            setCorner1 = null;
        }));

        addButton(new MenuButton(51, 107, 50, 12, goToScreen(() -> parent),
            MenuButton.NO_TOOLTIP, drawText(Text.translatable("gui.stage_pad.button.cancel"), true), MenuButton.ButtonColor.RED));
        addButton(new MenuButton(167, 70, 30, 12, (b) -> clickSetCornerButton(b, true),
            showText(Text.translatable("gui.stage_pad.button.set_from_world"), Text.translatable("gui.stage_pad.button.set_from_clipboard").formatted(Formatting.YELLOW)), drawText(Text.translatable("gui.stage_pad.button.set_corner"), true), MenuButton.ButtonColor.GREEN));
        addButton(new MenuButton(167, 88, 30, 12, (b) -> clickSetCornerButton(b, false),
            showText(Text.translatable("gui.stage_pad.button.set_from_world"), Text.translatable("gui.stage_pad.button.set_from_clipboard").formatted(Formatting.YELLOW)), drawText(Text.translatable("gui.stage_pad.button.set_corner"), true), MenuButton.ButtonColor.GREEN));
        createButton = addButton(new MenuButton(107, 107, 50, 12, (b) ->
        {
            if (canCreate())
            {
                SplatcraftPacketHandler.sendToServer(new CreateOrEditStagePacket(stageId, Text.literal(stageName.getText()), corner1, corner2, dimension));

                buttons.forEach(button -> button.active = false);
                stageName.setFocused(false);
                pendingCreation = true;
            }
        }, MenuButton.NO_TOOLTIP, drawText(Text.translatable("gui.stage_pad.button.create"), true), MenuButton.ButtonColor.LIME));
        addButton(new StageSelectionScreen.HiddenButton(62, 69, 102, 14, copyPos(() -> corner1), showCopyPos(() -> corner1), (ps, b) ->
        {
        }));
        addButton(new StageSelectionScreen.HiddenButton(62, 87, 102, 14, copyPos(() -> corner2), showCopyPos(() -> corner2), (ps, b) ->
        {
        }));
        addTextBox(textRenderer ->
        {
            stageName = new MenuTextBox(textRenderer, 17, 40, 178, 12, Text.translatable("gui.stage_pad.label.set_stage_name.textbox"), false);
            stageName.setText(savedStageName);
            stageName.setFocused(true);
            return stageName;
        });

        corner1 = cornerA;
        corner2 = cornerB;
    }

    public StageCreationScreen(Text label, @Nullable Screen parent)
    {
        this(label, parent, savedName, corner1, corner2);
    }

    protected static String getShortenedInt(int v)
    {
        if (Integer.toString(v).length() > 5)
            for (NumberLetters nl : NumberLetters.values())
            {
                if (nl.minValue <= Math.abs(v))
                    return Integer.toString((int) (v / nl.minValue)) + nl.letter;
            }

        return String.valueOf(v);
    }

    protected void clickSetCornerButton(ButtonWidget button, boolean isCorner1)
    {
        if (hasShiftDown())
        {
            String[] coords = client.keyboard.getClipboard().replaceAll(",+\\s+|\\s+|,", " ").replaceAll("[^\\.\\d\\s-]", "").split(" ");
            BlockPos pos = null;

            if (coords.length >= 3)
                pos = CommonUtils.createBlockPos(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]), Double.parseDouble(coords[2]));

            if (isCorner1)
                corner1 = pos;
            else
                corner2 = pos;
        }
        else
        {
            setCorner1 = isCorner1;
            client.setScreen(null);
            client.player.sendMessage(Text.translatable("status.stage_pad.set_corner." + (isCorner1 ? 'a' : 'b')), true);
        }
    }

    @Override
    protected void init()
    {
        super.init();
        updateId();
    }

    @Override
    public void handleWidgets(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        if (pendingCreation)
            return;

        if (!savedName.equals(stageName.getText()))
        {
            savedName = stageName.getText();
            updateId();
        }

        createButton.active = canCreate();
    }

    private void updateId()
    {
        String savedId = stageName.getText().replace(' ', '_');
        String newId = savedId;

        if (client.world != null && !newId.isEmpty())
        {
            Map<String, Stage> stages = SaveInfoCapability.get().getStages(); //this is null in servers somehow??
            for (int i = 1; stages.containsKey(newId); i++)
                newId = savedId + "_" + i;
        }
        else
            newId = "";

        stageId = newId;
    }

    private boolean canCreate()
    {
        return !stageId.isEmpty() && corner1 != null && corner2 != null;
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

        Text label = Text.translatable("gui.stage_pad.label.create_stage");
        guiGraphics.drawTextWithShadow(textRenderer, label, x + 105 - textRenderer.getWidth(label) / 2, y + 14, 0xFFFFFF);
        guiGraphics.drawTextWithShadow(textRenderer, Text.translatable("gui.stage_pad.label.set_stage_name"), x + 14, y + 28, 0xFFFFFF);
        guiGraphics.drawTextWithShadow(textRenderer, Text.translatable("gui.stage_pad.label.stage_id", stageId), x + 14, y + 55, 0x808080);

        label = Text.translatable("gui.stage_pad.label.corner_1");
        guiGraphics.drawTextWithShadow(textRenderer, label, x + 60 - textRenderer.getWidth(label), y + 72, 0xFFFFFF);

        label = Text.translatable("gui.stage_pad.label.corner_2");
        guiGraphics.drawTextWithShadow(textRenderer, label, x + 60 - textRenderer.getWidth(label), y + 90, 0xFFFFFF);

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

    @Override
    public void onStagesUpdate()
    {
        updateId();
    }

    public String getStageId()
    {
        return stageId;
    }

    enum NumberLetters
    {
        BILLION(10, 'B'),
        MILLION(7, 'M'),
        THOUSAND(4, 'K');
        final int length;
        final double minValue;
        final char letter;

        NumberLetters(int length, char letter)
        {
            this.length = length;
            minValue = Math.pow(10, (length - 1));
            this.letter = letter;
        }
    }
}