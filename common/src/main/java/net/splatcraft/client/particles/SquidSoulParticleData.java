package net.splatcraft.client.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.splatcraft.registries.SplatcraftParticleTypes;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class SquidSoulParticleData implements ParticleEffect
{
    public static final MapCodec<SquidSoulParticleData> CODEC = RecordCodecBuilder.mapCodec((instance) ->
        instance.group(
            Codec.FLOAT.fieldOf("r").forGetter(SquidSoulParticleData::getRed),
            Codec.FLOAT.fieldOf("g").forGetter(SquidSoulParticleData::getGreen),
            Codec.FLOAT.fieldOf("b").forGetter(SquidSoulParticleData::getBlue)
        ).apply(instance, SquidSoulParticleData::new));
    public static final PacketCodec<RegistryByteBuf, SquidSoulParticleData> PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.FLOAT, SquidSoulParticleData::getRed,
        PacketCodecs.FLOAT, SquidSoulParticleData::getBlue,
        PacketCodecs.FLOAT, SquidSoulParticleData::getGreen,
        SquidSoulParticleData::new);
    protected final float red;
    protected final float green;
    protected final float blue;

    public SquidSoulParticleData(InkColor color)
    {
        this(ColorUtils.getColorLockedIfConfig(color).getRGB());
    }

    private SquidSoulParticleData(float[] rgb)
    {
        this(rgb[0], rgb[1], rgb[2]);
    }

    public SquidSoulParticleData(float red, float green, float blue)
    {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    @Override
    public @NotNull ParticleType<?> getType()
    {
        return SplatcraftParticleTypes.SQUID_SOUL;
    }

    @Override
    public String toString()
    {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f", Registries.PARTICLE_TYPE.getKey(getType()), red, green, blue);
    }

    public float getRed()
    {
        return red;
    }

    public float getGreen()
    {
        return green;
    }

    public float getBlue()
    {
        return blue;
    }
}
