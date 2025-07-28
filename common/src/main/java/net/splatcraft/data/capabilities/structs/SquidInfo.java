package net.splatcraft.data.capabilities.structs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.splatcraft.handlers.SquidFormHandler.SquidState;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;

import java.util.Objects;
import java.util.Optional;

public record SquidInfo(
	InkColor color,
	boolean isSquid,
	Optional<Direction> climbedDirection,
	float squidSurgeState,
	SquidState squidState
)
{
	public static final float MIN_SQUID_SURGE_CHARGE = 7;
	public static final float MAX_SQUID_SURGE_CHARGE = 20;
	public static final float SQUID_SURGE_USAGE_OFFSET = 100;
	public static final float SQUID_SURGE_ENDLAG = 20;
	public static final MapCodec<SquidInfo> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
		InkColor.RAW_INT_CODEC.optionalFieldOf("color", InkColor.INVALID).forGetter(SquidInfo::color),
		Codec.BOOL.optionalFieldOf("is_squid", false).forGetter(SquidInfo::isSquid),
		Direction.CODEC.optionalFieldOf("climbed_direction").forGetter(SquidInfo::climbedDirection),
		Codec.FLOAT.optionalFieldOf("squid_surge_charge", 0f).forGetter(SquidInfo::squidSurgeState),
		SquidState.CODEC.optionalFieldOf("squid_state", SquidState.SURFACED).forGetter(SquidInfo::squidState)
	).apply(inst, SquidInfo::new));
	public static final Codec<SquidInfo> CODEC = MAP_CODEC.codec();
	public SquidInfo()
	{
		this(ColorUtils.getRandomStarterColor());
	}
	public SquidInfo(InkColor defaultColor)
	{
		this(defaultColor, false, Optional.empty(), 0f, SquidState.SURFACED);
	}
	public SquidInfo withColor(InkColor color)
	{
		if (Objects.equals(this.color, color))
			return this;
		return new SquidInfo(color, isSquid, climbedDirection, squidSurgeState, squidState);
	}
	public SquidInfo setSquid(boolean isSquid)
	{
		if (Objects.equals(this.isSquid, isSquid))
			return this;
		return new SquidInfo(color, isSquid, climbedDirection, squidSurgeState, squidState);
	}
	public SquidInfo setClimbedDirection(Direction direction)
	{
		return setClimbedDirection(Optional.ofNullable(direction));
	}
	public SquidInfo setClimbedDirection(Optional<Direction> climbedDirection)
	{
		if (Objects.equals(this.climbedDirection, climbedDirection))
			return this;
		return new SquidInfo(color, isSquid, climbedDirection, squidSurgeState, squidState);
	}
	public SquidInfo setSquidSurgeState(float squidSurgeState)
	{
		if (Objects.equals(this.squidSurgeState, squidSurgeState))
			return this;
		return new SquidInfo(color, isSquid, climbedDirection, squidSurgeState, squidState);
	}
	public SquidInfo withSquidState(SquidState squidState)
	{
		if (Objects.equals(this.squidState, squidState))
			return this;
		return new SquidInfo(color, isSquid, climbedDirection, squidSurgeState, squidState);
	}
	public boolean isDoingSquidSurge()
	{
		return squidSurgeState >= SQUID_SURGE_USAGE_OFFSET;
	}
	public boolean canChargeSquidSurge()
	{
		return squidSurgeState >= 0 && squidSurgeState < SQUID_SURGE_USAGE_OFFSET;
	}
	public float getSquidSurgeCharge()
	{
		return squidSurgeState > SQUID_SURGE_USAGE_OFFSET ? 0 : squidSurgeState;
	}
	public float getSquidSurgePower()
	{
		return squidSurgeState - SQUID_SURGE_USAGE_OFFSET;
	}
	public SquidInfo chargeSquidSurge()
	{
		if (squidSurgeState < MAX_SQUID_SURGE_CHARGE)
			return setSquidSurgeState(Math.min(squidSurgeState + 1, MAX_SQUID_SURGE_CHARGE));
		return this;
	}
	public SquidInfo flagSquidSurgeUsage()
	{
		if (squidSurgeState < MIN_SQUID_SURGE_CHARGE)
			return setSquidSurgeState(0);
		
		return setSquidSurgeState(SQUID_SURGE_USAGE_OFFSET + squidSurgeState);
	}
	public SquidInfo flagSquidSurgeEnd()
	{
		return setSquidSurgeState(-SQUID_SURGE_ENDLAG);
	}
}
