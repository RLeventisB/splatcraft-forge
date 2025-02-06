package net.splatcraft.client.particles;

import net.minecraft.client.particle.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SquidSoulParticle extends SpriteBillboardParticle
{
	private final SpriteProvider spriteProvider;
	public SquidSoulParticle(ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ, SquidSoulParticleData data, SpriteProvider sprite)
	{
		super(world, x, y, z, motionX, motionY, motionZ);
		
		red = Math.max(0.018f, data.getRed() - 0.018f);
		green = Math.max(0.018f, data.getGreen() - 0.018f);
		blue = Math.max(0.018f, data.getBlue() - 0.018f);
		
		gravityStrength = 0.15f;
		maxAge = 20;
		scale = 0.3f;
		collidesWithWorld = false;
		
		spriteProvider = sprite;
	}
	@Override
	public void tick()
	{
		prevPosX = x;
		prevPosY = y;
		prevPosZ = z;
		
		if (age++ >= maxAge)
		{
			markDead();
		}
		else
		{
			velocityY += 0.04f * gravityStrength;
			move(0, velocityY, 0);
			velocityY *= 0.98F;
		}
	}
	@Override
	protected int getBrightness(float tint)
	{
		return 0xf000f0;
	}
	@Override
	protected void method_60374(VertexConsumer vertexConsumer, Quaternionf rotation, float x, float y, float z, float tickDelta)
	{
		Vector3f[] vertexPositions = new Vector3f[]
			{
				new Vector3f(1.0F, -1.0F, 0.0F),
				new Vector3f(1.0F, 1.0F, 0.0F),
				new Vector3f(-1.0F, 1.0F, 0.0F),
				new Vector3f(-1.0F, -1.0F, 0.0F)
			};
		float size = getSize(tickDelta);
		
		for (int corner = 0; corner < 4; ++corner)
		{
			Vector3f uv = vertexPositions[corner];
			vertexPositions[corner] = rotation.transform(uv, new Vector3f()).mul(size).add(x, y, z);
		}
		
		for (int layer = 0; layer < 3; layer++)
		{
			float r = layer == 1 ? red : 1;
			float g = layer == 1 ? green : 1;
			float b = layer == 1 ? blue : 1;
			float a = alpha;
			if (age > maxAge - 5)
			{
				a = (1f - Math.max(0, age - maxAge + 5) - tickDelta) * 0.2f;
			}
			
			setSprite(spriteProvider.getSprite(layer + 1, 3));
			
			float minU = getMinU();
			float maxU = getMaxU();
			float minV = getMinV();
			float maxV = getMaxV();
			int brightness = 15728880;
			
			vertexConsumer.vertex(vertexPositions[0].x(), vertexPositions[0].y(), vertexPositions[0].z()).texture(maxU, maxV).color(r, g, b, a).light(brightness);
			vertexConsumer.vertex(vertexPositions[1].x(), vertexPositions[1].y(), vertexPositions[1].z()).texture(maxU, minV).color(r, g, b, a).light(brightness);
			vertexConsumer.vertex(vertexPositions[2].x(), vertexPositions[2].y(), vertexPositions[2].z()).texture(minU, minV).color(r, g, b, a).light(brightness);
			vertexConsumer.vertex(vertexPositions[3].x(), vertexPositions[3].y(), vertexPositions[3].z()).texture(minU, maxV).color(r, g, b, a).light(brightness);
		}
	}
	@Override
	public @NotNull ParticleTextureSheet getType()
	{
		return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
	}
	public static class Factory implements ParticleFactory<SquidSoulParticleData>
	{
		private final SpriteProvider spriteSet;
		public Factory(SpriteProvider sprite)
		{
			spriteSet = sprite;
		}
		@Nullable
		@Override
		public Particle createParticle(@NotNull SquidSoulParticleData typeIn, @NotNull ClientWorld levelIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
		{
			return new SquidSoulParticle(levelIn, x, y, z, xSpeed, ySpeed, zSpeed, typeIn, spriteSet);
		}
	}
}
