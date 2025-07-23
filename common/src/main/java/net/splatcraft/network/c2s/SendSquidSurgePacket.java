package net.splatcraft.network.c2s;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.data.capabilities.structs.EntityInfo;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateSquidSurgePacket;
import net.splatcraft.platform.Components;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class SendSquidSurgePacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SendSquidSurgePacket.class);
	private static final StreamCodec<RegistryFriendlyByteBuf, SendSquidSurgePacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.optional(Direction.STREAM_CODEC), v -> v.climbedDirection,
		ByteBufCodecs.FLOAT, v -> v.squidSurgeCharge,
		SendSquidSurgePacket::new
	);
	private final Optional<Direction> climbedDirection;
	private final float squidSurgeCharge;
	public SendSquidSurgePacket(Optional<Direction> climbedDirection, float squidSurgeCharge)
	{
		this.climbedDirection = climbedDirection;
		this.squidSurgeCharge = squidSurgeCharge;
	}
	public static SendSquidSurgePacket decode(RegistryFriendlyByteBuf buffer)
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
		EntityInfo info = Components.ENTITY_INFO.getOrCreate(target);
		info.setClimbedDirection(climbedDirection);
		info.setSquidSurgeState(squidSurgeCharge);
		
		SplatcraftPacketHandler.sendToTrackers(new UpdateSquidSurgePacket(target, squidSurgeCharge, climbedDirection), target);
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		STREAM_CODEC.encode(buffer, this);
	}
}
