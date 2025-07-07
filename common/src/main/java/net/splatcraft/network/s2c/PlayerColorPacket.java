package net.splatcraft.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.Splatcraft;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerColorPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = new Type<>(Splatcraft.identifierOf("player_color_packet"));
	private final InkColor color;
	UUID target;
	String playerName;
	public PlayerColorPacket(UUID player, String name, InkColor color)
	{
		this.color = color;
		target = player;
		playerName = name;
	}
	public PlayerColorPacket(Player player, InkColor color)
	{
		this(player.getUUID(), player.getDisplayName().getString(), color);
	}
	public static PlayerColorPacket decode(RegistryFriendlyByteBuf buffer)
	{
		int color = buffer.readInt();
		String name = buffer.readUtf();
		UUID player = buffer.readUUID();
		return new PlayerColorPacket(player, name, InkColor.constructOrReuse(color));
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeInt(color.getColor());
		buffer.writeUtf(playerName);
		buffer.writeUUID(target);
	}
	@Override
	public void execute()
	{
		Player player = Minecraft.getInstance().level.getPlayerByUUID(target);
		if (player != null)
			ColorUtils.setPlayerColor(player, color, false);
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
