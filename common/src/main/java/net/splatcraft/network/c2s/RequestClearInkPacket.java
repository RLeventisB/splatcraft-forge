package net.splatcraft.network.c2s;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerWorld;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.data.Stage;
import net.splatcraft.items.remotes.InkDisruptorItem;

public class RequestClearInkPacket extends PlayC2SPacket
{
    final String stageId;

    public RequestClearInkPacket(String stageId)
    {
        this.stageId = stageId;
    }

    public static RequestClearInkPacket decode(FriendlyByteBuf buffer)
    {
        return new RequestClearInkPacket(buffer.readUtf());
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeUtf(stageId);
    }

    @Override
    public void execute(Player player)
    {
        Stage stage = Stage.getStage(player.getWorld(), stageId);
        ServerWorld stageLevel = player.getWorld().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, stage.dimID));
        player.displayClientMessage(InkDisruptorItem.clearInk(stageLevel, stage.getCornerA(), stage.getCornerB(), true).getOutput(), true);
    }
}