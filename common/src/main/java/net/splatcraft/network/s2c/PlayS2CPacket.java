package net.splatcraft.network.s2c;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.splatcraft.network.SplatcraftPacket;

import java.util.function.Supplier;

public abstract class PlayS2CPacket extends SplatcraftPacket
{
    @Override
    public void consume(Supplier<NetworkEvent.Context> ctx)
    {
        if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT)
        {
            ctx.get().enqueueWork(this::execute);
        }
        ctx.get().setPacketHandled(true);
    }

    public abstract void execute();
}
