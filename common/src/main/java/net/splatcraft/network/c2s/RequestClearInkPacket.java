package net.splatcraft.network.c2s;

import dev.architectury.utils.GameInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.world.ServerWorld;
import net.splatcraft.data.Stage;
import net.splatcraft.items.remotes.InkDisruptorItem;
import net.splatcraft.util.CommonUtils;

public class RequestClearInkPacket extends PlayC2SPacket
{
	public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(RequestClearInkPacket.class);
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
		Stage stage = Stage.getStage(stageId);
		ServerWorld stageworld = stage.getStageWorld(GameInstance.getServer());
		player.sendMessage(InkDisruptorItem.clearInk(stageworld, stage.getCornerA(), stage.getCornerB(), true).getOutput(), true);
	}
}