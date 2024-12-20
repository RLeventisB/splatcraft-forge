package net.splatcraft.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class GraphicsUtils
{
    public static void drawTexture(DrawContext graphics, Identifier pAtlasLocation, float pX, float pY, float pUOffset, float pVOffset, int pUWidth, int pVHeight)
    {
        drawTexture(graphics, pAtlasLocation, pX, pY, 0, pUOffset, pVOffset, pUWidth, pVHeight, 256, 256);
    }

    public static void drawTexture(DrawContext graphics, Identifier pAtlasLocation, float pX, float pY, float pBlitOffset, float pUOffset, float pVOffset, float pUWidth, float pVHeight, float pTextureWidth, float pTextureHeight)
    {
        drawTexture(graphics, pAtlasLocation, pX, pX + pUWidth, pY, pY + pVHeight, pBlitOffset, pUWidth, pVHeight, pUOffset, pVOffset, pTextureWidth, pTextureHeight);
    }

    public static void drawTexture(DrawContext graphics, Identifier pAtlasLocation, float pX, float pY, float pWidth, float pHeight, float pUOffset, float pVOffset, float pUWidth, float pVHeight, float pTextureWidth, float pTextureHeight)
    {
        drawTexture(graphics, pAtlasLocation, pX, pX + pWidth, pY, pY + pHeight, 0, pUWidth, pVHeight, pUOffset, pVOffset, pTextureWidth, pTextureHeight);
    }

    public static void drawTexture(DrawContext graphics, Identifier pAtlasLocation, float pX, float pY, float pUOffset, float pVOffset, float pWidth, float pHeight, float pTextureWidth, float pTextureHeight)
    {
        drawTexture(graphics, pAtlasLocation, pX, pY, pWidth, pHeight, pUOffset, pVOffset, pWidth, pHeight, pTextureWidth, pTextureHeight);
    }

    public static void drawTexture(DrawContext graphics, Identifier pAtlasLocation, float pX1, float pX2, float pY1, float pY2, float pBlitOffset, float pUWidth, float pVHeight, float pUOffset, float pVOffset, float pTextureWidth, float pTextureHeight)
    {
        innerBlit(graphics, pAtlasLocation, pX1, pX2, pY1, pY2, pBlitOffset, (pUOffset + 0.0F) / pTextureWidth, (pUOffset + pUWidth) / pTextureWidth, (pVOffset + 0.0F) / pTextureHeight, (pVOffset + pVHeight) / pTextureHeight);
    }

    public static void innerBlit(DrawContext graphics, Identifier pAtlasLocation, float pX1, float pX2, float pY1, float pY2, float pBlitOffset, float pMinU, float pMaxU, float pMinV, float pMaxV)
    {
        RenderSystem.setShaderTexture(0, pAtlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        Matrix4f matrix4f = graphics.getMatrices().peek().getPositionMatrix();
        BufferBuilder bufferbuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferbuilder.vertex(matrix4f, pX1, pY1, pBlitOffset).texture(pMinU, pMinV); // if this can handle ints, then why not lol
        bufferbuilder.vertex(matrix4f, pX1, pY2, pBlitOffset).texture(pMinU, pMaxV);
        bufferbuilder.vertex(matrix4f, pX2, pY2, pBlitOffset).texture(pMaxU, pMaxV);
        bufferbuilder.vertex(matrix4f, pX2, pY1, pBlitOffset).texture(pMaxU, pMinV);
        BufferRenderer.drawWithGlobalProgram(bufferbuilder.end());
    }

    // omfg thank you random repo i didnt know about (sorry if this sounds rude)https://github.com/FiguraMC/Figura/blob/85a406acd607dc39d1d2c865af817a70941d1956/common/src/main/java/org/figuramc/figura/utils/MathUtils.java#L88
    public static Vector4f worldToScreenSpace(Vec3d worldSpace, Matrix4f projMat)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();

        Vec3d camPos = camera.getPos();
        Vec3d relativePos = worldSpace.subtract(camPos.x, camPos.y, camPos.z);
        return relativeWorldToScreenSpace(relativePos, projMat);
    }

    public static @NotNull Vector4f relativeWorldToScreenSpace(Vec3d relativePos, Matrix4f projMat)
    {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Matrix3f transformMatrix = new Matrix3f().rotation(camera.getRotation());
        transformMatrix.invert();

        Vector3f camSpace = relativePos.toVector3f();
        transformMatrix.transform(camSpace);

        Vector4f projectiveCamSpace = new Vector4f(camSpace, 1f);
        projMat.transform(projectiveCamSpace);
        float w = projectiveCamSpace.w();

        return new Vector4f(projectiveCamSpace.x() / w / 2f, projectiveCamSpace.y() / w / 2f, w, (float) Math.sqrt(relativePos.dotProduct(relativePos)));
    }
}
