package net.splatcraft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.data.Stage;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.SendStageWarpDataToPadPacket;
import net.splatcraft.util.CommonUtils;

public class RequestUpdateStageSpawnPadsPacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(RequestUpdateStageSpawnPadsPacket.class);
	final String stageId;
	public RequestUpdateStageSpawnPadsPacket(String stageId)
	{
		this.stageId = stageId;
	}
	public RequestUpdateStageSpawnPadsPacket(Stage stage)
	{
		this(stage.id);
	}
	public static RequestUpdateStageSpawnPadsPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new RequestUpdateStageSpawnPadsPacket(buffer.readUtf());
	}
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeUtf(stageId);
	}
	@Override
	public void execute(Player player)
	{
		Stage.getStage(stageId).updateSpawnPads(player.level());
		SplatcraftPacketHandler.sendToPlayer(SendStageWarpDataToPadPacket.compile(player), (ServerPlayer) player);
	}
}