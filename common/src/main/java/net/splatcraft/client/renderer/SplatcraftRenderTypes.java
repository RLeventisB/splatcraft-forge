package net.splatcraft.client.renderer;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Pair;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.platform.Services;
import net.splatcraft.util.ClientUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

// code based from this gist https://gist.github.com/gigaherz/b8756ff463541f07a644ef8f14cb10f5
// thank u gigaherz for doing the guide!!!
@OnlyIn(Dist.CLIENT)
public class SplatcraftRenderTypes
{
	private static final Supplier<ParticleRenderType> inkFrontRenderTypeSupplier = Suppliers.memoize(() -> new ParticleRenderType()
	{
		@Override
		public @NotNull BufferBuilder begin(@NotNull Tesselator tesselator, @NotNull TextureManager textureManager)
		{
			RenderSystem.disableBlend();
			RenderSystem.depthMask(true);
			RenderSystem.disableDepthTest();
			RenderSystem.setShader(CustomRenderTypes::getParticleInkFrontShader);
			RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
			return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
		}
		@Override
		public String toString()
		{
			return "SPLATCRAFT_INK_FRONT";
		}
	});
	public static void initialize()
	{
		Services.PLATFORM.registerShader((provider)
				->
			{
				try
				{
					return Pair.of(new ShaderInstance(provider, "splatcraft:rendertype_entity_silhouette", DefaultVertexFormat.NEW_ENTITY),
						inst -> CustomRenderTypes.entitySilhouetteShader = inst);
				}
				catch (IOException e)
				{
					throw new RuntimeException(e);
				}
			}
		);
		Services.PLATFORM.registerShader((provider)
				->
			{
				try
				{
					return Pair.of(new CustomShaderInstance(provider, "splatcraft:rendertype_particle_inkfront", DefaultVertexFormat.PARTICLE),
						inst -> CustomRenderTypes.particleInkFrontShader = inst);
				}
				catch (IOException e)
				{
					throw new RuntimeException(e);
				}
			}
		);
	}
	public static RenderType entitySilhouette(ResourceLocation texture)
	{
		return CustomRenderTypes.ENTITY_SILHOUETTE.apply(texture);
	}
	public static ParticleRenderType getInkFrontRendertype()
	{
		return inkFrontRenderTypeSupplier.get();
	}
	private static class CustomShaderInstance extends ShaderInstance
	{
		private final Uniform ORTHOGONAL_PROJECTION_MATRIX;
		public CustomShaderInstance(ResourceProvider resourceProvider, String name, VertexFormat vertexFormat) throws IOException
		{
			super(resourceProvider, name, vertexFormat);
			ORTHOGONAL_PROJECTION_MATRIX = getUniform("OrthoProjMat");
		}
		@Override
		public void setDefaultUniforms(VertexFormat.@NotNull Mode mode, @NotNull Matrix4f frustumMatrix, @NotNull Matrix4f projectionMatrix, @NotNull Window window)
		{
			super.setDefaultUniforms(mode, frustumMatrix, projectionMatrix, window);
			
			if (ORTHOGONAL_PROJECTION_MATRIX != null)
			{
				Minecraft client = ClientUtils.getClient();
//				ORTHOGONAL_PROJECTION_MATRIX.set(perspective(projectionMatrix.perspectiveFov(), (float) client.getWindow().getWidth() / client.getWindow().getHeight(), 0.05f, client.gameRenderer.getDepthFar()));
				ORTHOGONAL_PROJECTION_MATRIX.set(projectionMatrix);
			}
		}
		public static Matrix4f perspective(final float fovy, final float aspect, final float near, final float far)
		{
			float y2 = near * (float) Math.tan(fovy / 2f);
			float y1 = -y2;
			float x1 = y1 * aspect;
			float x2 = y2 * aspect;
			return frustum(x1, x2, y1, y2, near, far);
		}
		public static Matrix4f frustum(final float left, final float right, final float bottom, final float top, final float near, final float far)
		{
/*
			return new Matrix4f(
				2 * near / (right - left), 0.0f, 0.0f, 0.0f,   //X column
				0.0f, 2 * near / (top - bottom), 0.0f, 0.0f,   //Y column
				(right + left) / (right - left), (top + bottom) / (top - bottom), (near + far) / (near - far), -1.0f,  //Z column
				0.0f, 0.0f, 2 * near * far / (near - far), 0.0f    //Z column
			);
*/
			return new Matrix4f(
				2 * near / (right - left), 0.0f, 0.0f, 0.0f,   //X column
				0.0f, 2 * near / (top - bottom), 0.0f, 0.0f,   //Y column
				0, 0, 2 * near / (far - near), 0.0f,  //Z column
				0.0f, 0.0f, 0, 2f    //Z column
			);
		}
	}
	private static class CustomRenderTypes extends RenderType
	{
		private static ShaderInstance entitySilhouetteShader;
		private static ShaderInstance particleInkFrontShader;
		private static final ShaderStateShard RENDERTYPE_ENTITY_SILHOUETTE_SHADER = new ShaderStateShard(() -> entitySilhouetteShader);
		// Dummy constructor needed to make java happy
		private CustomRenderTypes(String s, VertexFormat v, VertexFormat.Mode m, int i, boolean b, boolean b2, Runnable r, Runnable r2)
		{
			super(s, v, m, i, b, b2, r, r2);
			throw new IllegalStateException("This class is not meant to be constructed!");
		}
		public static Function<ResourceLocation, RenderType> ENTITY_SILHOUETTE = Util.memoize(CustomRenderTypes::entitySilhouette);
		private static RenderType entitySilhouette(ResourceLocation location)
		{
			RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
				.setShaderState(RENDERTYPE_ENTITY_SILHOUETTE_SHADER)
				.setTextureState(new RenderStateShard.TextureStateShard(location, false, false))
				.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
				.setLightmapState(NO_LIGHTMAP)
				.setOverlayState(NO_OVERLAY)
				.setDepthTestState(GREATER_DEPTH_TEST)
				.setWriteMaskState(COLOR_WRITE)
				.createCompositeState(true);
			return create("splatcraft_entity_silhouette", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false, rendertype$state);
		}
		public static ShaderInstance getParticleInkFrontShader()
		{
			return particleInkFrontShader;
		}
	}
	public record WrappedSilhouetteMultiBufferSource(MultiBufferSource orginalBufferSource,
	                                                 float strength) implements MultiBufferSource
	{
		@Override
		public @NotNull VertexConsumer getBuffer(@NotNull RenderType renderType)
		{
			if (renderType instanceof RenderType.CompositeRenderType compositeRenderType &&
				renderType.format() == DefaultVertexFormat.NEW_ENTITY)
			{
				ResourceLocation location = compositeRenderType.state.textureState.cutoutTexture().get();
				return new WrappedSilhouetteVertexConsumer(getSilhouetteBuffer(location), strength);
			}
			return orginalBufferSource.getBuffer(renderType);
		}
		public @NotNull VertexConsumer getSilhouetteBuffer(@NotNull ResourceLocation location)
		{
			return orginalBufferSource.getBuffer(entitySilhouette(location));
		}
	}
	public record WrappedSilhouetteVertexConsumer(VertexConsumer original, float strength) implements VertexConsumer
	{
		@Override
		public @NotNull VertexConsumer addVertex(float v, float v1, float v2)
		{
			return original.addVertex(v, v1, v2);
		}
		@Override
		public @NotNull VertexConsumer setColor(int i, int i1, int i2, int i3)
		{
			float alpha = Mth.lerp(i3 / 255f, strength, strength);
			return original.setColor(i, i1, i2, (int) (alpha * 255f));
		}
		@Override
		public @NotNull VertexConsumer setUv(float v, float v1)
		{
			return original.setUv(v, v1);
		}
		@Override
		public @NotNull VertexConsumer setUv1(int i, int i1)
		{
			return original.setUv1(i, i1);
		}
		@Override
		public @NotNull VertexConsumer setUv2(int i, int i1)
		{
			return original.setUv2(i, i1);
		}
		@Override
		public @NotNull VertexConsumer setNormal(float v, float v1, float v2)
		{
			return original.setNormal(v, v1, v2);
		}
	}
}
