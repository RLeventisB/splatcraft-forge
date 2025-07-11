package net.splatcraft.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.items.JumpLureItem;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class UseJumpLurePacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(UseJumpLurePacket.class);
	@Nullable
	final UUID targetUUID;
	final InkColor color;
	public UseJumpLurePacket(InkColor color, @Nullable UUID targetUUID)
	{
		this.targetUUID = targetUUID;
		this.color = color;
	}
	public static UseJumpLurePacket decode(FriendlyByteBuf buf)
	{
		return new UseJumpLurePacket(InkColor.PACKET_CODEC.decode(buf), buf.readBoolean() ? null : buf.readUUID());
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		InkColor.PACKET_CODEC.encode(buffer, color);
		buffer.writeBoolean(targetUUID == null);
		if (targetUUID != null)
			buffer.writeUUID(targetUUID);
	}
	@Override
	public void execute(Player player)
	{
		JumpLureItem.activate((ServerPlayer) player, targetUUID, color);
	}
}
