package net.splatcraft.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.splatcraft.Splatcraft;
import net.splatcraft.entities.SpawnShieldEntity;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class SpawnShieldRenderer extends EntityRenderer<SpawnShieldEntity>
{
    private static final Function<Identifier, RenderLayer> ENTITY_TRANSLUCENT_CULL = Util.memoize((p_173198_) ->
    {
        RenderLayer.MultiPhaseParameters rendertype$compositestate = RenderLayer.MultiPhaseParameters.builder().program(new RenderPhase.ShaderProgram(GameRenderer::getRenderTypeEntityTranslucentProgram)).texture(new RenderPhase.Texture(p_173198_, false, false)).transparency(new RenderPhase.Transparency("translucent_transparency", () ->
        {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        }, () ->
        {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        })).lightmap(new RenderPhase.Lightmap(true)).overlay(new RenderPhase.Overlay(true)).build(true);
        return RenderLayer.of("entity_translucent_cull", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, true, true, rendertype$compositestate);
    });

    public SpawnShieldRenderer(EntityRendererFactory.Context context)
    {
        super(context);
    }

    @Override
    public void render(SpawnShieldEntity entity, float entityYaw, float partialTicks, @NotNull MatrixStack matrixStack, @NotNull VertexConsumerProvider buffer, int packedLight)
    {
        float activeTime = entity.getActiveTime();
        float[] rgb = entity.getColor().getRGB();
        float size = entity.getSize();
        float radius = size / 2f;

        if (activeTime <= 0)
        {
            return;
        }

        VertexConsumer builder = buffer.getBuffer(ENTITY_TRANSLUCENT_CULL.apply(getTexture(entity)));// buffer.getBuffer(RenderLayer.entityTranslucentCull(getTextureLocation(entity)));//buffer.getBuffer(Minecraft.isFabulousGraphicsOrBetter() ? RenderLayer.translucentMovingBlock() : RenderLayer.translucentNoCrumbling());

        float alpha = activeTime / entity.MAX_ACTIVE_TIME;

        addVertex(builder, matrixStack, -radius, size, -radius, 0, size, rgb[0], rgb[1], rgb[2], alpha);
        addVertex(builder, matrixStack, radius, size, -radius, size, size, rgb[0], rgb[1], rgb[2], alpha);
        addVertex(builder, matrixStack, radius, 0, -radius, size, 0, rgb[0], rgb[1], rgb[2], alpha);
        addVertex(builder, matrixStack, -radius, 0, -radius, 0, 0, rgb[0], rgb[1], rgb[2], alpha);

        addVertex(builder, matrixStack, -radius, 0, radius, 0, 0, rgb[0], rgb[1], rgb[2], alpha);
        addVertex(builder, matrixStack, radius, 0, radius, size, 0, rgb[0], rgb[1], rgb[2], alpha);
        addVertex(builder, matrixStack, radius, size, radius, size, size, rgb[0], rgb[1], rgb[2], alpha);
        addVertex(builder, matrixStack, -radius, size, radius, 0, size, rgb[0], rgb[1], rgb[2], alpha);

        addVertex(builder, matrixStack, -radius, 0, -radius, 0, 0, rgb[0], rgb[1], rgb[2], alpha);
        addVertex(builder, matrixStack, -radius, 0, radius, 0, size, rgb[0], rgb[1], rgb[2], alpha);
        addVertex(builder, matrixStack, -radius, size, radius, size, size, rgb[0], rgb[1], rgb[2], alpha);
        addVertex(builder, matrixStack, -radius, size, -radius, size, 0, rgb[0], rgb[1], rgb[2], alpha);

        addVertex(builder, matrixStack, radius, 0, -radius, 0, 0, rgb[0], rgb[1], rgb[2], alpha);
        addVertex(builder, matrixStack, radius, size, -radius, size, 0, rgb[0], rgb[1], rgb[2], alpha);
        addVertex(builder, matrixStack, radius, size, radius, size, size, rgb[0], rgb[1], rgb[2], alpha);
        addVertex(builder, matrixStack, radius, 0, radius, 0, size, rgb[0], rgb[1], rgb[2], alpha);

        addVertex(builder, matrixStack, -radius, 0, -radius, 0, 0, rgb[0], rgb[1], rgb[2], alpha);
        addVertex(builder, matrixStack, radius, 0, -radius, size, 0, rgb[0], rgb[1], rgb[2], alpha);
        addVertex(builder, matrixStack, radius, 0, radius, size, size, rgb[0], rgb[1], rgb[2], alpha);
        addVertex(builder, matrixStack, -radius, 0, radius, 0, size, rgb[0], rgb[1], rgb[2], alpha);

        addVertex(builder, matrixStack, -radius, size, radius, 0, size, rgb[0], rgb[1], rgb[2], alpha);
        addVertex(builder, matrixStack, radius, size, radius, size, size, rgb[0], rgb[1], rgb[2], alpha);
        addVertex(builder, matrixStack, radius, size, -radius, size, 0, rgb[0], rgb[1], rgb[2], alpha);
        addVertex(builder, matrixStack, -radius, size, -radius, 0, 0, rgb[0], rgb[1], rgb[2], alpha);
    }

    private void addVertex(VertexConsumer builder, MatrixStack matrixStack, float x, float y, float z, float textureX, float textureY, float r, float g, float b, float a)
    {
        builder.vertex(matrixStack.peek().getPositionMatrix(), x, y, z)
            .color(r, g, b, a)
            .texture(textureX, textureY)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(0, 240)
            .normal(0.0F, 1.0F, 0.0F)
        ;
    }

    @Override
    public @NotNull Identifier getTexture(@NotNull SpawnShieldEntity p_110775_1_)
    {
        return Splatcraft.identifierOf("textures/block/allowed_color_barrier_fancy.png");
    }
}