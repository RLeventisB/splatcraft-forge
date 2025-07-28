package net.splatcraft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class OverwriteJumpingStatePacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(OverwriteJumpingStatePacket.class);
	public static final Map<Player, Boolean> overwrittenGroundedStates = new HashMap<>();
	final boolean forcedOnGround;
	public OverwriteJumpingStatePacket(boolean forcedOnGround)
	{
		this.forcedOnGround = forcedOnGround;
	}
	public static OverwriteJumpingStatePacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new OverwriteJumpingStatePacket(buffer.readBoolean());
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeBoolean(forcedOnGround);
	}
	public static Optional<Boolean> popForcedGroundedState(Player player)
	{
		return Optional.ofNullable(overwrittenGroundedStates.remove(player));
	}
	@Override
	public void execute(ServerPlayer player)
	{
		overwrittenGroundedStates.put(player, forcedOnGround);
	}
}
