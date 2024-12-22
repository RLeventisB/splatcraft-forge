package net.splatcraft.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.splatcraft.items.JumpLureItem;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class UseJumpLurePacket extends PlayC2SPacket
{
    public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(UseJumpLurePacket.class);
    @Nullable
    final UUID targetUUID;
    final InkColor color;
    public UseJumpLurePacket(InkColor color, @Nullable UUID targetUUID)
    {
        this.targetUUID = targetUUID;
        this.color = color;
    }

    public static UseJumpLurePacket decode(PacketByteBuf buf)
    {
        return new UseJumpLurePacket(InkColor.constructOrReuse(buf.readInt()), buf.readBoolean() ? null : buf.readUuid());
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeInt(color.getColor());
        buffer.writeBoolean(targetUUID == null);
        if (targetUUID != null)
            buffer.writeUuid(targetUUID);
    }

    @Override
    public void execute(PlayerEntity player)
    {
        JumpLureItem.activate((ServerPlayerEntity) player, targetUUID, color);
    }
}
