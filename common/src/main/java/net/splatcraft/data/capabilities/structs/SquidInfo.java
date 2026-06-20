package net.splatcraft.data.capabilities.structs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.handlers.SquidFormHandler.SquidState;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.SendSquidRollPacket;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.SquidRollAction;
import net.splatcraft.util.structs.InkColor;
import org.joml.Vector2f;

import java.util.Objects;
import java.util.Optional;

public record SquidInfo(
	InkColor color,
	boolean isSquid,
	Optional<Direction> climbedDirection,
	float squidSurgeState,
	byte rollLeniencyTime,
	byte rollRepeatPunishmentTime,
	boolean isRollRepeated,
	SquidState squidState
)
{
	public static byte ROLL_LENIENCY_FRAMES = 3;
	public static byte ROLL_REPEAT_PUNISH_FRAMES = 25;
	public static float ROLL_REQUIRED_SPEED = 0.25f;
	public static float ROLL_MIN_ANGLE_DIFFERENCE = 60f * Mth.DEG_TO_RAD;
	public static float REPEATED_ROLL_PUNISHMENT = 0.85f;
	public static float ROLL_HORIZONTAL_SPEED = 0.25f;
	public static float ROLL_VERTICAL_SPEED = 0.45f;
	public static final float MIN_SQUID_SURGE_CHARGE = 7;
	public static final float MAX_SQUID_SURGE_CHARGE = 20;
	public static final float SQUID_SURGE_USAGE_OFFSET = 100;
	public static final float SQUID_SURGE_ENDLAG = 20;
	public static final MapCodec<SquidInfo> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
		InkColor.RAW_INT_CODEC.optionalFieldOf("color", InkColor.INVALID).forGetter(SquidInfo::color),
		Codec.BOOL.optionalFieldOf("is_squid", false).forGetter(SquidInfo::isSquid),
		Direction.CODEC.optionalFieldOf("climbed_direction").forGetter(SquidInfo::climbedDirection),
		Codec.FLOAT.optionalFieldOf("squid_surge_charge", 0f).forGetter(SquidInfo::squidSurgeState),
		Codec.BYTE.optionalFieldOf("roll_leniancy_time", (byte) 0).forGetter(SquidInfo::rollLeniencyTime),
		Codec.BYTE.optionalFieldOf("roll_repeat_punishment_time", (byte) 0).forGetter(SquidInfo::rollRepeatPunishmentTime),
		Codec.BOOL.optionalFieldOf("is_roll_repeated", false).forGetter(SquidInfo::isRollRepeated),
		SquidState.CODEC.optionalFieldOf("squid_state", SquidState.SURFACED).forGetter(SquidInfo::squidState)
	).apply(inst, SquidInfo::new));
	public static final Codec<SquidInfo> CODEC = MAP_CODEC.codec();
	public SquidInfo()
	{
		this(ColorUtils.getRandomStarterColor());
	}
	public SquidInfo(InkColor defaultColor)
	{
		this(defaultColor, false, Optional.empty(), 0f, (byte) 0, (byte) 0, false, SquidState.SURFACED);
	}
	public SquidInfo withColor(InkColor color)
	{
		if (Objects.equals(this.color, color))
			return this;
		return new SquidInfo(color, isSquid, climbedDirection, squidSurgeState, rollLeniencyTime, rollRepeatPunishmentTime, isRollRepeated, squidState);
	}
	public SquidInfo setSquid(boolean isSquid)
	{
		if (Objects.equals(this.isSquid, isSquid))
			return this;
		return new SquidInfo(color, isSquid, climbedDirection, squidSurgeState, rollLeniencyTime, rollRepeatPunishmentTime, isRollRepeated, squidState);
	}
	public SquidInfo setClimbedDirection(Direction direction)
	{
		return setClimbedDirection(Optional.ofNullable(direction));
	}
	public SquidInfo setClimbedDirection(Optional<Direction> climbedDirection)
	{
		if (Objects.equals(this.climbedDirection, climbedDirection))
			return this;
		return new SquidInfo(color, isSquid, climbedDirection, squidSurgeState, rollLeniencyTime, rollRepeatPunishmentTime, isRollRepeated, squidState);
	}
	public SquidInfo setSquidSurgeState(float squidSurgeState)
	{
		if (Objects.equals(this.squidSurgeState, squidSurgeState))
			return this;
		return new SquidInfo(color, isSquid, climbedDirection, squidSurgeState, rollLeniencyTime, rollRepeatPunishmentTime, isRollRepeated, squidState);
	}
	public SquidInfo setRollLeniencyTime(int rollLeniencyTime)
	{
		return setRollLeniencyTime((byte) rollLeniencyTime);
	}
	public SquidInfo setRollLeniencyTime(byte rollLeniencyTime)
	{
		if (Objects.equals(this.rollLeniencyTime, rollLeniencyTime))
			return this;
		return new SquidInfo(color, isSquid, climbedDirection, squidSurgeState, rollLeniencyTime, rollRepeatPunishmentTime, isRollRepeated, squidState);
	}
	public SquidInfo setRollRepeatPunishmentTime(int rollRepeatPunishmentTime)
	{
		return setRollRepeatPunishmentTime((byte) rollRepeatPunishmentTime);
	}
	public SquidInfo setRollRepeatPunishmentTime(byte rollRepeatPunishmentTime)
	{
		if (Objects.equals(this.rollRepeatPunishmentTime, rollRepeatPunishmentTime))
			return this;
		return new SquidInfo(color, isSquid, climbedDirection, squidSurgeState, rollLeniencyTime, rollRepeatPunishmentTime, isRollRepeated, squidState);
	}
	public SquidInfo setIsRollRepeated(boolean isRollRepeated)
	{
		if (Objects.equals(this.isRollRepeated, isRollRepeated))
			return this;
		return new SquidInfo(color, isSquid, climbedDirection, squidSurgeState, rollLeniencyTime, rollRepeatPunishmentTime, isRollRepeated, squidState);
	}
	public SquidInfo withSquidState(SquidState squidState)
	{
		if (Objects.equals(this.squidState, squidState))
			return this;
		return new SquidInfo(color, isSquid, climbedDirection, squidSurgeState, rollLeniencyTime, rollRepeatPunishmentTime, isRollRepeated, squidState);
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
		// cancel squid surge if there is not enough charge
		if (squidSurgeState < MIN_SQUID_SURGE_CHARGE)
			return setSquidSurgeState(0);

		return setSquidSurgeState(SQUID_SURGE_USAGE_OFFSET + squidSurgeState);
	}
	public SquidInfo flagSquidSurgeEnd()
	{
		return setSquidSurgeState(-SQUID_SURGE_ENDLAG);
	}
	public SquidInfo doSquidRoll(LivingEntity entity, Vector2f jumpDirection, boolean fromPacket)
	{
		boolean isLocalPlayer = entity instanceof Player player && player.isLocalPlayer();
		boolean clientSide = entity.level().isClientSide();
		if (isRollRepeated() && !fromPacket)
		{
			jumpDirection.mul(REPEATED_ROLL_PUNISHMENT);
		}

		if (clientSide)
			entity.setDeltaMovement(jumpDirection.x, ROLL_VERTICAL_SPEED, jumpDirection.y);
		entity.setOnGround(true);

		EntityAction.setEntityAction(entity, new SquidRollAction());

		if (clientSide && !fromPacket)
			SplatcraftPacketHandler.sendToServer(new SendSquidRollPacket(jumpDirection, isRollRepeated()));

		entity.level().playSound(null, entity, SplatcraftSounds.squidRollJump, SoundSource.PLAYERS, 0.8F,
			CommonUtils.nextTriangular(entity.level().getRandom(), 0.95f, 0.095f));

		return setIsRollRepeated(true).setRollRepeatPunishmentTime(ROLL_REPEAT_PUNISH_FRAMES);
	}
}
