package net.splatcraft.forge.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.forge.util.PlayerCooldown;

import java.util.UUID;

public class WeaponUseEndPacket extends PlayC2SPacket
{
    UUID target;

    public WeaponUseEndPacket(UUID target)
    {
        this.target = target;
    }

    public static WeaponUseEndPacket decode(FriendlyByteBuf buffer)
    {
        return new WeaponUseEndPacket(buffer.readUUID());
    }

    @Override
    public void execute(Player player)
    {
        Player target = player.level().getPlayerByUUID(this.target);
        PlayerCooldown.setCooldownTime(target, 1);
        PlayerCooldown.setPlayerCooldown(player, null);
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeUUID(target);
    }
}
