package net.splatcraft.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.splatcraft.data.Stage;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.SendStageWarpDataToPadPacket;
import net.splatcraft.util.CommonUtils;

public class RequestUpdateStageSpawnPadsPacket extends PlayC2SPacket
{
    public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(RequestUpdateStageSpawnPadsPacket.class);
    final String stageId;

    public RequestUpdateStageSpawnPadsPacket(String stageId)
    {
        this.stageId = stageId;
    }

    public RequestUpdateStageSpawnPadsPacket(Stage stage)
    {
        this(stage.id);
    }

    public static RequestUpdateStageSpawnPadsPacket decode(RegistryByteBuf buffer)
    {
        return new RequestUpdateStageSpawnPadsPacket(buffer.readString());
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
        Stage.getStage(player.getWorld(), stageId).updateSpawnPads(player.getWorld());
        SplatcraftPacketHandler.sendToPlayer(SendStageWarpDataToPadPacket.compile(player), (ServerPlayerEntity) player);
    }
}