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

        this.velocityX = motionX;
        this.velocityY = motionY;
        this.velocityZ = motionZ;

        this.setColor(Math.max(0.018f, data.getRed() - 0.018f),
            Math.max(0.018f, data.getGreen() - 0.018f),
            Math.max(0.018f, data.getBlue() - 0.018f));

        this.scale = 0.33F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F * data.getScale();
        this.gravityStrength = 0;
        this.maxAge = 6 + this.random.nextInt(4);

        spriteProvider = provider;
        this.setSpriteForAge(provider);
    }

    @Override
    public void tick()
    {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        if (this.age++ >= this.maxAge || world.getBlockState(new BlockPos((int) this.x, (int) this.y, (int) this.z)).isLiquid())
        {
            this.markDead();
        }
        else
        {
            this.setSpriteForAge(this.spriteProvider);
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
            this.provider = sprite;
        }

        @Nullable
        @Override
        public Particle createParticle(@NotNull InkExplosionParticleData typeIn, @NotNull ClientWorld levelIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
        {
            return new InkExplosionParticle(levelIn, x, y, z, xSpeed, ySpeed, zSpeed, typeIn, this.provider);
        }
    }
}
