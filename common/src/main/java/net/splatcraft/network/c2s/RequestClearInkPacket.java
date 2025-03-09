package net.splatcraft.network.c2s;

import dev.architectury.utils.GameInstance;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.data.Stage;
import net.splatcraft.items.remotes.InkDisruptorItem;
import net.splatcraft.util.CommonUtils;

public class RequestClearInkPacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(RequestClearInkPacket.class);
	final String stageId;
	public RequestClearInkPacket(String stageId)
	{
		this.stageId = stageId;
	}
	public static RequestClearInkPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new RequestClearInkPacket(buffer.readUtf());
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
		Stage stage = Stage.getStage(stageId);
		ServerLevel stageworld = stage.getStageWorld(GameInstance.getServer());
		player.displayClientMessage(InkDisruptorItem.clearInk(stageworld, stage.getCornerA(), stage.getCornerB(), true).getOutput(), true);
	}
}