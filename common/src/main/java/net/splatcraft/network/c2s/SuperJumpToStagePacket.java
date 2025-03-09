package net.splatcraft.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.data.Stage;
import net.splatcraft.util.CommonUtils;

public class SuperJumpToStagePacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SuperJumpToStagePacket.class);
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
		Stage.getStage(stageId).superJumpToStage((ServerPlayer) player);
	}
}