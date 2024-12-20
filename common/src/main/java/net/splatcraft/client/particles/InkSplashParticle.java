package net.splatcraft.client.particles;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.splatcraft.util.ClientUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkSplashParticle extends SpriteBillboardParticle
{
    private final SpriteProvider spriteProvider;

    public InkSplashParticle(ClientWorld level, double x, double y, double z, double motionX, double motionY, double motionZ, InkSplashParticleData data, SpriteProvider sprite)
    {
        super(level, x, y, z, motionX, motionY, motionZ);

        velocityX = motionX;
        velocityY = motionY;
        velocityZ = motionZ;

        red = Math.max(0.018f, data.getRed() - 0.018f);
        green = Math.max(0.018f, data.getGreen() - 0.018f);
        blue = Math.max(0.018f, data.getBlue() - 0.018f);

        scale = 0.33F * (random.nextFloat() * 0.5F + 0.5F) * 2.0F * data.getScale();
        gravityStrength = 0;//0.1f;
        maxAge = 5;

        spriteProvider = sprite;
        setSpriteForAge(sprite);
    }

    @Override
    public void tick()
    {
        super.tick();
        if (gravityStrength > 0)
        {
            velocityY -= 0.004D + 0.04D * (double) gravityStrength;
        }
        if (world.getBlockState(new BlockPos((int) x, (int) y, (int) z)).isLiquid())
        {
            markDead();
        }
        else
        {
            setSpriteForAge(spriteProvider);
        }
    }

    @Override
    public @NotNull ParticleTextureSheet getType()
    {
        return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public void buildGeometry(@NotNull VertexConsumer buffer, @NotNull Camera renderInfo, float partialTicks)
    {
        boolean firstPerson = MinecraftClient.getInstance().options.getPerspective().isFirstPerson();
        if (firstPerson)
        {
            double dist = (squaredDistanceTo(ClientUtils.getClientPlayer(), x, y, z));
            if (dist < 4)
            {
                if (dist < 1)
                    return;
                setAlpha((float) (Math.sqrt(dist) - 1));
                super.buildGeometry(buffer, renderInfo, partialTicks);
            }
            else
                super.buildGeometry(buffer, renderInfo, partialTicks);
        }
        else
            super.buildGeometry(buffer, renderInfo, partialTicks);
    }

    protected double squaredDistanceTo(Entity entity, double x, double y, double z)
    {
        double d0 = entity.getX() - x;
        double d1 = entity.getEyeY() - y;
        double d2 = entity.getZ() - z;
        return d0 * d0 + d1 * d1 + d2 * d2;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<InkSplashParticleData>
    {
        private final SpriteProvider spriteSet;

        public Factory(SpriteProvider sprite)
        {
            spriteSet = sprite;
        }

        @Nullable
        @Override
        public Particle createParticle(@NotNull InkSplashParticleData typeIn, @NotNull ClientWorld levelIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
        {
            return new InkSplashParticle(levelIn, x, y, z, xSpeed, ySpeed, zSpeed, typeIn, spriteSet);
        }
    }
}
