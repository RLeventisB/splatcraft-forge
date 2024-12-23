package net.splatcraft.network.s2c;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import net.splatcraft.network.SplatcraftPacket;

public abstract class PlayS2CPacket extends SplatcraftPacket
{
	@Override
	public void consume(NetworkManager.PacketContext ctx)
	{
		if (ctx.getEnvironment() == Env.CLIENT)
		{
			ctx.queue(this::execute);
		}
//        ctx.get().setPacketHandled(true);
	}
	public abstract void execute();
}
