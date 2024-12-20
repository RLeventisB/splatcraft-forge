package net.splatcraft.client.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.util.dynamic.Codecs;
import net.splatcraft.registries.SplatcraftParticleTypes;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
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

    public InkSplashParticleData(InkColor color, float scale)
    {
        this(ColorUtils.getLockedColor(color).getRGB(), scale);
    }

    private InkSplashParticleData(float[] rgb, float scale)
    {
        this(rgb[0], rgb[1], rgb[2], scale);
    }

    public InkSplashParticleData(float red, float green, float blue, float scale)
    {
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
    public @NotNull String toString()
    {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.2f", Registries.PARTICLE_TYPE.getKey(getType()), red, green, blue, scale);
    }

    @Environment(EnvType.CLIENT)
    public float getRed()
    {
        return red;
    }

    @Environment(EnvType.CLIENT)
    public float getGreen()
    {
        return green;
    }

    @Environment(EnvType.CLIENT)
    public float getBlue()
    {
        return blue;
    }

    @Environment(EnvType.CLIENT)
    public float getScale()
    {
        return scale;
    }
}
