package net.splatcraft.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public abstract class SplatcraftPacket implements CustomPacketPayload
{
	public SplatcraftPacket()
	{
	
	}
	public abstract void encode(RegistryFriendlyByteBuf buffer);
	public abstract <T extends SplatcraftPacket> void consume(NetworkManager.PacketContext ctx);
}
