package net.splatcraft.network.s2c;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkColor;

import java.util.TreeMap;
import java.util.UUID;

public class UpdateClientColorsPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(UpdateClientColorsPacket.class);
	public static StreamCodec<RegistryFriendlyByteBuf, TreeMap<UUID, InkColor>> COLORS_CODEC = ByteBufCodecs.map(v -> new TreeMap<>(), UUIDUtil.STREAM_CODEC, InkColor.PACKET_CODEC);
	final TreeMap<UUID, InkColor> colors;
	final boolean reset;
	protected UpdateClientColorsPacket(TreeMap<UUID, InkColor> playerColors, boolean reset)
	{
		colors = playerColors;
		this.reset = reset;
	}
	public UpdateClientColorsPacket(TreeMap<UUID, InkColor> colors)
	{
		this(colors, true);
	}
	public UpdateClientColorsPacket(UUID player, InkColor color)
	{
		colors = new TreeMap<>();
		colors.put(player, color);
		reset = false;
	}
	public static UpdateClientColorsPacket decode(RegistryFriendlyByteBuf buffer)
	{
		boolean reset = buffer.readBoolean();
		TreeMap<UUID, InkColor> colors = COLORS_CODEC.decode(buffer);
		
		return new UpdateClientColorsPacket(colors, reset);
	}
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void execute()
	{
		if (reset)
		{
			ClientUtils.resetClientColors();
		}
		ClientUtils.putClientColors(colors);
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeBoolean(reset);
		COLORS_CODEC.encode(buffer, colors);
	}
}