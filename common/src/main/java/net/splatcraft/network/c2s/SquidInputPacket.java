package net.splatcraft.network.c2s;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.platform.Components;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

// this is basically ServerboundPlayerInputPacket but it ignores if youre riding a vehicle
public class SquidInputPacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SquidInputPacket.class);
	private static final StreamCodec<RegistryFriendlyByteBuf, SquidInputPacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.optional(Direction.STREAM_CODEC), v -> v.climbedDirection,
		ByteBufCodecs.FLOAT, v -> v.squidSurgeCharge,
		SquidInputPacket::new
	);
	private final Optional<Direction> climbedDirection;
	private final float squidSurgeCharge;
	public SquidInputPacket(Optional<Direction> climbedDirection, float squidSurgeCharge)
	{
		this.climbedDirection = climbedDirection;
		this.squidSurgeCharge = squidSurgeCharge;
	}
	public static SquidInputPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return STREAM_CODEC.decode(buffer);
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void execute(Player target)
	{
		EntityInfo playerInfo = Components.ENTITY_INFO.getOrCreate(target);
		playerInfo.setClimbedDirection(climbedDirection.orElse(null));
		playerInfo.setSquidSurgeState(squidSurgeCharge);
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		STREAM_CODEC.encode(buffer, this);
	}
}
