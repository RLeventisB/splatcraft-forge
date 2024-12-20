package net.splatcraft.network.s2c;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.data.capabilities.playerinfo.EntityInfo;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;

import java.util.UUID;

public class UpdatePlayerInfoPacket extends PlayS2CPacket
{
    private static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(UpdatePlayerInfoPacket.class);
    UUID target;
    NbtCompound nbt;

    protected UpdatePlayerInfoPacket(UUID player, NbtCompound nbt)
    {
        target = player;
        this.nbt = nbt;
    }

    public UpdatePlayerInfoPacket(PlayerEntity target)
    {
        this(target.getUuid(), EntityInfoCapability.get(target).writeNBT(new NbtCompound(), target.getWorld().getRegistryManager()));
    }

    public static UpdatePlayerInfoPacket decode(RegistryByteBuf buffer)
    {
        return new UpdatePlayerInfoPacket(UUID.fromString(buffer.readString()), buffer.readNbt());
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeString(target.toString());
        buffer.writeNbt(nbt);
    }

    @Override
    public void execute()
    {
        PlayerEntity target = MinecraftClient.getInstance().world.getPlayerByUuid(this.target);

        if (target != null)
        {
            EntityInfo playerInfo = EntityInfoCapability.get(target);
            playerInfo.readNBT(nbt, target.getRegistryManager());
            ClientUtils.setClientPlayerColor(this.target, playerInfo.getColor());
        }
    }
}
