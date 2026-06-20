package net.splatcraft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateSquidRollDataPacket;
import net.splatcraft.platform.Components;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

public class SendSquidRollDataPacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SendSquidRollDataPacket.class);
	private static final StreamCodec<RegistryFriendlyByteBuf, SendSquidRollDataPacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BYTE, v -> v.rollLeniencyTime,
		ByteBufCodecs.BYTE, v -> v.rollRepeatTime,
		ByteBufCodecs.BOOL, v -> v.isRollRepeated,
		SendSquidRollDataPacket::new
	);
	private final byte rollLeniencyTime;
	private final byte rollRepeatTime;
	private final boolean isRollRepeated;
	public SendSquidRollDataPacket(byte rollLeniencyTime, byte rollRepeatTime, boolean isRollRepeated)
	{
		this.rollLeniencyTime = rollLeniencyTime;
		this.rollRepeatTime = rollRepeatTime;
		this.isRollRepeated = isRollRepeated;
	}
	public static SendSquidRollDataPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return STREAM_CODEC.decode(buffer);
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void execute(ServerPlayer target)
	{
		Components.SQUID_INFO.updateOrCreate(target, info -> info.setRollLeniencyTime(rollLeniencyTime).setRollRepeatPunishmentTime(rollRepeatTime).setIsRollRepeated(isRollRepeated));

		SplatcraftPacketHandler.sendToTrackers(new UpdateSquidRollDataPacket(target, rollLeniencyTime, rollRepeatTime, isRollRepeated), target);
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		STREAM_CODEC.encode(buffer, this);
	}
}
