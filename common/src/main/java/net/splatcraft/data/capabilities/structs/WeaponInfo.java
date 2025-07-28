package net.splatcraft.data.capabilities.structs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.splatcraft.util.EntityStoredCharge;

import java.util.Objects;
import java.util.Optional;

public record WeaponInfo(
	int dodgeCount,
	int higherStartupTicks,
	Optional<EntityStoredCharge> storedCharge
)
{
	public static final int SQUID_LAG_DURATION = 10;
	public static final Codec<WeaponInfo> CODEC = RecordCodecBuilder.create(
		inst -> inst.group(
			Codec.INT.optionalFieldOf("dodge_count", 0).forGetter(WeaponInfo::dodgeCount),
			Codec.INT.optionalFieldOf("higher_startup_ticks", 0).forGetter(WeaponInfo::higherStartupTicks),
			EntityStoredCharge.CODEC.optionalFieldOf("stored_charge").forGetter(WeaponInfo::storedCharge)
		).apply(inst, WeaponInfo::new)
	);
	public WeaponInfo()
	{
		this(0, 0, Optional.empty());
	}
	public WeaponInfo withDodgeCount(int dodgeCount)
	{
		if (Objects.equals(this.dodgeCount, dodgeCount))
			return this;
		return new WeaponInfo(dodgeCount, higherStartupTicks, storedCharge);
	}
	public WeaponInfo withHigherStartup(int higherStartupTicks)
	{
		if (Objects.equals(this.higherStartupTicks, higherStartupTicks))
			return this;
		return new WeaponInfo(dodgeCount, higherStartupTicks, storedCharge);
	}
	public WeaponInfo withStoredCharge(Optional<EntityStoredCharge> storedCharge)
	{
		if (Objects.equals(this.storedCharge, storedCharge))
			return this;
		return new WeaponInfo(dodgeCount, higherStartupTicks, storedCharge);
	}
	public WeaponInfo withStoredCharge(EntityStoredCharge storedCharge)
	{
		return withStoredCharge(Optional.ofNullable(storedCharge));
	}
	public WeaponInfo flagSquidCancel()
	{
		return flagSquidCancel(SQUID_LAG_DURATION);
	}
	public WeaponInfo flagSquidCancel(int frames)
	{
		return withHigherStartup(frames);
	}
	public WeaponInfo resetHigherStartup()
	{
		return withHigherStartup(0);
	}
	public WeaponInfo reduceSquidAnimationTick()
	{
		if (higherStartupTicks > 0)
		{
			return withHigherStartup(higherStartupTicks - 1);
		}
		return this;
	}
	public boolean hasHigherStartup()
	{
		return higherStartupTicks > 0;
	}
}
