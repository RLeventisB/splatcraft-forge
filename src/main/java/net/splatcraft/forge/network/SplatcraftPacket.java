package net.splatcraft.forge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public abstract class SplatcraftPacket
{
    public abstract void encode(FriendlyByteBuf buffer);

    public abstract void consume(Supplier<NetworkEvent.Context> ctx);
}
