package net.splatcraft.network.s2c;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.splatcraft.data.InkColorRegistry;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkColor;

public class SendColorRegistryPacket extends PlayS2CPacket
{
	public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(SendColorRegistryPacket.class);
	private static final PacketCodec<RegistryByteBuf, BiMap<Identifier, InkColor>> PACKET_CODEC = PacketCodecs.map(HashBiMap::create, Identifier.PACKET_CODEC, InkColor.PACKET_CODEC);
	private final BiMap<Identifier, InkColor> colors;
	public SendColorRegistryPacket(BiMap<Identifier, InkColor> colors)
	{
		this.colors = colors;
	}
	public static SendColorRegistryPacket decode(RegistryByteBuf buffer)
	{
		return new SendColorRegistryPacket(PACKET_CODEC.decode(buffer));
	}
	@Override
	public Id<? extends CustomPayload> getId()
	{
		return ID;
	}
	@Override
	public void encode(RegistryByteBuf buffer)
	{
		PACKET_CODEC.encode(buffer, colors);
	}
	@Environment(EnvType.CLIENT)
	@Override
	public void execute()
	{
		InkColorRegistry.REGISTRY.clear();
		InkColorRegistry.REGISTRY.putAll(colors);
	}
}