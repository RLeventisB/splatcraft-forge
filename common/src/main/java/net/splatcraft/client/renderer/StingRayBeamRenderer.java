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
import net.splatcraft.entities.StingRayBeamEntity;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class StingRayBeamRenderer extends EntityRenderer<StingRayBeamEntity>
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
	public StingRayBeamRenderer(EntityRendererFactory.Context context)
	{
		super(context);
	}
	@Override
	public void render(StingRayBeamEntity entity, float entityYaw, float partialTicks, @NotNull MatrixStack matrixStack, @NotNull VertexConsumerProvider buffer, int packedLight)
	{
		float lifespan = entity.getLifespan() + partialTicks;
		int color = entity.getColor().getColorWithAlpha(255);
		
		if (lifespan <= 0)
		{
			return;
		}
		
		VertexConsumer builder = buffer.getBuffer(ENTITY_TRANSLUCENT_CULL.apply(getTexture(entity)));// buffer.getBuffer(RenderLayer.entityTranslucentCull(getTextureLocation(entity)));//buffer.getBuffer(Minecraft.isFabulousGraphicsOrBetter() ? RenderLayer.translucentMovingBlock() : RenderLayer.translucentNoCrumbling());
		
		builder.vertex(matrixStack.peek().getPositionMatrix(), 0, 0, 0)
			.color(color)
			.texture(0, 0)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(0, 240)
			.normal(0.0F, 1.0F, 0.0F);
	}
	@Override
	public @NotNull Identifier getTexture(@NotNull StingRayBeamEntity entity)
	{
		return Splatcraft.identifierOf("textures/block/allowed_color_barrier_fancy.png");
	}
}