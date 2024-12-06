package net.splatcraft.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class GraphicsUtils
{
    public static void blit(GuiGraphics graphics, ResourceLocation pAtlasLocation, float pX, float pY, float pUOffset, float pVOffset, int pUWidth, int pVHeight)
    {
        blit(graphics, pAtlasLocation, pX, pY, 0, pUOffset, pVOffset, pUWidth, pVHeight, 256, 256);
    }

    public static void blit(GuiGraphics graphics, ResourceLocation pAtlasLocation, float pX, float pY, float pBlitOffset, float pUOffset, float pVOffset, float pUWidth, float pVHeight, float pTextureWidth, float pTextureHeight)
    {
        blit(graphics, pAtlasLocation, pX, pX + pUWidth, pY, pY + pVHeight, pBlitOffset, pUWidth, pVHeight, pUOffset, pVOffset, pTextureWidth, pTextureHeight);
    }

    public static void blit(GuiGraphics graphics, ResourceLocation pAtlasLocation, float pX, float pY, float pWidth, float pHeight, float pUOffset, float pVOffset, float pUWidth, float pVHeight, float pTextureWidth, float pTextureHeight)
    {
        blit(graphics, pAtlasLocation, pX, pX + pWidth, pY, pY + pHeight, 0, pUWidth, pVHeight, pUOffset, pVOffset, pTextureWidth, pTextureHeight);
    }

    public static void blit(GuiGraphics graphics, ResourceLocation pAtlasLocation, float pX, float pY, float pUOffset, float pVOffset, float pWidth, float pHeight, float pTextureWidth, float pTextureHeight)
    {
        blit(graphics, pAtlasLocation, pX, pY, pWidth, pHeight, pUOffset, pVOffset, pWidth, pHeight, pTextureWidth, pTextureHeight);
    }

    public static void blit(GuiGraphics graphics, ResourceLocation pAtlasLocation, float pX1, float pX2, float pY1, float pY2, float pBlitOffset, float pUWidth, float pVHeight, float pUOffset, float pVOffset, float pTextureWidth, float pTextureHeight)
    {
        innerBlit(graphics, pAtlasLocation, pX1, pX2, pY1, pY2, pBlitOffset, (pUOffset + 0.0F) / pTextureWidth, (pUOffset + pUWidth) / pTextureWidth, (pVOffset + 0.0F) / pTextureHeight, (pVOffset + pVHeight) / pTextureHeight);
    }

    public static void innerBlit(GuiGraphics graphics, ResourceLocation pAtlasLocation, float pX1, float pX2, float pY1, float pY2, float pBlitOffset, float pMinU, float pMaxU, float pMinV, float pMaxV)
    {
        RenderSystem.setShaderTexture(0, pAtlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = graphics.pose().last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(matrix4f, pX1, pY1, pBlitOffset).uv(pMinU, pMinV).endVertex(); // if this can handle ints, then why not lol
        bufferbuilder.vertex(matrix4f, pX1, pY2, pBlitOffset).uv(pMinU, pMaxV).endVertex();
        bufferbuilder.vertex(matrix4f, pX2, pY2, pBlitOffset).uv(pMaxU, pMaxV).endVertex();
        bufferbuilder.vertex(matrix4f, pX2, pY1, pBlitOffset).uv(pMaxU, pMinV).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());
    }

    // omfg thank you random repo i didnt know about (sorry if this sounds rude)https://github.com/FiguraMC/Figura/blob/85a406acd607dc39d1d2c865af817a70941d1956/common/src/main/java/org/figuramc/figura/utils/MathUtils.java#L88
    public static Vector4f worldToScreenSpace(Vec3d worldSpace, Matrix4f projMat)
    {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();

        Vec3d camPos = camera.getPosition();
        Vec3d relativePos = worldSpace.subtract(camPos.x, camPos.y, camPos.z);
        return relativeWorldToScreenSpace(relativePos, projMat);
    }

    public static @NotNull Vector4f relativeWorldToScreenSpace(Vec3d relativePos, Matrix4f projMat)
    {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Matrix3f transformMatrix = new Matrix3f().rotation(camera.rotation());
        transformMatrix.invert();

        Vector3f camSpace = relativePos.toVector3f();
        transformMatrix.transform(camSpace);

        Vector4f projectiveCamSpace = new Vector4f(camSpace, 1f);
        projMat.transform(projectiveCamSpace);
        float w = projectiveCamSpace.w();

        return new Vector4f(projectiveCamSpace.x() / w / 2f, projectiveCamSpace.y() / w / 2f, w, (float) Math.sqrt(relativePos.dot(relativePos)));
    }
}
