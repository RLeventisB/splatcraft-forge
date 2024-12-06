package net.splatcraft.client.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3d;
import net.minecraftforge.registries.ForgeRegistries;
import net.splatcraft.client.handlers.JumpLureHudHandler;
import net.splatcraft.mixin.accessors.GameRendererFovAccessor;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.util.GraphicsUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class SuperJumpSelectorScreen
{
    private static final Minecraft mc = Minecraft.getInstance();

    public SuperJumpSelectorScreen()
    {
    }

    private static void drawOptionIcon(GuiGraphics graphics, float x, float y, float z, float scale, float partialTicks, MenuItem option)
    {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, scale);
        option.renderIcon(graphics, ((screenWidth / 2f + x) / scale - 8), ((screenHeight / 2f + y) / scale - 8), z, 1, partialTicks);
        graphics.pose().popPose();
    }

    public double render(GuiGraphics graphics, float partialTicks, JumpLureHudHandler.SuperJumpTargets targets, double scrollDelta, boolean clicked)
    {
        ArrayList<UUID> playerUuids = new ArrayList<>(targets.playerTargetUuids);
        int entryCount = playerUuids.size() + (targets.canTargetSpawn ? 2 : 1);
        int index = Math.floorMod((int) scrollDelta, entryCount);

        ArrayList<MenuItem> options = new ArrayList<>(playerUuids.stream().map(uuid -> new PlayerMenuItem(mc.getConnection().getPlayerInfo(uuid).getProfile())).toList());

        if (targets.canTargetSpawn)
            options.add(0, new ItemStackMenuItem(new ItemStack(SplatcraftItems.spawnPad.get()), Component.literal("Go to Spawn")));
        options.add(0, new ItemStackMenuItem(new ItemStack(Items.BARRIER), Component.literal("Cancel")));

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        for (int i = -Math.min(entryCount / 2, 4); i <= Math.min(entryCount / 2, 4); i++)
        {
            int finalIndex = Math.floorMod((i + index), entryCount);
            float x = screenWidth / 2f - 10 + i * 20;
            options.get(finalIndex).renderIcon(graphics, x, 10, 0, 1, partialTicks);

            graphics.drawCenteredString(mc.font, Integer.toString(finalIndex), (int) x + 8, 30, finalIndex == index ? targets.color : 0xFFFFFF);
        }

        double fov = ((GameRendererFovAccessor) mc.gameRenderer).invokeGetFov(
            mc.gameRenderer.getMainCamera(),
            partialTicks,
            true);
        Matrix4f projectionMatrix = mc.gameRenderer.getProjectionMatrix(fov);

        graphics.pose().pushPose();
        boolean selected = false;
        for (int i = 1; i < options.size(); i++)
        {
            var option = options.get(i);

            float x = -1, y = -1, scale = 1;
            if (i == 1 && targets.canTargetSpawn && option instanceof ItemStackMenuItem)
            {
                Vec3d spawnPos = Vec3d.atCenterOf(targets.spawnPosition);
                Vector4f screenPos = GraphicsUtils.worldToScreenSpace(spawnPos, projectionMatrix);
                if (screenPos.z > 0)
                    continue;

                scale = screenPos.w;

                x = (screenPos.x * screenWidth);
                y = (screenPos.y * screenHeight);
            }
            else if (option instanceof PlayerMenuItem menuItem)
            {
                Vec3d playerPos = mc.level.getPlayerByUUID(menuItem.profile.getId()).position();
                Vector4f screenPos = GraphicsUtils.worldToScreenSpace(playerPos, projectionMatrix);
                if (screenPos.z > 0)
                    continue;

                scale = screenPos.w;

                x = (screenPos.x * screenWidth);
                y = (screenPos.y * screenHeight);
            }
            scale = Math.max(1, (float) Math.pow(scale, 0.3f));
            boolean close = MathHelper.lengthSquared(x, y) < 1024;
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

            drawOptionIcon(graphics, x, y, 0f, 2.5f / scale, partialTicks, option);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            RenderSystem.setShaderFogStart(0f);
            RenderSystem.setShaderFogEnd(0f);
            RenderSystem.setShaderFogColor(1f, 1f, 1f, 0f);

            drawOptionIcon(graphics, x, y, 100f, 2f / scale, partialTicks, option);

            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 3000f);
            graphics.drawCenteredString(mc.font, option.getName(), (int) ((screenWidth / 2f + x)), (int) ((screenHeight / 2f + y - 8)), i == index ? targets.color : 0xFFFFFF);
            graphics.pose().popPose();
        }
        if (!selected && clicked)
        {
            scrollDelta = 0;
        }
        graphics.pose().popPose();

        graphics.drawCenteredString(mc.font, options.get(index).getName(), screenWidth / 2, 48, 0xFFFFFF);

        return scrollDelta;
    }

    interface MenuItem
    {
        Component getName();

        void renderIcon(GuiGraphics graphics, float x, float y, float z, float alpha, float partialTicks);
    }

    static class ItemStackMenuItem implements MenuItem
    {
        final Component name;
        final ItemStack stack;

        ItemStackMenuItem(ItemStack stack, Component name)
        {
            this.name = name;
            this.stack = stack;
        }

        // yes this is GuiGraphics::renderItem but pX and pY are floats
        public static void renderItem(GuiGraphics graphics, @Nullable LivingEntity pEntity, @Nullable Level pLevel, ItemStack pStack, float pX, float pY, int pSeed, float pGuiOffset)
        {
            if (!pStack.isEmpty())
            {
                Minecraft minecraft = Minecraft.getInstance();
                BakedModel bakedmodel = minecraft.getItemRenderer().getModel(pStack, pLevel, pEntity, pSeed);
                graphics.pose().pushPose();
                graphics.pose().translate((pX + 8), (pY + 8), 150 + (bakedmodel.isGui3d() ? pGuiOffset : 0));

                try
                {
                    graphics.pose().mulPoseMatrix((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));
                    graphics.pose().scale(16.0F, 16.0F, 16.0F);
                    boolean flag = !bakedmodel.usesBlockLight();
                    if (flag)
                    {
                        Lighting.setupForFlatItems();
                    }

                    minecraft.getItemRenderer().render(pStack, ItemDisplayContext.GUI, false, graphics.pose(), graphics.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, bakedmodel);
                    graphics.flush();
                    if (flag)
                    {
                        Lighting.setupFor3DItems();
                    }
                }
                catch (Throwable var12)
                {
                    CrashReport crashreport = CrashReport.forThrowable(var12, "Rendering item");
                    CrashReportCategory crashreportcategory = crashreport.addCategory("Item being rendered");
                    crashreportcategory.setDetail("Item Type", () -> String.valueOf(pStack.getItem()));
                    crashreportcategory.setDetail("Registry Name", () -> String.valueOf(ForgeRegistries.ITEMS.getKey(pStack.getItem())));
                    crashreportcategory.setDetail("Item Damage", () -> String.valueOf(pStack.getDamageValue()));
                    crashreportcategory.setDetail("Item NBT", () -> String.valueOf(pStack.getTag()));
                    crashreportcategory.setDetail("Item Foil", () -> String.valueOf(pStack.hasFoil()));
                    throw new ReportedException(crashreport);
                }

                graphics.pose().popPose();
            }
        }

        @Override
        public Component getName()
        {
            return name;
        }

        @Override
        public void renderIcon(GuiGraphics graphics, float x, float y, float z, float alpha, float partialTicks)
        {
            float[] rgba = RenderSystem.getShaderColor().clone();
            RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], alpha);
            renderItem(graphics, Minecraft.getInstance().player, Minecraft.getInstance().level, this.stack, x, y, 0, z);
            RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], rgba[3]);
        }
    }

    static class PlayerMenuItem implements MenuItem
    {
        private final GameProfile profile;
        private final ResourceLocation location;
        private final Component name;

        public PlayerMenuItem(GameProfile profile)
        {
            this.profile = profile;
            Minecraft minecraft = Minecraft.getInstance();
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = minecraft.getSkinManager().getInsecureSkinInformation(profile);
            if (map.containsKey(MinecraftProfileTexture.Type.SKIN))
                this.location = minecraft.getSkinManager().registerTexture(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
            else
                this.location = DefaultPlayerSkin.getDefaultSkin(UUIDUtil.getOrCreatePlayerUUID(profile));

            this.name = Component.literal(profile.getName());
        }

        public static void blit(GuiGraphics graphics, ResourceLocation pAtlasLocation, int pX, int pY, int pWidth, int pHeight, float pUOffset, float pVOffset, int pUWidth, int pVHeight, int pTextureWidth, int pTextureHeight)
        {
            innerBlit(graphics, pAtlasLocation, pX, pX + pWidth, pY, pY + pHeight, 0, (pUOffset + 0.0F) / (float) pTextureWidth, (pUOffset + (float) pUWidth) / (float) pTextureWidth, (pVOffset + 0.0F) / (float) pTextureHeight, (pVOffset + (float) pVHeight) / (float) pTextureHeight);
        }

        public static void innerBlit(GuiGraphics graphics, ResourceLocation pAtlasLocation, float pX1, float pX2, float pY1, float pY2, float pBlitOffset, float pMinU, float pMaxU, float pMinV, float pMaxV)
        {
            RenderSystem.setShaderTexture(0, pAtlasLocation);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            Matrix4f matrix4f = graphics.pose().last().pose();
            BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
            bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bufferbuilder.vertex(matrix4f, pX1, pY1, pBlitOffset).uv(pMinU, pMinV).endVertex();
            bufferbuilder.vertex(matrix4f, pX1, pY2, pBlitOffset).uv(pMinU, pMaxV).endVertex();
            bufferbuilder.vertex(matrix4f, pX2, pY2, pBlitOffset).uv(pMaxU, pMaxV).endVertex();
            bufferbuilder.vertex(matrix4f, pX2, pY1, pBlitOffset).uv(pMaxU, pMinV).endVertex();
            BufferUploader.drawWithShader(bufferbuilder.end());
        }

        @Override
        public Component getName()
        {
            return name;
        }

        @Override
        public void renderIcon(GuiGraphics graphics, float x, float y, float z, float alpha, float partialTicks)
        {
            float[] rgba = RenderSystem.getShaderColor().clone();
            RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], alpha);
            innerBlit(graphics, location, x, x + 16, y, y + 16, z, 0.125f, 0.25f, 0.125f, 0.25f);
            innerBlit(graphics, location, x, x + 16, y, y + 16, z, 0.625f, 0.75f, 0.125f, 0.25f);
            RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], rgba[3]);
        }
    }
}
