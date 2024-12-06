package net.splatcraft.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;

import java.util.UUID;

public class UpdatePlayerInfoPacket extends PlayS2CPacket
{
    UUID target;
    NbtCompound nbt;

    protected UpdatePlayerInfoPacket(UUID player, NbtCompound nbt)
    {
        this.target = player;
        this.nbt = nbt;
    }

    public UpdatePlayerInfoPacket(Player target)
    {
        this(target.getUUID(), PlayerInfoCapability.get(target).writeNBT(new NbtCompound()));
    }

    public static UpdatePlayerInfoPacket decode(FriendlyByteBuf buffer)
    {
        return new UpdatePlayerInfoPacket(UUID.fromString(buffer.readUtf()), buffer.readNbt());
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeUtf(target.toString());
        buffer.writeNbt(nbt);
    }

    @Override
    public void execute()
    {
        Player target = Minecraft.getInstance().level.getPlayerByUUID(this.target);

        if (target != null)
        {
            PlayerInfoCapability.get(target).readNBT(nbt);
            ClientUtils.setClientPlayerColor(this.target, ColorUtils.getColorFromNbt(this.nbt));
        }
    }
}
