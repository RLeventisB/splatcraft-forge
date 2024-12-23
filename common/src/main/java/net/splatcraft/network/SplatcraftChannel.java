package net.splatcraft.network;

import com.google.common.collect.Maps;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.splatcraft.Splatcraft;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

@SuppressWarnings("removal")
public class SplatcraftChannel
{
	private static int ID = 0;
	private final Map<Class<?>, MessageInfo<?>> encoders = Maps.newHashMap();
	public static long hashCodeString(String str)
	{
		long h = 0;
		var length = str.length();
		for (var i = 0; i < length; i++)
		{
			h = 31 * h + str.charAt(i);
		}
		return h;
	}
	public <T> void register(Class<T> type, BiConsumer<T, RegistryByteBuf> encoder, Function<RegistryByteBuf, T> decoder, BiConsumer<T, NetworkManager.PacketContext> messageConsumer, NetworkManager.Side side)
	{
		var info = new MessageInfo<T>(Splatcraft.identifierOf("packet_" + ID++), encoder, decoder, messageConsumer);
		encoders.put(type, info);
		NetworkManager.NetworkReceiver<RegistryByteBuf> receiver = (buf, context) ->
			info.messageConsumer.accept(info.decoder.apply(buf), context);
		
		if (side == NetworkManager.Side.C2S)
		{
			NetworkManager.registerReceiver(side, info.packetId, receiver);
		}
		else
		{
			if (Platform.getEnvironment() == Env.CLIENT)
			{
				NetworkManager.registerReceiver(NetworkManager.s2c(), info.packetId, receiver);
			}
			else
			{
				NetworkManager.registerS2CPayloadType(info.packetId);
			}
		}
	}
	public <T> Packet<?> toPacket(NetworkManager.Side side, T message, DynamicRegistryManager access)
	{
		var messageInfo = (MessageInfo<T>) encoders.get(message.getClass());
		var buf = new RegistryByteBuf(Unpooled.buffer(), access);
		messageInfo.encoder.accept(message, buf);
		return NetworkManager.toPacket(side, messageInfo.packetId, buf);
	}
	public <T> void sendToPlayer(ServerPlayerEntity player, T message)
	{
		player.networkHandler.send(toPacket(NetworkManager.s2c(), message, player.getRegistryManager()), null);
	}
	public <T> void sendToPlayers(List<ServerPlayerEntity> players, T message)
	{
		if (players.isEmpty())
			return;
		var packet = toPacket(NetworkManager.s2c(), message, players.getFirst().getRegistryManager());
		for (var player : players)
		{
			player.networkHandler.send(packet, null);
		}
	}
	@Environment(EnvType.CLIENT)
	public <T> void sendToServer(T message)
	{
		ClientPlayNetworkHandler connection = MinecraftClient.getInstance().getNetworkHandler();
		if (connection != null)
		{
			// yes there is a connection.sendPacket but forge renames this to send and that makes javac angry so :(
			connection.getConnection().send(toPacket(NetworkManager.c2s(), message, connection.getRegistryManager()));
		}
		else
		{
			throw new IllegalStateException("Unable to send packet to the server while not in game!");
		}
	}
	private record MessageInfo<T>(
		Identifier packetId,
		BiConsumer<T, RegistryByteBuf> encoder,
		Function<RegistryByteBuf, T> decoder,
		BiConsumer<T, NetworkManager.PacketContext> messageConsumer
	)
	{
	}
}
