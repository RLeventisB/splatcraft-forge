package net.splatcraft.client.gui.stagepad;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameRules;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.Stage;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.RequestSetStageRulePacket;
import net.splatcraft.registries.SplatcraftGameRules;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class StageRulesScreen extends AbstractStagePadScreen
{
    private static final Identifier TEXTURES = Splatcraft.identifierOf("textures/gui/stage_pad/stage_rules.png");
    private final HashMap<GameRules.Key<GameRules.BooleanRule>, RuleEntry> rules = new HashMap<>();
    private Stage stage;
    private boolean scrollBarHeld = false;
    private double scroll = 0;

    public StageRulesScreen(Text label, String stageId, Screen mainMenu)
    {
        super(label);
        stage = Stage.getStage(MinecraftClient.getInstance().world, stageId);

        for (GameRules.Key<GameRules.BooleanRule> rule : Stage.VALID_SETTINGS.values())
        {
            rules.put(rule, new RuleEntry(
                addButton(new RuleNameLabel(rule)),
                addButton(new RuleValueButton(rule)), stage.getSetting(rule)));
        }

        addOptionsTabs(label, stageId, mainMenu);
    }

    @Override
    public void onStagesUpdate()
    {
        stage = Stage.getStage(MinecraftClient.getInstance().world, stage.id);
        rules.forEach((gameRule, value) -> value.value = stage.getSetting(gameRule));
    }

    @Override
    public void handleWidgets(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        int index = 0;
        for (RuleEntry rule : rules.values())
        {
            int i = index - (int) (scroll * Math.max(0, rules.size() - 8));
            MenuButton ruleLabel = rule.label;
            MenuButton ruleButton = rule.button;

            if (i >= 8 || i < 0)
            {
                ruleLabel.visible = false;
                ruleButton.visible = false;
            }
            else
            {
                ruleLabel.relativeY = (i + 2) * 12;
                ruleLabel.visible = true;
                ruleButton.relativeY = (i + 2) * 12;
                ruleButton.visible = true;
            }

            index++;
        }
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

        RenderSystem.setShaderTexture(0, WIDGETS);
        guiGraphics.drawTexture(WIDGETS, x + 188, y + 24 + (int) (scroll * 81), 196 + (rules.size() > 8 ? (scrollBarHeld ? 2 : 1) * 12 : 0), 0, 12, 15);
    }

    @Override
    public boolean canClickButtons()
    {
        return !scrollBarHeld;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int clickButton)
    {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        if (rules.size() > 8 && isMouseOver(mouseX, mouseY, x + 190, y + 24, x + 200, y + 120))
            scrollBarHeld = true;

        return super.mouseClicked(mouseX, mouseY, clickButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int clickButton)
    {
        scrollBarHeld = false;
        return super.mouseReleased(mouseX, mouseY, clickButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double p_94702_, double p_94703_)
    {
        int y = (height - backgroundHeight) / 2;

        if (scrollBarHeld)
            scroll = MathHelper.clamp((mouseY - (y + 24)) / 96f, 0, 1);

        return super.mouseDragged(mouseX, mouseY, mouseButton, p_94702_, p_94703_);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)
    {
        if (rules.size() >= 8)
            scroll = MathHelper.clamp(scroll - Math.signum(verticalAmount) / (rules.size() - 8), 0.0f, 1.0f);

        return true;
    }

    class RuleEntry
    {
        public final RuleNameLabel label;
        public final RuleValueButton button;
        public Boolean value;

        RuleEntry(RuleNameLabel label, RuleValueButton button, Boolean value)
        {
            this.label = label;
            this.button = button;
            this.value = value;
        }
    }

    class RuleNameLabel extends MenuButton
    {
        public RuleNameLabel(GameRules.Key<GameRules.BooleanRule> rule)
        {
            super(10, 24, 144, 12, (b) ->
            {
            }, ((button, guiGraphics, mouseX, mouseY, partialTicks) ->
            {
                ArrayList<OrderedText> lines = new ArrayList<>(textRenderer.wrapLines(Text.translatable(rule.getTranslationKey()), 150));
                String descriptionKey = rule.getTranslationKey() + ".description";

                lines.add(Text.literal(rule.getName().replace(Splatcraft.MODID + ".", "")).formatted(Formatting.YELLOW).asOrderedText());

                if (I18n.hasTranslation(descriptionKey))
                    lines.addAll(textRenderer.wrapLines(Text.translatable(descriptionKey).formatted(Formatting.GRAY), 150));

                guiGraphics.drawOrderedTooltip(MinecraftClient.getInstance().textRenderer, lines, mouseX, mouseY);
            }), (ps, b) ->
            {
            }, ButtonColor.GREEN);
            setMessage(Text.translatable(rule.getTranslationKey()));
        }

        @Override
        public void renderWidget(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks)
        {
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderTexture(0, TEXTURES);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            guiGraphics.drawTexture(TEXTURES, getX(), getY(), 0, 244, width, height);

            Text label = getMessage();
            OrderedText sequence = label.asOrderedText();

            if (textRenderer.getWidth(label) > width)
                sequence = Language.getInstance().reorder(StringVisitable.concat(Arrays.asList(textRenderer.trimToWidth(label, width - 12), StringVisitable.styled("...", Style.EMPTY))));

            guiGraphics.drawTextWithShadow(textRenderer, sequence, getX() + (3), getY() + (getHeight() - 8) / 2, getFGColor() | MathHelper.ceil(getAlpha() * 255.0F) << 24);
        }

        @Override
        public void playDownSound(@NotNull SoundManager soundManager)
        {
        }
    }

    class RuleValueButton extends MenuButton
    {
        static RuleValueButton heldButton = null;
        final GameRules.Key<GameRules.BooleanRule> rule;

        public RuleValueButton(GameRules.Key<GameRules.BooleanRule> rule)
        {
            super(154, 24, 34, 12, (b) ->
            {
            }, (button, poseStack, mouseX, mouseY, partialTicks) ->
            {
                if (heldButton == null || heldButton.rule.equals(rule) /*can't ref to self before super, so this'll have to do*/)
                {
                    Boolean value = rules.get(rule).value;
                    showText(value == null ? Text.translatable("gui.stage_pad.button.rule_value.default",
                        Text.translatable("gui.stage_pad.button.rule_value." + (SplatcraftGameRules.getClientsideBooleanValue(rule) ? "on" : "off"))) :
                        Text.translatable("gui.stage_pad.button.rule_value." + (value ? "on" : "off"))).onTooltip(button, poseStack, mouseX, mouseY, partialTicks);
                }
            }, (guiGraphics, button) ->
            {
            }, ButtonColor.LIME);
            this.rule = rule;
        }

        @Override
        public void onRelease(double mouseX, double mouseY)
        {
            super.onRelease(mouseX, mouseY);

            if (equals(heldButton))
            {
                SplatcraftPacketHandler.sendToServer(new RequestSetStageRulePacket(stage.id, rule.getName(), rules.get(rule).value));
                heldButton = null;
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY)
        {
            super.onClick(mouseX, mouseY);

            if (heldButton == null)
                heldButton = this;

            setValue(rules.get(rule), mouseX);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double p_94702_, double p_94703_)
        {
            if (equals(heldButton))
            {
                RuleEntry ruleEntry = rules.get(rule);
                Boolean prevValue = ruleEntry.value;

                setValue(ruleEntry, mouseX);

                if (!(prevValue == null && ruleEntry.value == null) && (ruleEntry.value == null || !ruleEntry.value.equals(prevValue)))
                    playDownSound(MinecraftClient.getInstance().getSoundManager());
            }
            return super.mouseDragged(mouseX, mouseY, mouseButton, p_94702_, p_94703_);
        }

        void setValue(RuleEntry entry, double mouseX)
        {
            entry.value = new Boolean[]{false, null, true}[MathHelper.clamp((int) ((mouseX - getX()) / width * 3), 0, 2)];
        }

        @Override
        public void renderWidget(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks)
        {
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            int i = getYImage(isSelected());
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.setShaderTexture(0, TEXTURES);
            guiGraphics.drawTexture(TEXTURES, getX(), getY(), 144, 244, width, height);

            Boolean bool = rules.get(rule).value;
            int j = bool == null ? 1 : bool ? 2 : 0;

            int notchWidth = 12;
            RenderSystem.setShaderTexture(0, WIDGETS);
            guiGraphics.drawTexture(WIDGETS, getX() + (j * (notchWidth - 1)), getY(), 0, getColor().ordinal() * 36 + i * 12, notchWidth / 2, height);
            guiGraphics.drawTexture(WIDGETS, getX() + (j * (notchWidth - 1)) + notchWidth / 2, getY(), 180 - notchWidth / 2, getColor().ordinal() * 36 + i * 12, notchWidth / 2, height);

            drawIcon(guiGraphics, WIDGETS, (j * width / 3), 0, 220 + j * 12, 36, 12, 12).apply(guiGraphics, this);
        }

        @Override
        protected int getYImage(boolean hovered)
        {
            return super.getYImage(heldButton == null ? hovered : equals(heldButton));
        }

        @Override
        public ButtonColor getColor()
        {
            Boolean value = rules.get(rule).value;
            return value == null ? ButtonColor.CYAN : value ? ButtonColor.LIME : ButtonColor.RED;
        }
    }
}