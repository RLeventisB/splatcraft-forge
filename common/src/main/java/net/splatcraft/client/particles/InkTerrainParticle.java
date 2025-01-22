package net.splatcraft.client.particles;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class InkTerrainParticle extends SpriteBillboardParticle
{
	public InkTerrainParticle(ClientWorld p_108282_, double p_108283_, double p_108284_, double p_108285_, double p_108286_, double p_108287_, double p_108288_, float r, float g, float b)
	{
		this(p_108282_, p_108283_, p_108284_, p_108285_, p_108286_, p_108287_, p_108288_, CommonUtils.createBlockPos(p_108283_, p_108284_, p_108285_), r, g, b);
	}
	public InkTerrainParticle(ClientWorld level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, BlockPos blockPos, float r, float g, float b)
	{
		super(level, x, y, z, xSpeed, ySpeed, zSpeed);
		setSprite(MinecraftClient.getInstance().getBlockRenderManager().getModels().getModelParticleSprite(SplatcraftBlocks.inkedBlock.get().getDefaultState()));
		gravityStrength = 1.0F;
		red = 0.6F * r;
		green = 0.6F * g;
		blue = 0.6F * b;
		
		scale /= 2.0F;
	}
	public @NotNull ParticleTextureSheet getType()
	{
		return ParticleTextureSheet.TERRAIN_SHEET;
	}
	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<InkTerrainParticleData>
	{
		public Factory(SpriteProvider sprite)
		{
		}
		@Nullable
		@Override
		public Particle createParticle(InkTerrainParticleData typeIn, @NotNull ClientWorld levelIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
		{
			return new InkTerrainParticle(levelIn, x, y, z, xSpeed, ySpeed, zSpeed, typeIn.red, typeIn.green, typeIn.blue);
		}
	}
}

