package net.splatcraft.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.platform.Services;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.Function;

// code based from this gist https://gist.github.com/gigaherz/b8756ff463541f07a644ef8f14cb10f5
// thank u gigaherz for doing the guide!!!
@OnlyIn(Dist.CLIENT)
public class SplatcraftRenderTypes
{
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
	}
	public static RenderType entitySilhouette(ResourceLocation texture)
	{
		return CustomRenderTypes.ENTITY_SILHOUETTE.apply(texture);
	}
	private static class CustomRenderTypes extends RenderType
	{
		private static ShaderInstance entitySilhouetteShader;
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
				.setCullState(NO_CULL)
				.setDepthTestState(RenderStateShard.GREATER_DEPTH_TEST)
				.createCompositeState(true);
			return create("splatcraft_entity_silhouette", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false, rendertype$state);
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
