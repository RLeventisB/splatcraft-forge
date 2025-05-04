package net.splatcraft.client.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SquidSoulParticle extends TextureSheetParticle
{
	private final SpriteSet spriteProvider;
	public SquidSoulParticle(ClientLevel world, double x, double y, double z, double motionX, double motionY, double motionZ, SquidSoulParticleData data, SpriteSet sprite)
	{
		super(world, x, y, z, motionX, motionY, motionZ);

		rCol = Math.max(0.018f, data.getRed() - 0.018f);
		gCol = Math.max(0.018f, data.getGreen() - 0.018f);
		bCol = Math.max(0.018f, data.getBlue() - 0.018f);

		gravity = 0.15f;
		lifetime = 20;
		quadSize = 0.3f;
		hasPhysics = false;

		spriteProvider = sprite;
	}
	@Override
	public void tick()
	{
		xo = x;
		yo = y;
		zo = z;

		if (age++ >= lifetime)
		{
			remove();
		}
		else
		{
			yd += 0.04f * gravity;
			move(0, yd, 0);
			yd *= 0.98F;
		}
	}
	@Override
	protected int getLightColor(float tint)
	{
		return 0xf000f0;
	}
	@Override
	protected void renderRotatedQuad(@NotNull VertexConsumer vertexConsumer, @NotNull Quaternionf rotation, float x, float y, float z, float tickDelta)
	{
		Vector3f[] vertexPositions = new Vector3f[]
			{
				new Vector3f(1.0F, -1.0F, 0.0F),
				new Vector3f(1.0F, 1.0F, 0.0F),
				new Vector3f(-1.0F, 1.0F, 0.0F),
				new Vector3f(-1.0F, -1.0F, 0.0F)
			};
		float size = getQuadSize(tickDelta);

		for (int corner = 0; corner < 4; ++corner)
		{
			Vector3f uv = vertexPositions[corner];
			vertexPositions[corner] = rotation.transform(uv, new Vector3f()).mul(size).add(x, y, z);
		}

		for (int layer = 0; layer < 3; layer++)
		{
			float r = layer == 1 ? rCol : 1;
			float g = layer == 1 ? gCol : 1;
			float b = layer == 1 ? bCol : 1;
			float a = alpha;
			if (age > lifetime - 5)
			{
				a = (1f - Math.max(0, age - lifetime + 5) - tickDelta) * 0.2f;
			}

			setSprite(spriteProvider.get(layer + 1, 3));

			float minU = getU0();
			float maxU = getU1();
			float minV = getV0();
			float maxV = getV1();
			int brightness = 15728880;

			vertexConsumer.addVertex(vertexPositions[0].x(), vertexPositions[0].y(), vertexPositions[0].z()).setUv(maxU, maxV).setColor(r, g, b, a).setLight(brightness);
			vertexConsumer.addVertex(vertexPositions[1].x(), vertexPositions[1].y(), vertexPositions[1].z()).setUv(maxU, minV).setColor(r, g, b, a).setLight(brightness);
			vertexConsumer.addVertex(vertexPositions[2].x(), vertexPositions[2].y(), vertexPositions[2].z()).setUv(minU, minV).setColor(r, g, b, a).setLight(brightness);
			vertexConsumer.addVertex(vertexPositions[3].x(), vertexPositions[3].y(), vertexPositions[3].z()).setUv(minU, maxV).setColor(r, g, b, a).setLight(brightness);
		}
	}
	@Override
	public @NotNull ParticleRenderType getRenderType()
	{
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}
	public static class Factory implements ParticleProvider<SquidSoulParticleData>
	{
		private final SpriteSet spriteSet;
		public Factory(SpriteSet sprite)
		{
			spriteSet = sprite;
		}
		@Nullable
		@Override
		public Particle createParticle(@NotNull SquidSoulParticleData typeIn, @NotNull ClientLevel levelIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
		{
			return new SquidSoulParticle(levelIn, x, y, z, xSpeed, ySpeed, zSpeed, typeIn, spriteSet);
		}
	}
}
