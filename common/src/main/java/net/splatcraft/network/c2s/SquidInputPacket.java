package net.splatcraft.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.Direction;
import net.splatcraft.data.capabilities.playerinfo.EntityInfo;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.util.CommonUtils;

import java.util.Optional;

// this is basically ServerboundPlayerInputPacket but it ignores if youre riding a vehicle
public class SquidInputPacket extends PlayC2SPacket
{
    private static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(SquidInputPacket.class);
    private final Optional<Direction> climbedDirection;
    private final float squidSurgeCharge;

    public SquidInputPacket(Optional<Direction> climbedDirection, float squidSurgeCharge)
    {
        this.climbedDirection = climbedDirection;
        this.squidSurgeCharge = squidSurgeCharge;
    }

    public static SquidInputPacket decode(RegistryByteBuf buffer)
    {
        byte index = buffer.readByte();
        return new SquidInputPacket(index == Byte.MAX_VALUE ? Optional.empty() : Optional.of(Direction.byId(index)), buffer.readFloat());
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void execute(PlayerEntity target)
    {
        EntityInfo playerInfo = EntityInfoCapability.get(target);
        playerInfo.setClimbedDirection(climbedDirection.orElse(null));
        playerInfo.setSquidSurgeCharge(squidSurgeCharge);
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        if (climbedDirection.isPresent())
            buffer.writeByte(climbedDirection.get().getId());
        else
            buffer.writeByte(Byte.MAX_VALUE);
        buffer.writeFloat(squidSurgeCharge);
    }
}
