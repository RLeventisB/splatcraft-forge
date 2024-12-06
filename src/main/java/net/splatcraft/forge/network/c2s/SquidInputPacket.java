package net.splatcraft.forge.network.c2s;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfo;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;

import java.util.Optional;

// this is basically ServerboundPlayerInputPacket but it ignores if youre riding a vehicle
public class SquidInputPacket extends PlayC2SPacket
{
    private final Optional<Direction> climbedDirection;
    private final float squidSurgeCharge;

    public SquidInputPacket(Optional<Direction> climbedDirection, float squidSurgeCharge)
    {
        this.climbedDirection = climbedDirection;
        this.squidSurgeCharge = squidSurgeCharge;
    }

    public static SquidInputPacket decode(FriendlyByteBuf buffer)
    {
        byte index = buffer.readByte();
        return new SquidInputPacket(index == Byte.MAX_VALUE ? Optional.empty() : Optional.of(Direction.from3DDataValue(index)), buffer.readFloat());
    }

    @Override
    public void execute(Player target)
    {
        PlayerInfo playerInfo = PlayerInfoCapability.get(target);
        playerInfo.setClimbedDirection(climbedDirection.orElse(null));
        playerInfo.setSquidSurgeCharge(squidSurgeCharge);
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {
        if (climbedDirection.isPresent())
            buffer.writeByte(this.climbedDirection.get().get3DDataValue());
        else
            buffer.writeByte(Byte.MAX_VALUE);
        buffer.writeFloat(this.squidSurgeCharge);
    }
}
