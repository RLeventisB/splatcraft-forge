package net.splatcraft.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.data.Stage;

public class SuperJumpToStagePacket extends PlayC2SPacket
{
    final String stageId;

    public SuperJumpToStagePacket(String stageId)
    {
        this.stageId = stageId;
    }

    public static SuperJumpToStagePacket decode(FriendlyByteBuf buf)
    {
        return new SuperJumpToStagePacket(buf.readUtf());
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeUtf(stageId);
    }

    @Override
    public void execute(Player player)
    {
        Stage.getStage(player.getWorld(), stageId).superJumpToStage((ServerPlayer) player);
    }
}