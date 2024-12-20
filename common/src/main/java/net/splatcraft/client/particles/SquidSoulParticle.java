package net.splatcraft.client.particles;

import net.minecraft.client.particle.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
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
            velocityY += 0.04D * (double) gravityStrength;
            move(0, velocityY, 0);
            velocityY *= 0.98F;
        }
    }

    @Override
    public @NotNull ParticleTextureSheet getType()
    {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta)
    {
        Vec3d renderPos = camera.getPos();
        float lvt_5_1_ = (float) (MathHelper.lerp(tickDelta, prevPosX, x) - renderPos.getX());
        float lvt_6_1_ = (float) (MathHelper.lerp(tickDelta, prevPosY, y) - renderPos.getY());
        float lvt_7_1_ = (float) (MathHelper.lerp(tickDelta, prevPosZ, z) - renderPos.getZ());
        Quaternionf rotation = new Quaternionf();
//        if (this.angle == 0.0F)
//        {
//            rotation = camera.getRotation();
//        }
//        else
//        {
//            rotation = new Quaternionf(camera.getRotation());
//            float lvt_9_1_ = MathHelper.lerp(tickDelta, this.angle, this.prevAngle);
//            rotation.mul(RotationAxis.POSITIVE_Z.rotation(lvt_9_1_));
//        }
        getRotator().setRotation(rotation, camera, tickDelta);
        if (angle != 0.0F)
        {
            rotation.rotateZ(MathHelper.lerp(tickDelta, prevAngle, angle));
        }

        Vector3f lvt_9_2_ = new Vector3f(-1.0F, -1.0F, 0.0F);
        lvt_9_2_ = rotation.transform(lvt_9_2_);
        Vector3f[] lvt_10_1_ = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
        float lvt_11_1_ = getSize(tickDelta);

        for (int lvt_12_1_ = 0; lvt_12_1_ < 4; ++lvt_12_1_)
        {
            Vector3f lvt_13_1_ = lvt_10_1_[lvt_12_1_];
            lvt_13_1_ = rotation.transform(lvt_13_1_);
            lvt_13_1_.mul(lvt_11_1_);
            lvt_13_1_.add(lvt_5_1_, lvt_6_1_, lvt_7_1_);
        }

        for (int i = 0; i < 3; i++)
        {
            float r = i == 1 ? red : 1;
            float g = i == 1 ? green : 1;
            float b = i == 1 ? blue : 1;
            float a = alpha;
            if (age > maxAge - 5)
            {
                a = (1f - Math.max(0, age - maxAge + 5) - tickDelta) * 0.2f;
            }

            setSprite(spriteProvider.getSprite(i + 1, 3));

            float lvt_12_2_ = getMinU();
            float lvt_13_2_ = getMaxU();
            float lvt_14_1_ = getMinV();
            float lvt_15_1_ = getMaxV();
            int lvt_16_1_ = 15728880;//this.getBrightnessForRender(partialTicks);

            vertexConsumer.vertex(lvt_10_1_[0].x(), lvt_10_1_[0].y(), lvt_10_1_[0].z()).texture(lvt_13_2_, lvt_15_1_).color(r, g, b, a).color(lvt_16_1_);
            vertexConsumer.vertex(lvt_10_1_[1].x(), lvt_10_1_[1].y(), lvt_10_1_[1].z()).texture(lvt_13_2_, lvt_14_1_).color(r, g, b, a).color(lvt_16_1_);
            vertexConsumer.vertex(lvt_10_1_[2].x(), lvt_10_1_[2].y(), lvt_10_1_[2].z()).texture(lvt_12_2_, lvt_14_1_).color(r, g, b, a).color(lvt_16_1_);
            vertexConsumer.vertex(lvt_10_1_[3].x(), lvt_10_1_[3].y(), lvt_10_1_[3].z()).texture(lvt_12_2_, lvt_15_1_).color(r, g, b, a).color(lvt_16_1_);
        }
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
