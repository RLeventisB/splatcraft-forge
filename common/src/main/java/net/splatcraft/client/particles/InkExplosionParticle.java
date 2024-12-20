package net.splatcraft.client.particles;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkExplosionParticle extends SpriteBillboardParticle
{
    private final SpriteProvider spriteProvider;

    public InkExplosionParticle(ClientWorld level, double x, double y, double z, double motionX, double motionY, double motionZ, InkExplosionParticleData data, SpriteProvider provider)
    {
        super(level, x, y, z, motionX, motionY, motionZ);

        velocityX = motionX;
        velocityY = motionY;
        velocityZ = motionZ;

        setColor(Math.max(0.018f, data.getRed() - 0.018f),
            Math.max(0.018f, data.getGreen() - 0.018f),
            Math.max(0.018f, data.getBlue() - 0.018f));

        scale = 0.33F * (random.nextFloat() * 0.5F + 0.5F) * 2.0F * data.getScale();
        gravityStrength = 0;
        maxAge = 6 + random.nextInt(4);

        spriteProvider = provider;
        setSpriteForAge(provider);
    }

    @Override
    public void tick()
    {
        prevPosX = x;
        prevPosY = y;
        prevPosZ = z;
        if (age++ >= maxAge || world.getBlockState(new BlockPos((int) x, (int) y, (int) z)).isLiquid())
        {
            markDead();
        }
        else
        {
            setSpriteForAge(spriteProvider);
        }
    }

    @Override
    public ParticleTextureSheet getType()
    {
        return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<InkExplosionParticleData>
    {
        private final SpriteProvider provider;

        public Factory(SpriteProvider sprite)
        {
            provider = sprite;
        }

        @Nullable
        @Override
        public Particle createParticle(@NotNull InkExplosionParticleData typeIn, @NotNull ClientWorld levelIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
        {
            return new InkExplosionParticle(levelIn, x, y, z, xSpeed, ySpeed, zSpeed, typeIn, provider);
        }
    }
}
