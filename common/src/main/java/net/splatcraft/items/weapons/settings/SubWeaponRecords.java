package net.splatcraft.items.weapons.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.splatcraft.util.DamageRangesRecord;

public class SubWeaponRecords
{
	public record ThrowableExplodingSubDataRecord(
		DamageRangesRecord damageRanges,
		float inkSplashRadius,
		int fuseTime,
		float throwVelocity,
		float throwAngle
	)
	{
		public static final Codec<ThrowableExplodingSubDataRecord> CODEC = RecordCodecBuilder.create(
			inst -> inst.group(
				DamageRangesRecord.CODEC.fieldOf("damage_ranges").forGetter(ThrowableExplodingSubDataRecord::damageRanges),
				Codec.FLOAT.fieldOf("ink_splash_radius").forGetter(ThrowableExplodingSubDataRecord::inkSplashRadius),
				Codec.INT.fieldOf("fuse_time").forGetter(ThrowableExplodingSubDataRecord::fuseTime),
				Codec.FLOAT.fieldOf("throw_velocity").forGetter(ThrowableExplodingSubDataRecord::throwVelocity),
				Codec.FLOAT.fieldOf("throw_angle").forGetter(ThrowableExplodingSubDataRecord::throwAngle)
			).apply(inst, ThrowableExplodingSubDataRecord::new)
		);
	}
	public record BurstBombDataRecord(
		DamageRangesRecord damageRanges,
		float inkSplashRadius,
		float contactDamage,
		float throwVelocity,
		float throwAngle
	)
	{
		public static final Codec<BurstBombDataRecord> CODEC = RecordCodecBuilder.create(
			inst -> inst.group(
				DamageRangesRecord.CODEC.fieldOf("damage_ranges").forGetter(BurstBombDataRecord::damageRanges),
				Codec.FLOAT.fieldOf("ink_splash_radius").forGetter(BurstBombDataRecord::inkSplashRadius),
				Codec.FLOAT.fieldOf("contact_damage").forGetter(BurstBombDataRecord::contactDamage),
				Codec.FLOAT.fieldOf("throw_velocity").forGetter(BurstBombDataRecord::throwVelocity),
				Codec.FLOAT.fieldOf("throw_angle").forGetter(BurstBombDataRecord::throwAngle)
			).apply(inst, BurstBombDataRecord::new)
		);
	}
}
