package net.splatcraft.network.c2s;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.util.CommonUtils;

import java.util.Optional;

// this is basically ServerboundPlayerInputPacket but it ignores if youre riding a vehicle
public class SquidInputPacket extends PlayC2SPacket
{
    public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SquidInputPacket.class);
    private final Optional<Direction> climbedDirection;
    private final float squidSurgeCharge;

    public SquidInputPacket(Optional<Direction> climbedDirection, float squidSurgeCharge)
    {
        this.climbedDirection = climbedDirection;
        this.squidSurgeCharge = squidSurgeCharge;
    }

    public static SquidInputPacket decode(RegistryFriendlyByteBuf buffer)
    {
        byte index = buffer.readByte();
        return new SquidInputPacket(index == Byte.MAX_VALUE ? Optional.empty() : Optional.of(Direction.from3DDataValue(index)), buffer.readFloat());
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return ID;
    }

    @Override
    public void execute(Player target)
    {
        EntityInfo playerInfo = EntityInfoCapability.get(target);
        playerInfo.setClimbedDirection(climbedDirection.orElse(null));
        playerInfo.setSquidSurgeCharge(squidSurgeCharge);
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer)
    {
        if (climbedDirection.isPresent())
            buffer.writeByte(climbedDirection.get().get3DDataValue());
        else
            buffer.writeByte(Byte.MAX_VALUE);
        buffer.writeFloat(squidSurgeCharge);
    }
}
