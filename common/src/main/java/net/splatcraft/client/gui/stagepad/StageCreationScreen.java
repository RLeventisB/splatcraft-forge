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
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.CreateOrEditStagePacket;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class StageCreationScreen extends AbstractStagePadScreen
{
    private static final ResourceLocation TEXTURES = new ResourceLocation(Splatcraft.MODID, "textures/gui/stage_pad/stage_create.png");
    public static BlockPos corner1;
    public static BlockPos corner2;
    public static ResourceLocation dimension;
    static String savedName = "";
    @Nullable
    //null when stage creation screen was closed via escape key, opens creation menu back up without setting a corner pos
    static Boolean setCorner1 = null;
    private final MenuButton createButton;
    protected String stageId = "";
    private MenuTextBox stageName;
    private boolean pendingCreation = false;

    public StageCreationScreen(Component label, @Nullable Screen parent, String savedStageName, @Nullable BlockPos cornerA, @Nullable BlockPos cornerB)
    {
        super(label, ((level, player, hand, stack, pos) ->
        {
            if (setCorner1 != null && pos != null)
            {
                if (setCorner1)
                {
                    corner1 = pos;
                    if (!level.dimension().location().equals(dimension))
                    {
                        dimension = level.dimension().location();
                        corner2 = null;
                    }
                }
                else
                {
                    corner2 = pos;
                    if (!level.dimension().location().equals(dimension))
                    {
                        dimension = level.dimension().location();
                        corner1 = null;
                    }
                }
            }

            Minecraft.getInstance().setScreen(new StageCreationScreen(stack.getDisplayName(), parent));
            setCorner1 = null;
        }));

        addButton(new MenuButton(51, 107, 50, 12, goToScreen(() -> parent),
            MenuButton.NO_TOOLTIP, drawText(Component.translatable("gui.stage_pad.button.cancel"), true), MenuButton.ButtonColor.RED));
        addButton(new MenuButton(167, 70, 30, 12, (b) -> clickSetCornerButton(b, true),
            showText(Component.translatable("gui.stage_pad.button.set_from_world"), Component.translatable("gui.stage_pad.button.set_from_clipboard").withStyle(ChatFormatting.YELLOW)), drawText(Component.translatable("gui.stage_pad.button.set_corner"), true), MenuButton.ButtonColor.GREEN));
        addButton(new MenuButton(167, 88, 30, 12, (b) -> clickSetCornerButton(b, false),
            showText(Component.translatable("gui.stage_pad.button.set_from_world"), Component.translatable("gui.stage_pad.button.set_from_clipboard").withStyle(ChatFormatting.YELLOW)), drawText(Component.translatable("gui.stage_pad.button.set_corner"), true), MenuButton.ButtonColor.GREEN));
        createButton = addButton(new MenuButton(107, 107, 50, 12, (b) ->
        {
            if (canCreate())
            {
                SplatcraftPacketHandler.sendToServer(new CreateOrEditStagePacket(stageId, Component.literal(this.stageName.getValue()), corner1, corner2, dimension));

                buttons.forEach(button -> button.active = false);
                this.stageName.setFocused(false);
                pendingCreation = true;
            }
        }, MenuButton.NO_TOOLTIP, drawText(Component.translatable("gui.stage_pad.button.create"), true), MenuButton.ButtonColor.LIME));
        addButton(new StageSelectionScreen.HiddenButton(62, 69, 102, 14, copyPos(() -> corner1), showCopyPos(() -> corner1), (ps, b) ->
        {
        }));
        addButton(new StageSelectionScreen.HiddenButton(62, 87, 102, 14, copyPos(() -> corner2), showCopyPos(() -> corner2), (ps, b) ->
        {
        }));
        addTextBox(font ->
        {
            this.stageName = new MenuTextBox(font, 17, 40, 178, 12, Component.translatable("gui.stage_pad.label.set_stage_name.textbox"), false);
            this.stageName.setValue(savedStageName);
            this.stageName.setFocused(true);
            return this.stageName;
        });

        corner1 = cornerA;
        corner2 = cornerB;
    }

    public StageCreationScreen(Component label, @Nullable Screen parent)
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

    protected void clickSetCornerButton(Button button, boolean isCorner1)
    {
        if (hasShiftDown())
        {
            String[] coords = getMinecraft().keyboardHandler.getClipboard().replaceAll(",+\\s+|\\s+|,", " ").replaceAll("[^\\.\\d\\s-]", "").split(" ");
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
            minecraft.setScreen(null);
            getMinecraft().player.displayClientMessage(Component.translatable("status.stage_pad.set_corner." + (isCorner1 ? 'a' : 'b')), true);
        }
    }

    @Override
    protected void init()
    {
        super.init();
        updateId();
    }

    @Override
    public void handleWidgets(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        if (pendingCreation)
            return;

        if (!savedName.equals(stageName.getValue()))
        {
            savedName = stageName.getValue();
            updateId();
        }

        createButton.active = canCreate();
    }

    private void updateId()
    {
        String savedId = stageName.getValue().replace(' ', '_');
        String newId = savedId;

        if (getMinecraft().level != null && !newId.isEmpty())
        {
            HashMap<String, Stage> stages = SaveInfoCapability.get(getMinecraft().level.getServer()).getStages(); //this is null in servers somehow??
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
    public void renderBackground(@NotNull GuiGraphics guiGraphics)
    {
        super.renderBackground(guiGraphics);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, TEXTURES);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURES, x, y, 0, 0, imageWidth, imageHeight);

        Component label = Component.translatable("gui.stage_pad.label.create_stage");
        guiGraphics.drawString(font, label, x + 105 - font.width(label) / 2, y + 14, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("gui.stage_pad.label.set_stage_name"), x + 14, y + 28, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("gui.stage_pad.label.stage_id", stageId), x + 14, y + 55, 0x808080);

        label = Component.translatable("gui.stage_pad.label.corner_1");
        guiGraphics.drawString(font, label, x + 60 - font.width(label), y + 72, 0xFFFFFF);

        label = Component.translatable("gui.stage_pad.label.corner_2");
        guiGraphics.drawString(font, label, x + 60 - font.width(label), y + 90, 0xFFFFFF);

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