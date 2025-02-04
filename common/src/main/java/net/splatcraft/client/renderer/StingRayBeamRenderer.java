package net.splatcraft.client.renderer;

import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.splatcraft.Splatcraft;
import net.splatcraft.entities.StingRayBeamEntity;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Arrays;

public class StingRayBeamRenderer extends EntityRenderer<StingRayBeamEntity>
{
	private static final Identifier MAGIC_PIXEL = Identifier.ofVanilla("textures/misc/white.png");
	private static final Identifier SHOCKWAVE_TEXTURE = Splatcraft.identifierOf("textures/entity/special/sting_ray_beam_shockwave.png");
	private static final float RAY_LENGTH = 1024f;
	public StingRayBeamRenderer(EntityRendererFactory.Context context)
	{
		super(context);
	}
	private static Vector3f getRotationVector(float pitch, float yaw)
	{
		float f = pitch * MathHelper.RADIANS_PER_DEGREE;
		float g = -yaw * MathHelper.RADIANS_PER_DEGREE;
		float h = MathHelper.cos(g);
		float i = MathHelper.sin(g);
		float j = MathHelper.cos(f);
		float k = MathHelper.sin(f);
		return new Vector3f(i * j, -k, h * j);
	}
	@Override
	public void render(StingRayBeamEntity entity, float entityYaw, float partialTicks, @NotNull MatrixStack matrixStack, @NotNull VertexConsumerProvider buffer, int packedLight)
	{
		float lifespan = entity.getLifespan() + partialTicks;
		float worldTimeMod10 = Math.floorMod(entity.getWorld().getTime(), 10) + partialTicks;
		InkColor color = entity.getColor();
		byte state = entity.getState();
		
		switch (state)
		{
			case 0:
			{
				float progress = lifespan / entity.getStartup();
				
				VertexConsumer builder = buffer.getBuffer(RenderLayer.getBeaconBeam(getTexture(entity), true));
				renderBeam(builder, matrixStack, entity, 4, 0f, 10f, entity.getRayWidth(), ColorHelper.Argb.lerp(0.8f, 0, color.getColorWithAlpha(255)), partialTicks, 0);
				
				builder = buffer.getBuffer(RenderLayer.getBeaconBeam(SHOCKWAVE_TEXTURE, true));
				renderBeam(builder, matrixStack, entity, 8, 0f, 1f, MathHelper.lerp((float) Math.pow(progress, 1.3f), 3f, entity.getRayWidth()), ColorHelper.Argb.lerp(0.8f, 0, color.getColorWithAlpha(128)), partialTicks, worldTimeMod10 * 6.4f);
			}
			break;
			case 1:
			{
				VertexConsumer builder = buffer.getBuffer(RenderLayer.getBeaconBeam(getTexture(entity), false));
				renderBeam(builder, matrixStack, entity, 4, 0f, 10f, entity.getRayWidth(), color.getColorWithAlpha(255), partialTicks, 0);
			}
			
			break;
			case 2:
			{
				float progress = Math.min(1f, (lifespan - entity.getShockwaveDelay()) / 10f);
				
				VertexConsumer builder = buffer.getBuffer(RenderLayer.getBeaconBeam(getTexture(entity), false));
				renderBeam(builder, matrixStack, entity, 4, 0f, 10f, entity.getRayWidth(), color.getColorWithAlpha(255), partialTicks, 0);
				
				builder = buffer.getBuffer(RenderLayer.getBeaconBeam(SHOCKWAVE_TEXTURE, true));
				int colorRGB = color.getColorWithAlpha((int) (progress * progress * 128));
				colorRGB = ColorHelper.Argb.lerp(progress * 0.7f, -1, colorRGB);
				float zOffset = -worldTimeMod10 * 6.4f * 5f;
				float roll = worldTimeMod10 * 36;
				renderBeam(builder, matrixStack, entity, 8, roll, 1f, entity.getShockwaveWidth(), colorRGB, partialTicks, zOffset);
			}
			break;
		}
	}
	public void renderBeam(VertexConsumer builder, MatrixStack stack, StingRayBeamEntity entity, final int sides, final float roll, final float firstRingDistance, final float beamWidth, final int color, final float partialTicks, float vOffset)
	{
		renderBeam(builder, stack, entity, (byte) sides, roll, firstRingDistance, beamWidth, color, partialTicks, vOffset);
	}
	public void renderBeam(VertexConsumer builder, MatrixStack stack, StingRayBeamEntity entity, final byte sides, final float roll, final float firstRingDistance, final float beamWidth, final int color, final float partialTicks, float vOffset)
	{
		final Vector3f ZERO = new Vector3f();
		
		float pitch = entity.getPitch(partialTicks);
		float yaw = entity.getYaw(partialTicks);
		
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
			drawVertices(ZERO, ZERO, firstRingPoints[i], firstRingPoints[nextI], builder, stack, color, vOffset, firstRingDistance + vOffset);
			drawVertices(firstRingPoints[i], firstRingPoints[nextI], endPoints[i], endPoints[nextI], builder, stack, color, firstRingDistance + vOffset, RAY_LENGTH + vOffset);
		}
	}
	private void drawVertices(Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4, VertexConsumer builder, MatrixStack stack, final int color, final float minV, final float maxV)
	{
		builder.vertex(stack.peek(), p1)
			.color(color)
			.texture(0, minV)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(0, 240)
			.normal(0.0F, 1.0F, 0.0F)
		;
		builder.vertex(stack.peek(), p2)
			.color(color)
			.texture(0, minV)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(0, 240)
			.normal(0.0F, 1.0F, 0.0F)
		;
		builder.vertex(stack.peek(), p4)
			.color(color)
			.texture(0, maxV)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(0, 240)
			.normal(0.0F, 1.0F, 0.0F)
		;
		builder.vertex(stack.peek(), p3)
			.color(color)
			.texture(0, maxV)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(0, 240)
			.normal(0.0F, 1.0F, 0.0F)
		;
	}
	@Override
	public boolean shouldRender(StingRayBeamEntity entity, Frustum frustum, double x, double y, double z)
	{
		// testLineSegment sometimes is buggy :(
//		return frustum.frustumIntersection.testLineSegment(entity.getPos().toVector3f(), entity.getPos().add(entity.getRotationVector().multiply(RAY_LENGTH)).toVector3f());
		return true;
	}
	@Override
	public @NotNull Identifier getTexture(@NotNull StingRayBeamEntity entity)
	{
		return MAGIC_PIXEL;
	}
}