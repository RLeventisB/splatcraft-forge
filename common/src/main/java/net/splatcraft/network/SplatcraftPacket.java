package net.splatcraft.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;

public abstract class SplatcraftPacket implements CustomPayload
{
    public SplatcraftPacket(){
    
    }
    public abstract void encode(RegistryByteBuf buffer);

    public abstract void consume(NetworkManager.PacketContext ctx);
}
