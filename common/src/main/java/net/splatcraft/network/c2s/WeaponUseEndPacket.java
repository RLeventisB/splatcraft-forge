package net.splatcraft.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.PlayerCooldown;

import java.util.UUID;

public class WeaponUseEndPacket extends PlayC2SPacket
{
    public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(WeaponUseEndPacket.class);
    UUID target;

    public WeaponUseEndPacket(UUID target)
    {
        this.target = target;
    }

    public static WeaponUseEndPacket decode(RegistryByteBuf buffer)
    {
        return new WeaponUseEndPacket(buffer.readUuid());
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
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeUuid(target);
    }
}
