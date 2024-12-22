package net.splatcraft.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.PlayerCharge;

public class UpdateChargeStatePacket extends PlayC2SPacket
{
    public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(UpdateChargeStatePacket.class);
    private final boolean hasCharge;

    public UpdateChargeStatePacket(boolean hasCharge)
    {
        this.hasCharge = hasCharge;
    }

    public static UpdateChargeStatePacket decode(RegistryByteBuf buffer)
    {
        return new UpdateChargeStatePacket(buffer.readBoolean());
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeBoolean(hasCharge);
    }

    @Override
    public void execute(PlayerEntity player)
    {
        PlayerCharge.updateServerMap(player, hasCharge);
    }
}
