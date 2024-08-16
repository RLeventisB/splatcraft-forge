package net.splatcraft.forge.network.c2s;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

// this is basically ServerboundPlayerInputPacket but it ignores if youre riding a vehicle
public class SquidInputPacket extends PlayC2SPacket
{
    public final float xxa;
    public final float zza;
    public final boolean isJumping;

    public SquidInputPacket(LocalPlayer player)
    {
        this(player.xxa, player.zza, player.input.jumping);
    }

    public SquidInputPacket(float xxa, float zza, boolean jumping)
    {
        this.xxa = xxa;
        this.zza = zza;
        this.isJumping = jumping;
    }

    @Override
    public void execute(Player target)
    {
        target.xxa = xxa;
        target.zza = zza;

        target.setJumping(isJumping);
    }

    public static SquidInputPacket decode(FriendlyByteBuf buffer)
    {
        return new SquidInputPacket(buffer.readFloat(), buffer.readFloat(), buffer.readByte() == 1);
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeFloat(this.xxa);
        buffer.writeFloat(this.zza);
        buffer.writeByte(this.isJumping ? 1 : 0);
    }
}
