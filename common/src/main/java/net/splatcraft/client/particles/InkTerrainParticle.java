package net.splatcraft.client.particles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.registries.SplatcraftBlocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class InkTerrainParticle extends TextureSheetParticle
{
	public InkTerrainParticle(ClientLevel p_108282_, double p_108283_, double p_108284_, double p_108285_, double p_108286_, double p_108287_, double p_108288_, float r, float g, float b)
	{
		this(p_108282_, p_108283_, p_108284_, p_108285_, p_108286_, p_108287_, p_108288_, BlockPos.containing(p_108283_, p_108284_, p_108285_), r, g, b);
	}
	public InkTerrainParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, BlockPos blockPos, float r, float g, float b)
	{
		super(level, x, y, z, xSpeed, ySpeed, zSpeed);
		setSprite(Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(SplatcraftBlocks.inkedBlock.value().defaultBlockState()));
		gravity = 1.0F;
		rCol = 0.6F * r;
		gCol = 0.6F * g;
		bCol = 0.6F * b;

		quadSize /= 2.0F;
	}
	public @NotNull ParticleRenderType getRenderType()
	{
		return ParticleRenderType.TERRAIN_SHEET;
	}
	@OnlyIn(Dist.CLIENT)
	public static class Factory implements ParticleProvider<InkTerrainParticleData>
	{
		public Factory(SpriteSet sprite)
		{
		}
		@Nullable
		@Override
		public Particle createParticle(InkTerrainParticleData typeIn, @NotNull ClientLevel levelIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
		{
			return new InkTerrainParticle(levelIn, x, y, z, xSpeed, ySpeed, zSpeed, typeIn.red, typeIn.green, typeIn.blue);
		}
	}
}

