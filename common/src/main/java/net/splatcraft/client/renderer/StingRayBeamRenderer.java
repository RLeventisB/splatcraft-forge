package net.splatcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.splatcraft.Splatcraft;
import net.splatcraft.entities.StingRayBeamEntity;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Arrays;

public class StingRayBeamRenderer extends EntityRenderer<StingRayBeamEntity>
{
	private static final ResourceLocation MAGIC_PIXEL = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
	private static final ResourceLocation SHOCKWAVE_TEXTURE = Splatcraft.identifierOf("textures/entity/special/sting_ray_beam_shockwave.png");
	private static final float RAY_LENGTH = 1024f;
	public StingRayBeamRenderer(EntityRendererProvider.Context context)
	{
		super(context);
	}
	private static Vector3f getRotationVector(float pitch, float yaw)
	{
		float f = pitch * Mth.DEG_TO_RAD;
		float g = -yaw * Mth.DEG_TO_RAD;
		float h = Mth.cos(g);
		float i = Mth.sin(g);
		float j = Mth.cos(f);
		float k = Mth.sin(f);
		return new Vector3f(i * j, -k, h * j);
	}
	@Override
	public void render(StingRayBeamEntity entity, float entityYaw, float partialTicks, @NotNull PoseStack matrixStack, @NotNull MultiBufferSource buffer, int packedLight)
	{
		float lifespan = entity.getLifespan() + partialTicks;
		float worldTimeMod10 = Mth.positiveModulo(lifespan / 100f, 10);
		InkColor color = entity.getColor();
		byte state = entity.getState();

		switch (state)
		{
			case 0:
			{
				float progress = lifespan / entity.getStartup();

				VertexConsumer builder = buffer.getBuffer(RenderType.entitySolid(getTextureLocation(entity)));
				renderBeam(builder, matrixStack, entity, 4, 0f, 10f, entity.getRayWidth(), FastColor.ARGB32.lerp(0.8f, 0, color.getColorWithAlpha(64)), partialTicks, 0, 1f);

				builder = buffer.getBuffer(RenderType.entityTranslucent(SHOCKWAVE_TEXTURE));
				renderBeam(builder, matrixStack, entity, 8, 0f, 1f, Mth.lerp((float) Math.pow(progress, 1.3f), 3f, entity.getRayWidth()), FastColor.ARGB32.lerp(0.8f, 0, color.getColorWithAlpha(32)), partialTicks, worldTimeMod10 * 12.8f, 1f);
			}
			break;
			case 1:
			{
				VertexConsumer builder = buffer.getBuffer(RenderType.entitySolid(getTextureLocation(entity)));
				renderBeam(builder, matrixStack, entity, 4, 0f, 10f, entity.getRayWidth(), color.getColorWithAlpha(255), partialTicks, 0, 1f);
			}

			break;
			case 2:
			{
				float progress = Math.min(1f, (lifespan - entity.getShockwaveDelay()) / 10f);

				VertexConsumer builder = buffer.getBuffer(RenderType.entitySolid(getTextureLocation(entity)));
				renderBeam(builder, matrixStack, entity, 4, 0f, 10f, entity.getRayWidth(), color.getColorWithAlpha(255), partialTicks, 0, 1f);

				builder = buffer.getBuffer(RenderType.entityTranslucent(SHOCKWAVE_TEXTURE));
				int colorRGB = color.getColorWithAlpha((int) (progress * progress * 72));
				colorRGB = FastColor.ARGB32.lerp(progress * 0.7f, -1, colorRGB);
				float zOffset = -worldTimeMod10 * 32.0f;
				float roll = worldTimeMod10 * 500;
				renderBeam(builder, matrixStack, entity, 8, roll, 1f, entity.getShockwaveWidth(), colorRGB, partialTicks, zOffset, 0.4f);
			}
			break;
		}
	}
	public void renderBeam(VertexConsumer builder, PoseStack stack, StingRayBeamEntity entity, final int sides, final float roll, final float firstRingDistance, final float beamWidth, final int color, final float partialTicks, float vOffset, float vScale)
	{
		renderBeam(builder, stack, entity, (byte) sides, roll, firstRingDistance, beamWidth, color, partialTicks, vOffset, vScale);
	}
	public void renderBeam(VertexConsumer builder, PoseStack stack, StingRayBeamEntity entity, final byte sides, final float roll, final float firstRingDistance, final float beamWidth, final int color, final float partialTicks, float vOffset, float vScale)
	{
		final Vector3f ZERO = new Vector3f();

		float pitch = entity.getViewXRot(partialTicks);
		float yaw = entity.getViewYRot(partialTicks);

		Vector3f forward = getRotationVector(pitch, yaw);

		Vector3f ringForward = forward.mul(firstRingDistance, new Vector3f());
		Vector3f endForward = forward.mul(RAY_LENGTH, new Vector3f());

		Vector3f[] directions = new Vector3f[sides];
		for (int i = 0; i < sides; i++)
		{
			directions[i] = getRotationVector(pitch + roll + ((float) i / sides) * 360f, yaw - 90).mul(beamWidth);
		}

		Vector3f[] firstRingPoints = Arrays.stream(directions).map(v -> ringForward.add(v, new Vector3f())).toArray(Vector3f[]::new);
		Vector3f[] endPoints = Arrays.stream(directions).map(v -> endForward.add(v, new Vector3f())).toArray(Vector3f[]::new);

		for (byte i = 0; i < sides; i++)
		{
			byte nextI = (byte) ((i + 1) % sides);
			drawVertices(ZERO, ZERO, firstRingPoints[i], firstRingPoints[nextI], builder, stack, color, vOffset * vScale, (firstRingDistance + vOffset) * vScale);
			drawVertices(firstRingPoints[i], firstRingPoints[nextI], endPoints[i], endPoints[nextI], builder, stack, color, (firstRingDistance + vOffset) * vScale, (RAY_LENGTH + vOffset) * vScale);
		}
	}
	private void drawVertices(Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4, VertexConsumer builder, PoseStack stack, final int color, final float minV, final float maxV)
	{
		builder.addVertex(stack.last(), p1)
			.setColor(color)
			.setUv(0, minV)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setUv2(0, 240)
			.setNormal(0.0F, 1.0F, 0.0F)
		;
		builder.addVertex(stack.last(), p2)
			.setColor(color)
			.setUv(0, minV)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setUv2(0, 240)
			.setNormal(0.0F, 1.0F, 0.0F)
		;
		builder.addVertex(stack.last(), p4)
			.setColor(color)
			.setUv(0, maxV)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setUv2(0, 240)
			.setNormal(0.0F, 1.0F, 0.0F)
		;
		builder.addVertex(stack.last(), p3)
			.setColor(color)
			.setUv(0, maxV)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setUv2(0, 240)
			.setNormal(0.0F, 1.0F, 0.0F)
		;
	}
	@Override
	public boolean shouldRender(@NotNull StingRayBeamEntity entity, @NotNull Frustum frustum, double x, double y, double z)
	{
		// testLineSegment sometimes is buggy :(
//		return frustum.frustumIntersection.testLineSegment(entity.getPos().toVector3f(), entity.getPos().add(entity.getRotationVector().multiply(RAY_LENGTH)).toVector3f());
		return true;
	}
	@Override
	public @NotNull ResourceLocation getTextureLocation(@NotNull StingRayBeamEntity entity)
	{
		return MAGIC_PIXEL;
	}
}