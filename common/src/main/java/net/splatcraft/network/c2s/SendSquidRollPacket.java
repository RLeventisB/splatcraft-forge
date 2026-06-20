package net.splatcraft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateSquidRollPacket;
import net.splatcraft.platform.Components;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

public class SendSquidRollPacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SendSquidRollPacket.class);
	private static final StreamCodec<RegistryFriendlyByteBuf, SendSquidRollPacket> STREAM_CODEC = StreamCodec.composite(
		CodecUtils.Codecs.VECTOR2F_STREAM_CODEC, v -> v.inputDirection,
		ByteBufCodecs.BOOL, v -> v.rollRepeated,
		SendSquidRollPacket::new
	);
	private final Vector2f inputDirection;
	private final boolean rollRepeated;
	public SendSquidRollPacket(Vector2f inputDirection, boolean rollRepeated)
	{
		this.inputDirection = inputDirection;
		this.rollRepeated = rollRepeated;
	}
	public static SendSquidRollPacket decode(RegistryFriendlyByteBuf buffer)
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
		Components.SQUID_INFO.updateOrCreate(target, info -> info.doSquidRoll(target, inputDirection, true));

		SplatcraftPacketHandler.sendToTrackers(new UpdateSquidRollPacket(target, inputDirection, rollRepeated), target);
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		STREAM_CODEC.encode(buffer, this);
	}
}
