package net.splatcraft.network;

import commonnetwork.networking.data.PacketContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public abstract class SplatcraftPacket implements CustomPacketPayload
{
	public SplatcraftPacket()
	{
	
	}
	public abstract void encode(RegistryFriendlyByteBuf buffer);
	public abstract <T extends SplatcraftPacket> void consume(PacketContext<T> ctx);
}
