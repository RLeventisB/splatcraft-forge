package net.splatcraft.network.c2s;

import dev.architectury.utils.GameInstance;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.data.Stage;
import net.splatcraft.items.remotes.TurfScannerItem;
import net.splatcraft.util.CommonUtils;

import java.util.ArrayList;

public class RequestTurfScanPacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(RequestTurfScanPacket.class);
	final String stageId;
	final boolean isTopDown;
	public RequestTurfScanPacket(String stageId, boolean isTopDown)
	{
		this.stageId = stageId;
		this.isTopDown = isTopDown;
	}
	public static RequestTurfScanPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new RequestTurfScanPacket(buffer.readUtf(), buffer.readBoolean());
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
		buffer.writeBoolean(isTopDown);
	}
	@Override
	public void execute(Player player)
	{
		Stage stage = Stage.getStage(stageId);
		ServerPlayer serverPlayer = (ServerPlayer) player;
		
		ServerLevel stageworld = stage.getStageWorld(GameInstance.getServer());
		ArrayList<ServerPlayer> playerList = new ArrayList<>(stageworld.getEntitiesOfClass(ServerPlayer.class, stage.getBounds(), EntitySelector.NO_SPECTATORS));
		if (!playerList.contains(serverPlayer))
			playerList.addFirst(serverPlayer);
		player.displayClientMessage(TurfScannerItem.scanTurf(stageworld, stageworld, stage.cornerA, stage.cornerB, isTopDown ? 0 : 1, playerList).getOutput(), true);
	}
}