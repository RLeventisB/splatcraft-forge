package net.splatcraft.forge.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.util.PlayerCooldown;

import java.util.UUID;

public class DodgeRollEndPacket extends PlayC2SPacket
{
    UUID target;

    public DodgeRollEndPacket(UUID target)
    {
        this.target = target;
    }

    public static DodgeRollEndPacket decode(FriendlyByteBuf buffer)
    {
        return new DodgeRollEndPacket(buffer.readUUID());
    }

    @Override
    public void execute(Player player)
    {
        Player target = player.level().getPlayerByUUID(this.target);
        PlayerCooldown.setCooldownTime(target, 1);
        PlayerCooldown.setPlayerCooldown(player, null);
        PlayerInfoCapability.get(player).setDodgeCount(0);
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeUUID(target);
    }
}
