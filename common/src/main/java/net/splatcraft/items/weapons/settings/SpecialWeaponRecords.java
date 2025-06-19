package net.splatcraft.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.SplatcraftConvertors;

public class SpecialWeaponRecords
{
	public record StingRayDataRecord(
		byte startupTicks,
		byte shockwaveDelay,
		float mobilityOnUse,
		float damageCenter,
		float damageShockwave,
		float radiusCenter,
		float radiusShockwave,
		float turningValue,
		float turningValueWithShockwave,
		float revealRadius,
		float paintingRadius,
		float paintSearchRadius
	) implements DynamicDataRecord<StingRayDataRecord>
	{
		public static final MapCodec<StingRayDataRecord> CODEC = RecordCodecBuilder.mapCodec(
			inst -> inst.group(
				Codec.BYTE.fieldOf("startup_ticks").forGetter(StingRayDataRecord::startupTicks),
				Codec.BYTE.fieldOf("shockwave_delay").forGetter(StingRayDataRecord::shockwaveDelay),
				Codec.FLOAT.fieldOf("mobility_on_use").forGetter(StingRayDataRecord::mobilityOnUse),
				Codec.FLOAT.fieldOf("damage_center").forGetter(StingRayDataRecord::damageCenter),
				Codec.FLOAT.fieldOf("damage_shockwave").forGetter(StingRayDataRecord::damageShockwave),
				Codec.FLOAT.fieldOf("radius_center").forGetter(StingRayDataRecord::radiusCenter),
				Codec.FLOAT.fieldOf("radius_shockwave").forGetter(StingRayDataRecord::radiusShockwave),
				Codec.FLOAT.fieldOf("turning_value").forGetter(StingRayDataRecord::turningValue),
				Codec.FLOAT.fieldOf("turning_value_with_shockwave").forGetter(StingRayDataRecord::turningValueWithShockwave),
				Codec.FLOAT.fieldOf("reveal_radius").forGetter(StingRayDataRecord::revealRadius),
				Codec.FLOAT.fieldOf("painting_radius").forGetter(StingRayDataRecord::paintingRadius),
				Codec.FLOAT.optionalFieldOf("painting_search_radius", 1000f).forGetter(StingRayDataRecord::paintSearchRadius)
			).apply(inst, StingRayDataRecord::new)
		);
		public static final ResourceLocation ID = Splatcraft.identifierOf("sting_ray");
		@Override
		public StingRayDataRecord convertSelf()
		{
			return new StingRayDataRecord(
				(byte) (startupTicks / SplatcraftConvertors.SplatoonFramesPerMinecraftTick),
				(byte) (shockwaveDelay / SplatcraftConvertors.SplatoonFramesPerMinecraftTick),
				mobilityOnUse,
				damageCenter / SplatcraftConvertors.SplatoonHealthPerMinecraftHealth * SplatcraftConvertors.SplatoonFramesPerMinecraftTick,
				damageShockwave / SplatcraftConvertors.SplatoonHealthPerMinecraftHealth * SplatcraftConvertors.SplatoonFramesPerMinecraftTick,
				radiusCenter / SplatcraftConvertors.DistanceUnitsPerMinecraftSquare,
				radiusShockwave / SplatcraftConvertors.DistanceUnitsPerMinecraftSquare,
				turningValue,
				turningValueWithShockwave,
				revealRadius / SplatcraftConvertors.DistanceUnitsPerMinecraftSquare,
				paintingRadius / SplatcraftConvertors.DistanceUnitsPerMinecraftSquare,
				paintSearchRadius / SplatcraftConvertors.DistanceUnitsPerMinecraftSquare
			);
		}
	}
}
