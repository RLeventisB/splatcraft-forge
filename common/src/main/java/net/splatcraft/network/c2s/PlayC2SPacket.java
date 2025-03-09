package net.splatcraft.network.c2s;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.network.SplatcraftPacket;

public abstract class PlayC2SPacket extends SplatcraftPacket
{
	@Override
	public <T extends SplatcraftPacket> void consume(NetworkManager.PacketContext ctx)
	{
		if (ctx.getEnvironment() == Env.SERVER)
		{
			ctx.queue(() -> execute(ctx.getPlayer()));
		}
//        ctx.get().setPacketHandled(true);
	}
	public abstract void execute(Player player);
}
