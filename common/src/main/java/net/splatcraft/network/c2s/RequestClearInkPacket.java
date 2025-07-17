package net.splatcraft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.data.Stage;
import net.splatcraft.items.remotes.InkDisruptorItem;
import net.splatcraft.platform.Services;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

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
	public @NotNull Type<? extends CustomPacketPayload> type()
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
		ServerLevel stageworld = stage.getStageWorld(Services.PLATFORM.getServerInstance());
		player.displayClientMessage(InkDisruptorItem.clearInk(stageworld, stage.getMinCorner(), stage.getMaxCorner(), true).getOutput(), true);
	}
}