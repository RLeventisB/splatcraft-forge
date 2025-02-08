package net.splatcraft.network.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkColor;

import java.util.TreeMap;
import java.util.UUID;

public class UpdateClientColorsPacket extends PlayS2CPacket
{
	public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(UpdateClientColorsPacket.class);
	public static PacketCodec<RegistryByteBuf, TreeMap<UUID, InkColor>> COLORS_CODEC = PacketCodecs.map(v -> new TreeMap<>(), Uuids.PACKET_CODEC, InkColor.PACKET_CODEC);
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
	public static UpdateClientColorsPacket decode(RegistryByteBuf buffer)
	{
		boolean reset = buffer.readBoolean();
		TreeMap<UUID, InkColor> colors = COLORS_CODEC.decode(buffer);
		
		return new UpdateClientColorsPacket(colors, reset);
	}
	@Override
	public Id<? extends CustomPayload> getId()
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
	public void encode(RegistryByteBuf buffer)
	{
		buffer.writeBoolean(reset);
		COLORS_CODEC.encode(buffer, colors);
	}
}