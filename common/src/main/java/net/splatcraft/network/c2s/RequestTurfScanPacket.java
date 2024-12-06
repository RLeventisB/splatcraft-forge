package net.splatcraft.network.c2s;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerWorld;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.data.Stage;
import net.splatcraft.items.remotes.TurfScannerItem;

import java.util.ArrayList;

public class RequestTurfScanPacket extends PlayC2SPacket
{
    final String stageId;
    final boolean isTopDown;

    public RequestTurfScanPacket(String stageId, boolean isTopDown)
    {
        this.stageId = stageId;
        this.isTopDown = isTopDown;
    }

    public static RequestTurfScanPacket decode(FriendlyByteBuf buffer)
    {
        return new RequestTurfScanPacket(buffer.readUtf(), buffer.readBoolean());
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeUtf(stageId);
        buffer.writeBoolean(isTopDown);
    }

    @Override
    public void execute(Player player)
    {
        Stage stage = Stage.getStage(player.getWorld(), stageId);
        ServerPlayer serverPlayer = (ServerPlayer) player;

        ServerWorld stageLevel = player.getWorld().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, stage.dimID));
        ArrayList<ServerPlayer> playerList = new ArrayList<>(stageLevel.getEntitiesOfClass(ServerPlayer.class, stage.getBounds()));
        if (!playerList.contains(serverPlayer))
            playerList.add(0, serverPlayer);
        player.displayClientMessage(TurfScannerItem.scanTurf(stageLevel, stageLevel, stage.cornerA, stage.cornerB, isTopDown ? 0 : 1, playerList).getOutput(), true);
    }
}