package net.splatcraft.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.splatcraft.data.Stage;
import net.splatcraft.items.remotes.InkDisruptorItem;
import net.splatcraft.util.CommonUtils;

public class RequestClearInkPacket extends PlayC2SPacket
{
    private static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(RequestClearInkPacket.class);
    final String stageId;

    public RequestClearInkPacket(String stageId)
    {
        this.stageId = stageId;
    }

    public static RequestClearInkPacket decode(RegistryByteBuf buffer)
    {
        return new RequestClearInkPacket(buffer.readString());
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
        Stage stage = Stage.getStage(player.getWorld(), stageId);
        net.minecraft.server.world.ServerWorld stageLevel = player.getWorld().getServer().getWorld(RegistryKeys.toWorldKey(RegistryKey.of(RegistryKeys.DIMENSION, stage.dimID)));
        player.sendMessage(InkDisruptorItem.clearInk(stageLevel, stage.getCornerA(), stage.getCornerB(), true).getOutput(), true);
    }
}