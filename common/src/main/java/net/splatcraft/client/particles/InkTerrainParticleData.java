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
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.registries.SplatcraftParticleTypes;
import net.splatcraft.util.ColorUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class InkTerrainParticleData implements ParticleEffect
{
    public static final MapCodec<InkTerrainParticleData> CODEC = RecordCodecBuilder.mapCodec((instance) ->
        instance.group(
            Codec.FLOAT.fieldOf("r").forGetter(InkTerrainParticleData::getRed),
            Codec.FLOAT.fieldOf("g").forGetter(InkTerrainParticleData::getGreen),
            Codec.FLOAT.fieldOf("b").forGetter(InkTerrainParticleData::getBlue)
        ).apply(instance, InkTerrainParticleData::new));
    public static final PacketCodec<RegistryByteBuf, InkTerrainParticleData> PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.FLOAT, InkTerrainParticleData::getRed,
        PacketCodecs.FLOAT, InkTerrainParticleData::getBlue,
        PacketCodecs.FLOAT, InkTerrainParticleData::getGreen,
        InkTerrainParticleData::new);
    protected final float red;
    protected final float green;
    protected final float blue;

    public InkTerrainParticleData(int color)
    {
        this(ColorUtils.hexToRGB(color));
    }

    private InkTerrainParticleData(float[] rgb)
    {
        this(rgb[0], rgb[1], rgb[2]);
    }

    public InkTerrainParticleData(float red, float green, float blue)
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
    }

    @Override
    public @NotNull ParticleType<?> getType()
    {
        return SplatcraftParticleTypes.INK_TERRAIN;
    }

    @Override
    public @NotNull String toString()
    {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f", Registries.PARTICLE_TYPE.getKey(getType()), this.red, this.green, this.blue);
    }

    public float getRed()
    {
        return this.red;
    }

    public float getGreen()
    {
        return this.green;
    }

    public float getBlue()
    {
        return this.blue;
    }
}
