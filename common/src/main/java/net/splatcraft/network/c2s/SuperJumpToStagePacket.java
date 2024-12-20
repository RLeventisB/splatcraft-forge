package net.splatcraft.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.splatcraft.data.Stage;
import net.splatcraft.util.CommonUtils;

public class SuperJumpToStagePacket extends PlayC2SPacket
{
    private static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(SuperJumpToStagePacket.class);
    final String stageId;

    public SuperJumpToStagePacket(String stageId)
    {
        this.stageId = stageId;
    }

    public static SuperJumpToStagePacket decode(PacketByteBuf buf)
    {
        return new SuperJumpToStagePacket(buf.readString());
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeString(stageId);
    }

    @Override
    public void execute(PlayerEntity player)
    {
        Stage.getStage(player.getWorld(), stageId).superJumpToStage((ServerPlayerEntity) player);
    }
}