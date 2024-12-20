package net.splatcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.splatcraft.Splatcraft;
import net.splatcraft.crafting.*;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.CraftWeaponPacket;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.tileentities.container.WeaponWorkbenchContainer;
import net.splatcraft.util.ColorUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WeaponWorkbenchScreen extends AbstractInventoryScreen<WeaponWorkbenchContainer>
{
    private static final Identifier TEXTURES = Splatcraft.identifierOf("textures/gui/weapon_crafting.png");
    private final PlayerInventory inventory;
    PlayerEntity player;
    private int tabPos = 0;
    private int sectionPos = 0;
    private int typePos = 0;
    private int subTypePos = 0;
    private int ingredientPos = 0;
    private int tickTime = 0;
    private WeaponWorkbenchSubtypeRecipe selectedRecipe = null;
    private WeaponWorkbenchRecipe selectedWeapon = null;
    private int craftButtonState = -1;

    public WeaponWorkbenchScreen(WeaponWorkbenchContainer screenContainer, PlayerInventory inv, Text titleIn)
    {
        super(screenContainer, inv, titleIn);

        backgroundHeight = 226;
        titleX = 8;
        titleY = backgroundHeight - 92;
        player = inv.player;
        inventory = inv;
    }

    protected static Text getDisplayName(ItemStack stack)
    {
        MutableText iformattabletextcomponent = MutableText.of(stack.toHoverableText().getContent());
        if (stack.contains(DataComponentTypes.CUSTOM_NAME))
            iformattabletextcomponent.formatted(Formatting.ITALIC);

        iformattabletextcomponent.formatted(stack.getRarity().getFormatting()).styled((style) ->
            style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackContent(stack))));

        return iformattabletextcomponent;
    }

    @Override
    public void render(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        drawMouseoverTooltip(guiGraphics, mouseX, mouseY);

        tickTime++;
    }

    @Override
    public void drawBackground(@NotNull DrawContext guiGraphics, float delta, int mouseX, int mouseY)
    {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, TEXTURES);
        if (client != null)
        {
            int x = (width - backgroundWidth) / 2;
            int y = (height - backgroundHeight) / 2;

            guiGraphics.drawTexture(TEXTURES, x, y, 0, 0, backgroundWidth, backgroundHeight);

            World world = player.getWorld();
            List<RecipeEntry<WeaponWorkbenchTab>> tabList = world.getRecipeManager().getAllMatches(SplatcraftRecipeTypes.WEAPON_STATION_TAB_TYPE, new WeaponWorkbenchRecipeInput(inventory), world);
            tabList.removeIf(tab -> tab.value().hidden && tab.value().getTabRecipes(world, player).isEmpty());
            tabList.sort((o, o2) -> o.value().compareTo(o2.value()));

            List<WeaponWorkbenchRecipe> recipeList = tabList.get(tabPos).value().getTabRecipes(world, player);
            recipeList.sort(WeaponWorkbenchRecipe::compareTo);

            if (!recipeList.isEmpty())
            {
                if (recipeList.get(typePos).getAvailableRecipesTotal(player) == 1)
                {
                    drawRecipeStack(world, guiGraphics, recipeList, x, y, 0);
                }
                else
                {
                    for (int z = -1; z <= 1; z++)
                    {
                        drawRecipeStack(world, guiGraphics, recipeList, x, y, z);
                    }
                }
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void drawRecipeStack(World world, DrawContext guiGraphics, List<WeaponWorkbenchRecipe> recipeList, int x, int y, int i)
    {
        WeaponWorkbenchSubtypeRecipe selectedRecipe = recipeList.get(typePos).getRecipeFromIndex(player, subTypePos + i < 0 ? recipeList.get(typePos).getAvailableRecipesTotal(player) - 1 : (subTypePos + i) % recipeList.get(typePos).getAvailableRecipesTotal(player));
        ItemStack displayStack = selectedRecipe.getOutput().copy();
        ColorUtils.setInkColor(displayStack, EntityInfoCapability.get(player).getColor());

        guiGraphics.getMatrices().push();
        float scale = i == 0 ? -28F : -14F;
        DiffuseLighting.enableGuiDepthLighting();
        MatrixStack displayStackMatrix = new MatrixStack();
        displayStackMatrix.translate(x + 88 + i * 26, y + 73, 100);
        displayStackMatrix.scale(scale, scale, scale);
        displayStackMatrix.multiply(new Quaternionf(0, 1, 0, 0));

        VertexConsumerProvider.Immediate irendertypebuffer$impl = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        int light = 15728880;
        //if(!MinecraftForge.EVENT_BUS.post(new RenderItemEvent(displayStack, ItemCameraTransforms.TransformType.GUI, displayStackMatrix, irendertypebuffer$impl, light, OverlayTexture.NO_OVERLAY, minecraft.getDeltaFrameTime())))
        {
            ItemRenderer itemRenderer = client.getItemRenderer();
            if (itemRenderer != null)
            {
                client.getItemRenderer().renderItem(displayStack, ModelTransformationMode.GUI, false, displayStackMatrix, irendertypebuffer$impl, light, OverlayTexture.DEFAULT_UV, client.getItemRenderer().getModel(displayStack, world, player, 0));
            }
        }

        irendertypebuffer$impl.draw();
        guiGraphics.getMatrices().pop();
    }

    @Override
    protected void drawForeground(DrawContext guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawText(textRenderer, title.getString(), backgroundWidth / 2 - textRenderer.getWidth(title.getString()) / 2, 22, 4210752, true);
        guiGraphics.drawTextWithShadow(textRenderer, inventory.getDisplayName(), titleX, titleY, 4210752);

        World world = player.getWorld();
        List<RecipeEntry<WeaponWorkbenchTab>> tabList = world.getRecipeManager().getAllMatches(SplatcraftRecipeTypes.WEAPON_STATION_TAB_TYPE, new WeaponWorkbenchRecipeInput(inventory), world);
        tabList.sort(Comparator.comparing(RecipeEntry::value));
        tabList.removeIf(tab -> tab.value().hidden && tab.value().getTabRecipes(world, player).isEmpty());
        List<WeaponWorkbenchRecipe> recipeList = tabList.get(tabPos).value().getTabRecipes(world, player);
        recipeList.sort(WeaponWorkbenchRecipe::compareTo);

        selectedWeapon = null;
        selectedRecipe = null;
        if (!recipeList.isEmpty())
        {
            selectedWeapon = recipeList.get(typePos);
            selectedRecipe = selectedWeapon.getRecipeFromIndex(player, subTypePos);
        }
        else
        {
            int boxSize = 106;
            Text emptyText = Text.translatable("gui.ammo_knights_workbench.empty");
            List<OrderedText> split = textRenderer.wrapLines(emptyText, boxSize);

            int yy = 73 - split.size() * textRenderer.fontHeight / 2;
            for (OrderedText formattedcharsequence : split)
            {
                guiGraphics.drawText(textRenderer, formattedcharsequence, (backgroundWidth - textRenderer.getWidth(formattedcharsequence)) / 2, yy, 0xFFFFFF, true);
                yy += textRenderer.fontHeight;
            }
        }

        //Update Craft Button
        if (selectedRecipe != null)
        {
            boolean hasMaterial = true;
            for (int i = ingredientPos * 8; i < selectedRecipe.getInput().size() && i < ingredientPos * 8 + 8; i++)
            {
                Ingredient ingredient = selectedRecipe.getInput().get(i).getIngredient();
                int count = selectedRecipe.getInput().get(i).getCount();
                if (!SplatcraftRecipeTypes.getItem(player, ingredient, count, false))
                {
                    hasMaterial = false;
                    break;
                }
            }

            if (!hasMaterial)
            {
                craftButtonState = -1;
            }
            else if (craftButtonState == -1)
            {
                craftButtonState = 0;
            }
        }

        TextureManager textureManager = client.getTextureManager();
        if (textureManager != null)
        {
            //Draw Tab Buttons
            for (int i = 0; i < tabList.size(); i++)
            {
                int ix = backgroundWidth / 2 - (tabList.size() - 1) * 11 + i * 22;
                int iy = -5;
                int ty = tabPos == i ? 8 : 28;

                RenderSystem.setShaderTexture(0, TEXTURES);
                guiGraphics.drawTexture(TEXTURES, ix - 10, iy, 211, ty, 20, 20);

                Identifier tabIcon = tabList.get(i).value().getTabIcon();
                Item itemIcon = Registries.ITEM.get(tabIcon);

                if (!itemIcon.equals(Items.AIR))
                {
                    guiGraphics.drawItem(new ItemStack(itemIcon), ix - 8, iy + 2);
                }
                else
                {
                    RenderSystem.setShaderTexture(0, tabIcon);
                    guiGraphics.drawTexture(TEXTURES, ix - 8, iy + 2, 16, 16, 0, 0, 256, 256, 256, 256);
                }
            }
            RenderSystem.setShaderTexture(0, TEXTURES);
        }

        //Draw Weapon Selection
        for (int i = sectionPos * 8; i < recipeList.size() && i < sectionPos * 8 + 8; i++)
        {
            ItemStack displayStack = recipeList.get(i).getResult(DynamicRegistryManager.EMPTY);

            int j = i - sectionPos * 8;
            int ix = 17 + j * 18;
            int iy = 34;

            guiGraphics.drawItem(displayStack, ix, iy);
            if (isPointWithinBounds(ix, iy, 16, 16, mouseX, mouseY))
            {
                RenderSystem.disableDepthTest();
                RenderSystem.colorMask(true, true, true, false);
                int slotColor = -2130706433;
                guiGraphics.fillGradient(ix, iy, ix + 16, iy + 16, slotColor, slotColor);
                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.enableDepthTest();
            }
        }

        //Draw Ingredients
        if (selectedRecipe != null)
        {
            for (int i = ingredientPos * 8; i < selectedRecipe.getInput().size() && i < ingredientPos * 8 + 8; i++)
            {
                Ingredient ingredient = selectedRecipe.getInput().get(i).getIngredient();
                int count = selectedRecipe.getInput().get(i).getCount();
                ItemStack displayStack = ingredient.getMatchingStacks()[tickTime / 20 % ingredient.getMatchingStacks().length];

                int j = i - ingredientPos * 6;
                int ix = 17 + j * 18;
                int iy = 108;

                VertexConsumerProvider.Immediate irendertypebuffer$impl = VertexConsumerProvider.immediate(Tessellator.getInstance().allocator);

                boolean hasMaterial = SplatcraftRecipeTypes.getItem(player, ingredient, count, false);
                int color = hasMaterial ? 0xFFFFFF : 0xFF5555;
                String s = String.valueOf(count);

                guiGraphics.drawItem(displayStack, ix, iy);

                if (!hasMaterial)
                {
                    RenderSystem.disableDepthTest();
                    RenderSystem.colorMask(true, true, true, false);
                    int slotColor = 0x40ff0000;
                    guiGraphics.fillGradient(ix, iy, ix + 16, iy + 16, slotColor, slotColor);
                    RenderSystem.colorMask(true, true, true, true);
                    RenderSystem.enableDepthTest();
                }

                if (count != 1)
                {
                    guiGraphics.getMatrices().translate(0.0D, 0.0D, 200.0F);
                    textRenderer.draw(s, (float) (ix + 19 - 2 - textRenderer.getWidth(s)), (float) (iy + 6 + 3), color, true, guiGraphics.getMatrices().peek().getPositionMatrix(), irendertypebuffer$impl, TextRenderer.TextLayerType.NORMAL, 0, 15728880);
                    guiGraphics.getMatrices().translate(0.0D, 0.0D, -(200.0F));
                    irendertypebuffer$impl.draw();
                }
            }
        }
        RenderSystem.setShaderTexture(0, TEXTURES);

        //Tab Arrows TODO

        //Weapon Arrows
        int maxSections = (int) Math.ceil(recipeList.size() / 8f);
        if (maxSections > 1)
        {
            int ty = sectionPos + 1 < maxSections ? isPointWithinBounds(162, 36, 7, 11, mouseX, mouseY) ? 24 : 12 : 36;
            guiGraphics.drawTexture(TEXTURES, 162, 36, 231, ty, 7, 11);
            ty = sectionPos - 1 >= 0 ? isPointWithinBounds(7, 36, 7, 11, mouseX, mouseY) ? 24 : 12 : 36;
            guiGraphics.drawTexture(TEXTURES, 7, 36, 239, ty, 7, 11);
        }

        //Subtype Arrows
        boolean hasSubtypes = !recipeList.isEmpty() && recipeList.get(typePos).getAvailableRecipesTotal(player) > 1;
        int ty = hasSubtypes ? isPointWithinBounds(126, 67, 7, 11, mouseX, mouseY) ? 24 : 12 : 36;
        guiGraphics.drawTexture(TEXTURES, 126, 67, 231, ty, 7, 11);
        ty = hasSubtypes ? isPointWithinBounds(43, 67, 7, 11, mouseX, mouseY) ? 24 : 12 : 36;
        guiGraphics.drawTexture(TEXTURES, 43, 67, 239, ty, 7, 11);

        //Ingredient Arrows
        maxSections = selectedRecipe == null ? 0 : (int) Math.ceil(selectedRecipe.getInput().size() / 8f);
        if (selectedRecipe != null && maxSections > 1)
        {
            ty = sectionPos + 1 <= maxSections ? isPointWithinBounds(162, 110, 7, 11, mouseX, mouseY) ? 24 : 12 : 36;
            guiGraphics.drawTexture(TEXTURES, 162, 110, 231, ty, 7, 11);
            ty = sectionPos - 1 >= 0 ? isPointWithinBounds(7, 110, 7, 11, mouseX, mouseY) ? 24 : 12 : 36;
            guiGraphics.drawTexture(TEXTURES, 7, 110, 239, ty, 7, 11);
        }

        //Craft Button
        ty = 0;
        if (craftButtonState > 0)
        {
            ty = 12;
        }
        else if (craftButtonState == 0)
        {
            ty = isPointWithinBounds(71, 93, 34, 12, mouseX, mouseY) ? 24 : 36;
        }

        guiGraphics.drawTexture(TEXTURES, 71, 93, 177, ty, 34, 12);
        String craftStr = Text.translatable("gui.ammo_knights_workbench.craft").getString();

        guiGraphics.drawText(textRenderer, craftStr, backgroundWidth / 2 - textRenderer.getWidth(craftStr) / 2, 95, ty == 0 ? 0x999999 : 0xEFEFEF, true);
        RenderSystem.setShaderTexture(0, TEXTURES);

        //Selected Pointer
        int selectedPos = typePos - sectionPos * 8;
        if (selectedRecipe != null && selectedPos < 8 && selectedPos >= 0)
        {
            //matrixStack.translate(0.0D, 0.0D, (minecraft.getItemRenderer().zLevel + 500.0F));
            guiGraphics.drawTexture(TEXTURES, 13 + selectedPos * 18, 46, 246, 40, 8, 8);
        }

        //Tab Button Tooltips
        for (int i = 0; i < tabList.size(); i++)
        {
            int ix = backgroundWidth / 2 - (tabList.size() - 1) * 11 + i * 22;
            int iy = -5;
            //matrixStack.translate(0,0,500);
            if (isPointWithinBounds(ix - 10, iy, 18, 18, mouseX, mouseY))
            {
                ArrayList<Text> tooltip = new ArrayList<>();
                tooltip.add(tabList.get(i).value().getName());
                guiGraphics.drawTooltip(textRenderer, tooltip, mouseX - x, mouseY - y);
            }
            //matrixStack.translate(0,0,-500);
        }

        //Draw Recipe Tooltips
        for (int i = sectionPos * 8; i < recipeList.size() && i < sectionPos * 8 + 8; i++)
        {
            ItemStack displayStack = recipeList.get(i).getResult(DynamicRegistryManager.EMPTY);
            displayStack.set(SplatcraftComponents.IS_PLURAL, true);

            int j = i - sectionPos * 8;
            int ix = 17 + j * 18;
            int iy = 34;

            if (isPointWithinBounds(ix, iy, 16, 16, mouseX, mouseY))
            {
                ArrayList<Text> tooltip = new ArrayList<>();
                Text t = Text.translatable("weaponRecipe." + recipeList.get(i).getId());
                if (t.getString().equals("weaponRecipe." + recipeList.get(i).getId()))
                {
                    tooltip.add(getDisplayName(displayStack));
                }
                else
                {
                    tooltip.add(t);
                }

                guiGraphics.drawTooltip(textRenderer, tooltip, mouseX - x, mouseY - y);
            }
        }

        if (selectedRecipe != null)
        {
            //Draw Ingredient Tooltips
            for (int i = ingredientPos * 8; i < selectedRecipe.getInput().size() && i < ingredientPos * 8 + 8; i++)
            {
                Ingredient ingredient = selectedRecipe.getInput().get(i).getIngredient();
                ItemStack displayStack = ingredient.getMatchingStacks()[tickTime / 20 % ingredient.getMatchingStacks().length];

                int j = i - ingredientPos * 6;
                int ix = 17 + j * 18;
                int iy = 108;

                if (isPointWithinBounds(ix, iy, 16, 16, mouseX, mouseY))
                {
                    guiGraphics.drawTooltip(textRenderer, getTooltipFromItem(displayStack), displayStack.getTooltipData(), mouseX - x, mouseY - y);
                }
            }

            //Draw Selected Weapon Tooltip
            if (isPointWithinBounds(74, 59, 28, 28, mouseX, mouseY))
            {
                ItemStack output = selectedRecipe.getOutput();
                guiGraphics.drawTooltip(textRenderer, getTooltipFromItem(output), output.getTooltipData(), mouseX - x, mouseY - y);
            }
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (craftButtonState == 1)
        {
            craftButtonState = 0;
            if (selectedRecipe != null && isPointWithinBounds(71, 93, 34, 12, mouseX, mouseY))
            {
                SplatcraftPacketHandler.sendToServer(new CraftWeaponPacket(selectedWeapon.getId(), subTypePos));
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        World world = player.getWorld();
        List<RecipeEntry<WeaponWorkbenchTab>> tabList = world.getRecipeManager().getAllMatches(SplatcraftRecipeTypes.WEAPON_STATION_TAB_TYPE, new WeaponWorkbenchRecipeInput(inventory), world);
        tabList.sort(Comparator.comparing(RecipeEntry::value));
        tabList.removeIf(tab -> tab.value().hidden && tab.value().getTabRecipes(world, player).isEmpty());

        List<WeaponWorkbenchRecipe> recipeList = tabList.get(tabPos).value().getTabRecipes(world, player);
        recipeList.sort(WeaponWorkbenchRecipe::compareTo);

        //Tab Buttons
        for (int i = 0; i < tabList.size(); i++)
        {
            int ix = backgroundWidth / 2 - (tabList.size() - 1) * 11 + i * 22;
            int iy = -4;

            TextureManager textureManager = client.getTextureManager();
            if (textureManager != null)
            {
                RenderSystem.setShaderTexture(0, TEXTURES);
                if (tabPos != i && isPointWithinBounds(ix - 10, iy, 20, 20, mouseX, mouseY))
                {
                    tabPos = i;
                    typePos = 0;
                    sectionPos = 0;
                    subTypePos = 0;
                    ingredientPos = 0;
                    playButtonSound();
                }
            }
        }

        //Tab Button Arrows TODO

        //Weapon Selection
        for (int i = sectionPos * 8; i < recipeList.size() && i < sectionPos * 8 + 8; i++)
        {
            int j = i - sectionPos * 8;
            int ix = 17 + j * 18;
            int iy = 34;

            if (typePos != i && isPointWithinBounds(ix, iy, 16, 16, mouseX, mouseY))
            {
                typePos = i;
                subTypePos = 0;
                ingredientPos = 0;
                playButtonSound();
            }
        }

        //Weapon Selection Arrows
        int maxSections = (int) Math.ceil(recipeList.size() / 8f);
        if (maxSections > 1)
        {
            if (sectionPos + 1 < maxSections && isPointWithinBounds(162, 36, 7, 11, mouseX, mouseY))
            {
                subTypePos = 0;
                sectionPos++;
                ingredientPos = 0;
                playButtonSound();
            }
            else if (sectionPos - 1 >= 0 && isPointWithinBounds(7, 36, 7, 11, mouseX, mouseY))
            {
                subTypePos = 0;
                sectionPos--;
                ingredientPos = 0;
                playButtonSound();
            }
        }

        //Subtype Arrows
        int totalSubtypes = recipeList.isEmpty() ? 0 : recipeList.get(typePos).getAvailableRecipesTotal(player);
        if (!recipeList.isEmpty() && totalSubtypes > 1)
        {
            if (isPointWithinBounds(126, 67, 7, 11, mouseX, mouseY) || isPointWithinBounds(107, 66, 14, 14, mouseX, mouseY))
            {
                ingredientPos = 0;
                subTypePos = (subTypePos + 1) % totalSubtypes;

                playButtonSound();
            }
            else if (isPointWithinBounds(43, 67, 7, 11, mouseX, mouseY) || isPointWithinBounds(55, 66, 14, 14, mouseX, mouseY))
            {
                ingredientPos = 0;
                subTypePos--;
                if (subTypePos < 0)
                {
                    subTypePos = totalSubtypes - 1;
                }
                playButtonSound();
            }
        }

        //Ingredient Arrows
        maxSections = selectedRecipe == null ? 0 : (int) Math.ceil(selectedRecipe.getInput().size() / 8f);
        if (selectedRecipe != null && maxSections > 1)
        {
            if (sectionPos + 1 <= maxSections && isPointWithinBounds(162, 110, 7, 11, mouseX, mouseY))
            {
                ingredientPos++;
                playButtonSound();
            }
            else if (sectionPos - 1 >= 0 && isPointWithinBounds(7, 110, 7, 11, mouseX, mouseY))
            {
                ingredientPos--;
                playButtonSound();
            }
        }

        //Craft Button
        if (craftButtonState != -1 && isPointWithinBounds(71, 93, 34, 12, mouseX, mouseY))
        {
            craftButtonState = 1;
            playButtonSound();
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playButtonSound()
    {
        SoundManager soundHandler = client.getSoundManager();
        if (soundHandler != null)
        {
            soundHandler.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}