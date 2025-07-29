package net.splatcraft.network.s2c;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.platform.Components;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public class UpdateSquidSurgePacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(UpdateSquidSurgePacket.class);
	private final float squidSurgeState;
	private final UUID uuid;
	private final Optional<Direction> climbedDirection;
	public UpdateSquidSurgePacket(@NotNull LivingEntity entity, float state, Optional<Direction> climbedDirection)
	{
		uuid = entity.getUUID();
		squidSurgeState = state;
		this.climbedDirection = climbedDirection;
	}
	public UpdateSquidSurgePacket(@NotNull UUID uuid, float state, Optional<Direction> climbedDirection)
	{
		this.uuid = uuid;
		squidSurgeState = state;
		this.climbedDirection = climbedDirection;
	}
	public static UpdateSquidSurgePacket decode(FriendlyByteBuf buf)
	{
		return new UpdateSquidSurgePacket(buf.readUUID(), buf.readFloat(), buf.readOptional(Direction.STREAM_CODEC));
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeUUID(uuid);
		buffer.writeFloat(squidSurgeState);
		buffer.writeOptional(climbedDirection, Direction.STREAM_CODEC);
	}
	@Override
	public void execute()
	{
		Player player = ClientUtils.getClientPlayer().level().getPlayerByUUID(uuid);
		if (player != null)
		{
			Components.SQUID_INFO.updateOrCreate(player, info -> info.setSquidSurgeState(squidSurgeState).setClimbedDirection(climbedDirection));
		}
	}
}
