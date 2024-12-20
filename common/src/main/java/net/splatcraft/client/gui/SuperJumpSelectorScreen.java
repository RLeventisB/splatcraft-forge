package net.splatcraft.client.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.splatcraft.client.handlers.JumpLureHudHandler;
import net.splatcraft.mixin.accessors.GameRendererFovAccessor;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.GraphicsUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.UUID;

public class SuperJumpSelectorScreen
{
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public SuperJumpSelectorScreen()
    {
    }

    private static void drawOptionIcon(DrawContext graphics, float x, float y, float z, float scale, float partialTicks, MenuItem option)
    {
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        graphics.getMatrices().push();
        graphics.getMatrices().scale(scale, scale, scale);
        option.renderIcon(graphics, ((screenWidth / 2f + x) / scale - 8), ((screenHeight / 2f + y) / scale - 8), z, 1, partialTicks);
        graphics.getMatrices().pop();
    }

    public double render(DrawContext graphics, RenderTickCounter tickCounter, JumpLureHudHandler.SuperJumpTargets targets, double scrollDelta, boolean clicked)
    {
        ArrayList<UUID> playerUuids = new ArrayList<>(targets.playerTargetUuids);
        int entryCount = playerUuids.size() + (targets.canTargetSpawn ? 2 : 1);
        int index = Math.floorMod((int) scrollDelta, entryCount);

        ArrayList<MenuItem> options = new ArrayList<>(playerUuids.stream().map(uuid -> new PlayerMenuItem(mc.getNetworkHandler().getPlayerListEntry(uuid).getProfile())).toList());

        if (targets.canTargetSpawn)
            options.add(0, new ItemStackMenuItem(new ItemStack(SplatcraftItems.spawnPad.get()), Text.literal("Go to Spawn")));
        options.add(0, new ItemStackMenuItem(new ItemStack(Items.BARRIER), Text.literal("Cancel")));

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        for (int i = -Math.min(entryCount / 2, 4); i <= Math.min(entryCount / 2, 4); i++)
        {
            int finalIndex = Math.floorMod((i + index), entryCount);
            float x = screenWidth / 2f - 10 + i * 20;
            options.get(finalIndex).renderIcon(graphics, x, 10, 0, 1, tickCounter.getTickDelta(true));

            graphics.drawCenteredTextWithShadow(mc.textRenderer, Integer.toString(finalIndex), (int) x + 8, 30, finalIndex == index ? targets.color.getColorWithAlpha(255) : 0xFFFFFF);
        }

        double fov = ((GameRendererFovAccessor) mc.gameRenderer).invokeGetFov(
            mc.gameRenderer.getCamera(),
            tickCounter.getTickDelta(true),
            true);
        Matrix4f projectionMatrix = mc.gameRenderer.getBasicProjectionMatrix(fov);

        graphics.getMatrices().push();
        boolean selected = false;
        for (int i = 1; i < options.size(); i++)
        {
            var option = options.get(i);

            float x = -1, y = -1, scale = 1;
            if (i == 1 && targets.canTargetSpawn && option instanceof ItemStackMenuItem)
            {
                Vec3d spawnPos = Vec3d.ofCenter(targets.spawnPosition);
                Vector4f screenPos = GraphicsUtils.worldToScreenSpace(spawnPos, projectionMatrix);
                if (screenPos.z > 0)
                    continue;

                scale = screenPos.w;

                x = (screenPos.x * screenWidth);
                y = (screenPos.y * screenHeight);
            }
            else if (option instanceof PlayerMenuItem menuItem)
            {
                Vec3d playerPos = mc.world.getPlayerByUuid(menuItem.profile.getId()).getPos();
                Vector4f screenPos = GraphicsUtils.worldToScreenSpace(playerPos, projectionMatrix);
                if (screenPos.z > 0)
                    continue;

                scale = screenPos.w;

                x = (screenPos.x * screenWidth);
                y = (screenPos.y * screenHeight);
            }
            scale = Math.max(1, (float) Math.pow(scale, 0.3f));
            boolean close = MathHelper.squaredHypot(x, y) < 1024;
            if (close && clicked && !selected && index != i)
            {
                scrollDelta = i;
                index = i;
                selected = true;
            }
            if (index == i)
            {
                RenderSystem.setShaderFogStart(0f);
                RenderSystem.setShaderFogEnd(10000f);
                RenderSystem.setShaderFogColor(1f, 1f, 1f);
            }
            else if (close)
            {
                RenderSystem.setShaderFogStart(0f);
                RenderSystem.setShaderFogEnd(10000f);
                RenderSystem.setShaderFogColor(0.6f, 0.6f, 0.6f);
            }
            else
            {
                RenderSystem.setShaderColor(0f, 0f, 0f, 1f);
            }

            drawOptionIcon(graphics, x, y, 0f, 2.5f / scale, tickCounter.getTickDelta(true), option);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            RenderSystem.setShaderFogStart(0f);
            RenderSystem.setShaderFogEnd(0f);
            RenderSystem.setShaderFogColor(1f, 1f, 1f, 0f);

            drawOptionIcon(graphics, x, y, 100f, 2f / scale, tickCounter.getTickDelta(true), option);

            graphics.getMatrices().push();
            graphics.getMatrices().translate(0, 0, 3000f);
            graphics.drawCenteredTextWithShadow(mc.textRenderer, option.getName(), (int) ((screenWidth / 2f + x)), (int) ((screenHeight / 2f + y - 8)), i == index ? targets.color.getColorWithAlpha(255) : 0xFFFFFF);
            graphics.getMatrices().pop();
        }
        if (!selected && clicked)
        {
            scrollDelta = 0;
        }
        graphics.getMatrices().pop();

        graphics.drawCenteredTextWithShadow(mc.textRenderer, options.get(index).getName(), screenWidth / 2, 48, 0xFFFFFF);

        return scrollDelta;
    }

    interface MenuItem
    {
        Text getName();

        void renderIcon(DrawContext graphics, float x, float y, float z, float alpha, float partialTicks);
    }

    static class ItemStackMenuItem implements MenuItem
    {
        final Text name;
        final ItemStack stack;

        ItemStackMenuItem(ItemStack stack, Text name)
        {
            this.name = name;
            this.stack = stack;
        }

        // yes this is DrawContext::renderItem but pX and pY are floats
        public static void renderItem(DrawContext graphics, @Nullable LivingEntity pEntity, @Nullable World pLevel, ItemStack pStack, float pX, float pY, int pSeed, float pGuiOffset)
        {
            if (!pStack.isEmpty())
            {
                MinecraftClient minecraft = MinecraftClient.getInstance();
                BakedModel bakedmodel = minecraft.getItemRenderer().getModel(pStack, pLevel, pEntity, pSeed);
                graphics.getMatrices().push();
                graphics.getMatrices().translate((pX + 8), (pY + 8), 150 + (bakedmodel.hasDepth() ? pGuiOffset : 0));

                try
                {
                    graphics.getMatrices().multiplyPositionMatrix((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));
                    graphics.getMatrices().scale(16.0F, 16.0F, 16.0F);
                    boolean flag = !bakedmodel.isSideLit();
                    if (flag)
                    {
                        DiffuseLighting.disableGuiDepthLighting();
                    }

                    minecraft.getItemRenderer().renderItem(pStack, ModelTransformationMode.GUI, false, graphics.getMatrices(), graphics.getVertexConsumers(), 15728880, OverlayTexture.DEFAULT_UV, bakedmodel);
                    graphics.draw();
                    if (flag)
                    {
                        DiffuseLighting.enableGuiDepthLighting();
                    }
                }
                catch (Throwable var12)
                {
                    CrashReport crashreport = CrashReport.create(var12, "Rendering item");
                    CrashReportSection section = crashreport.addElement("Item being rendered");
                    section.add("Item Type", () -> String.valueOf(pStack.getItem()));
                    section.add("Registry Name", () -> String.valueOf(Registries.ITEM.getKey(pStack.getItem())));
                    section.add("Item Damage", () -> String.valueOf(pStack.getDamage()));
                    section.add("Item NBT", () -> String.valueOf(pStack.getComponents()));
                    section.add("Item Foil", () -> String.valueOf(pStack.hasGlint()));
                    throw new CrashException(crashreport);
                }

                graphics.getMatrices().pop();
            }
        }

        @Override
        public Text getName()
        {
            return name;
        }

        @Override
        public void renderIcon(DrawContext graphics, float x, float y, float z, float alpha, float partialTicks)
        {
            float[] rgba = RenderSystem.getShaderColor().clone();
            RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], alpha);
            renderItem(graphics, ClientUtils.getClientPlayer(), MinecraftClient.getInstance().world, stack, x, y, 0, z);
            RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], rgba[3]);
        }
    }

    static class PlayerMenuItem implements MenuItem
    {
        private final GameProfile profile;
        private final Identifier location;
        private final Text name;

        public PlayerMenuItem(GameProfile profile)
        {
            this.profile = profile;
            MinecraftClient minecraft = MinecraftClient.getInstance();
            SkinTextures textures = minecraft.getSkinProvider().getSkinTextures(profile);
            location = textures.texture();

            name = Text.literal(profile.getName());
        }

        public static void drawTexture(DrawContext graphics, Identifier pAtlasLocation, int pX, int pY, int pWidth, int pHeight, float pUOffset, float pVOffset, int pUWidth, int pVHeight, int pTextureWidth, int pTextureHeight)
        {
            innerBlit(graphics, pAtlasLocation, pX, pX + pWidth, pY, pY + pHeight, 0, (pUOffset + 0.0F) / (float) pTextureWidth, (pUOffset + (float) pUWidth) / (float) pTextureWidth, (pVOffset + 0.0F) / (float) pTextureHeight, (pVOffset + (float) pVHeight) / (float) pTextureHeight);
        }

        public static void innerBlit(DrawContext graphics, Identifier pAtlasLocation, float pX1, float pX2, float pY1, float pY2, float pBlitOffset, float pMinU, float pMaxU, float pMinV, float pMaxV)
        {
            RenderSystem.setShaderTexture(0, pAtlasLocation);
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            Matrix4f matrix4f = graphics.getMatrices().peek().getPositionMatrix();
            BufferBuilder bufferbuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            bufferbuilder.vertex(matrix4f, pX1, pY1, pBlitOffset).texture(pMinU, pMinV);
            bufferbuilder.vertex(matrix4f, pX1, pY2, pBlitOffset).texture(pMinU, pMaxV);
            bufferbuilder.vertex(matrix4f, pX2, pY2, pBlitOffset).texture(pMaxU, pMaxV);
            bufferbuilder.vertex(matrix4f, pX2, pY1, pBlitOffset).texture(pMaxU, pMinV);
            BufferRenderer.drawWithGlobalProgram(bufferbuilder.end());
        }

        @Override
        public Text getName()
        {
            return name;
        }

        @Override
        public void renderIcon(DrawContext graphics, float x, float y, float z, float alpha, float partialTicks)
        {
            float[] rgba = RenderSystem.getShaderColor().clone();
            RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], alpha);
            innerBlit(graphics, location, x, x + 16, y, y + 16, z, 0.125f, 0.25f, 0.125f, 0.25f);
            innerBlit(graphics, location, x, x + 16, y, y + 16, z, 0.625f, 0.75f, 0.125f, 0.25f);
            RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], rgba[3]);
        }
    }
}
