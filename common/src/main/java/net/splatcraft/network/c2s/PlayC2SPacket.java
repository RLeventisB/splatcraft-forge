package net.splatcraft.network.c2s;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.network.SplatcraftPacket;

public abstract class PlayC2SPacket extends SplatcraftPacket
{
	@Override
	public <T extends SplatcraftPacket> void consume(PacketContext<T> ctx)
	{
		if (ctx.side() == Side.SERVER)
		{
			execute(ctx.sender());
		}
//        ctx.get().setPacketHandled(true);
	}
	public abstract void execute(Player player);
}
