package net.splatcraft.network.s2c;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.splatcraft.network.SplatcraftPacket;

public abstract class PlayS2CPacket extends SplatcraftPacket
{
	@Override
	public <T extends SplatcraftPacket> void consume(PacketContext<T> ctx)
	{
		if (ctx.side() == Side.CLIENT)
		{
			execute();
		}
//        ctx.get().setPacketHandled(true);
	}
	public abstract void execute();
}
