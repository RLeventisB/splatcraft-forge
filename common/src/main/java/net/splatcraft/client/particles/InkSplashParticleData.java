package net.splatcraft.client.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.dynamic.Codecs;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.registries.SplatcraftParticleTypes;
import net.splatcraft.util.ColorUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class InkSplashParticleData implements ParticleEffect
{
    public static final MapCodec<InkSplashParticleData> CODEC = RecordCodecBuilder.mapCodec((instance) ->
        instance.group(
            Codec.FLOAT.fieldOf("r").forGetter(InkSplashParticleData::getRed),
            Codec.FLOAT.fieldOf("g").forGetter(InkSplashParticleData::getGreen),
            Codec.FLOAT.fieldOf("b").forGetter(InkSplashParticleData::getBlue),
            Codecs.POSITIVE_FLOAT.fieldOf("scale").forGetter(InkSplashParticleData::getScale)
        ).apply(instance, InkSplashParticleData::new));
    public static final PacketCodec<RegistryByteBuf, InkSplashParticleData> PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.FLOAT, InkSplashParticleData::getRed,
        PacketCodecs.FLOAT, InkSplashParticleData::getBlue,
        PacketCodecs.FLOAT, InkSplashParticleData::getGreen,
        PacketCodecs.FLOAT, InkSplashParticleData::getScale,
        InkSplashParticleData::new);
    protected final float red;
    protected final float green;
    protected final float blue;
    protected final float scale;

    public InkSplashParticleData(Integer color, float scale)
    {
        this(ColorUtils.hexToRGB(color), scale);
    }

    private InkSplashParticleData(float[] rgb, float scale)
    {
        this(rgb[0], rgb[1], rgb[2], scale);
    }

    public InkSplashParticleData(float red, float green, float blue, float scale)
    {
        if (SplatcraftConfig.Client.colorLock.get())
        {
            float[] rgb = ColorUtils.hexToRGB(ColorUtils.getLockedColor(ColorUtils.RGBtoHex(new float[]{red, green, blue})));
            red = rgb[0];
            green = rgb[1];
            blue = rgb[2];
        }

        this.red = red;
        this.green = green;
        this.blue = blue;
        this.scale = scale;
    }

    @Override
    public @NotNull ParticleType<?> getType()
    {
        return SplatcraftParticleTypes.INK_SPLASH;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer)
    {
        buffer.writeFloat(red);
        buffer.writeFloat(green);
        buffer.writeFloat(blue);
        buffer.writeFloat(scale);
    }

    @Override
    public @NotNull String writeToString()
    {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.2f", ForgeRegistries.PARTICLE_TYPES.getKey(this.getType()), this.red, this.green, this.blue, this.scale);
    }

    @OnlyIn(Dist.CLIENT)
    public float getRed()
    {
        return this.red;
    }

    @OnlyIn(Dist.CLIENT)
    public float getGreen()
    {
        return this.green;
    }

    @OnlyIn(Dist.CLIENT)
    public float getBlue()
    {
        return this.blue;
    }

    @OnlyIn(Dist.CLIENT)
    public float getScale()
    {
        return this.scale;
    }
}
