package net.splatcraft.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.splatcraft.data.Stage;
import net.splatcraft.items.remotes.TurfScannerItem;
import net.splatcraft.util.CommonUtils;

import java.util.ArrayList;

public class RequestTurfScanPacket extends PlayC2SPacket
{
    public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(RequestTurfScanPacket.class);
    final String stageId;
    final boolean isTopDown;

    public RequestTurfScanPacket(String stageId, boolean isTopDown)
    {
        this.stageId = stageId;
        this.isTopDown = isTopDown;
    }

    public static RequestTurfScanPacket decode(RegistryByteBuf buffer)
    {
        return new RequestTurfScanPacket(buffer.readString(), buffer.readBoolean());
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
        buffer.writeBoolean(isTopDown);
    }

    @Override
    public void execute(PlayerEntity player)
    {
        Stage stage = Stage.getStage(player.getWorld(), stageId);
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

        ServerWorld stageLevel = player.getWorld().getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, stage.dimID));
        ArrayList<ServerPlayerEntity> playerList = new ArrayList<>(stageLevel.getEntitiesByClass(ServerPlayerEntity.class, stage.getBounds(), EntityPredicates.EXCEPT_SPECTATOR));
        if (!playerList.contains(serverPlayer))
            playerList.add(0, serverPlayer);
        player.sendMessage(TurfScannerItem.scanTurf(stageLevel, stageLevel, stage.cornerA, stage.cornerB, isTopDown ? 0 : 1, playerList).getOutput(), true);
    }
}