package net.splatcraft.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.handlers.ShootingHandler;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.PlayerCooldown;

import java.util.UUID;

public class DodgeRollEndPacket extends PlayC2SPacket
{
    private static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(DodgeRollEndPacket.class);
    UUID target;

    public DodgeRollEndPacket(UUID target)
    {
        this.target = target;
    }

    public static DodgeRollEndPacket decode(RegistryByteBuf buffer)
    {
        return new DodgeRollEndPacket(buffer.readUuid());
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void execute(PlayerEntity player)
    {
        PlayerEntity target = player.getWorld().getPlayerByUuid(this.target);
        PlayerCooldown.setCooldownTime(target, 1);
        PlayerCooldown.setPlayerCooldown(player, null);
        EntityInfoCapability.get(player).setDodgeCount(0);

        if (ShootingHandler.isDoingShootingAction(player))
        {
            ShootingHandler.EntityData data = ShootingHandler.shootingData.get(player);
            data.recalculateFiringData();
        }
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeUuid(target);
    }
}
