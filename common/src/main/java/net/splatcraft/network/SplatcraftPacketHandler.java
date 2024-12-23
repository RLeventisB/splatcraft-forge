package net.splatcraft.network;

import com.google.common.collect.Maps;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.GameInstance;
import net.minecraft.entity.Entity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.splatcraft.network.c2s.*;
import net.splatcraft.network.s2c.*;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

@SuppressWarnings("removal")
public class SplatcraftPacketHandler
{
	// i am about to swear for the 10th time but i will look like vivziepop
	// basically networkmanager throws a cast exception or something and there is nothing i can do so i will use the deprecated method that will be soon deleted!! yipee!
	public static final SplatcraftChannel CHANNEL = new SplatcraftChannel();
	private static final Map<Class<?>, MessageInfo<?>> encoders = Maps.newHashMap();
	private static final int ID = 0;
	public static void registerMessages()
	{
		//INSTANCE.registerMessage(ID++, PlayerColorPacket.class, SplatcraftPacket::encode, PlayerColorPacket::decode, SplatcraftPacket::consume);
		registerMessage(UpdatePlayerInfoPacket.class, UpdatePlayerInfoPacket::decode);
		registerMessage(PlayerColorPacket.class, PlayerColorPacket::decode);
		registerMessage(PlayerSetSquidC2SPacket.class, PlayerSetSquidC2SPacket::decode);
		registerMessage(PlayerSetSquidS2CPacket.class, PlayerSetSquidS2CPacket::decode);
		registerMessage(UpdateBooleanGamerulesPacket.class, UpdateBooleanGamerulesPacket::decode);
		registerMessage(UpdateIntGamerulesPacket.class, UpdateIntGamerulesPacket::decode);
		registerMessage(RequestPlayerInfoPacket.class, RequestPlayerInfoPacket::decode);
		registerMessage(SendScanTurfResultsPacket.class, SendScanTurfResultsPacket::decode);
		registerMessage(UpdateColorScoresPacket.class, UpdateColorScoresPacket::decode);
		registerMessage(UpdateBlockColorPacket.class, UpdateBlockColorPacket::decode);
		registerMessage(DodgeRollPacket.class, DodgeRollPacket::decode);
		registerMessage(DodgeRollEndPacket.class, DodgeRollEndPacket::decode);
		registerMessage(WeaponUseEndPacket.class, WeaponUseEndPacket::decode);
		registerMessage(SquidInputPacket.class, SquidInputPacket::decode);
		registerMessage(CraftWeaponPacket.class, CraftWeaponPacket::decode);
		registerMessage(UpdateClientColorsPacket.class, UpdateClientColorsPacket::decode);
		registerMessage(UpdateInkOverlayPacket.class, UpdateInkOverlayPacket::decode);
		registerMessage(ReleaseChargePacket.class, ReleaseChargePacket::decode);
		registerMessage(UpdateChargeStatePacket.class, UpdateChargeStatePacket::decode);
		registerMessage(SwapSlotWithOffhandPacket.class, SwapSlotWithOffhandPacket::decode);
		registerMessage(UpdateStageListPacket.class, UpdateStageListPacket::decode);
		registerMessage(UpdateWeaponSettingsPacket.class, UpdateWeaponSettingsPacket::decode);
		registerMessage(SendPlayerOverlayPacket.class, SendPlayerOverlayPacket::decode);
		registerMessage(ReceivePlayerOverlayPacket.class, ReceivePlayerOverlayPacket::decode);
		registerMessage(UpdateInkPacket.class, UpdateInkPacket::decode);
		registerMessage(DeleteInkPacket.class, DeleteInkPacket::decode);
		registerMessage(WatchInkPacket.class, WatchInkPacket::decode);
		registerMessage(SendJumpLureDataPacket.class, SendJumpLureDataPacket::decode);
		registerMessage(UseJumpLurePacket.class, UseJumpLurePacket::decode);
		
		//Stage Pad packets
		registerMessage(SuperJumpToStagePacket.class, SuperJumpToStagePacket::decode);
		registerMessage(SendStageWarpDataToPadPacket.class, SendStageWarpDataToPadPacket::decode);
		registerMessage(RequestUpdateStageSpawnPadsPacket.class, RequestUpdateStageSpawnPadsPacket::decode);
		registerMessage(RequestWarpDataPacket.class, RequestWarpDataPacket::decode);
		registerMessage(CreateOrEditStagePacket.class, CreateOrEditStagePacket::decode);
		registerMessage(NotifyStageCreatePacket.class, NotifyStageCreatePacket::decode);
		registerMessage(RequestTurfScanPacket.class, RequestTurfScanPacket::decode);
		registerMessage(RequestClearInkPacket.class, RequestClearInkPacket::decode);
		registerMessage(RequestSetStageRulePacket.class, RequestSetStageRulePacket::decode);
	}
	private static <MSG extends SplatcraftPacket> void registerMessage(Class<MSG> messageType, Function<RegistryByteBuf, MSG> decoder)
	{
		registerMessage(messageType, SplatcraftPacket::encode, decoder, SplatcraftPacket::consume);
	}
	private static <MSG extends SplatcraftPacket> void registerMessage(Class<MSG> messageType, BiConsumer<MSG, RegistryByteBuf> encoder, Function<RegistryByteBuf, MSG> decoder, BiConsumer<MSG, NetworkManager.PacketContext> messageConsumer)
	{
		CHANNEL.register(
			messageType,
			encoder,
			decoder,
			messageConsumer,
			PlayC2SPacket.class.isAssignableFrom(messageType) ? NetworkManager.Side.C2S : NetworkManager.Side.S2C
		);

		/*CustomPayload.Id<MSG> id;
		try
		{
			id = (CustomPayload.Id<MSG>) messageType.getField("ID").get(null);
		}
		catch (IllegalAccessException | NoSuchFieldException e)
		{
			throw new RuntimeException(e);
		}
		NetworkManager.Side side = PlayC2SPacket.class.isAssignableFrom(messageType) ? NetworkManager.Side.C2S : NetworkManager.Side.S2C;
		if (side == NetworkManager.Side.C2S)
		{
			//noinspection removal
			NetworkManager.registerReceiver(
				side,
				id.id(),
//			packetCodec,
//			Collections.singletonList(new SplitPacketTransformer()),
				(buf, context) ->
				{
					messageConsumer.accept(decoder.apply(buf), context);
				}
			);
		}
		else if (Platform.getEnvironment() == Env.SERVER)
		{
			PacketCodec<RegistryByteBuf, MSG> packetCodec = PacketCodec.ofStatic(
				(t, u) -> encoder.accept(u, t),
				decoder::apply
			);
			
			NetworkManager.registerS2CPayloadType(id.id(), packetCodec, List.of(new SplitPacketTransformer()));
		}*/
	}
	public static <MSG extends PlayS2CPacket> void sendToPlayer(MSG message, ServerPlayerEntity player)
	{
		CHANNEL.sendToPlayer(player, message);
//        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
	}
	public static <MSG extends SplatcraftPacket> void sendToPlayers(MSG message, List<ServerPlayerEntity> players)
	{
		CHANNEL.sendToPlayers(players, message);
//        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
	}
	public static <MSG extends SplatcraftPacket> void sendToDim(MSG message, RegistryKey<World> world)
	{
		sendToPlayers(message, GameInstance.getServer().getPlayerManager().getPlayerList().stream().filter(v -> v.getWorld().getRegistryKey() == world).toList());

//        INSTANCE.send(PacketDistributor.DIMENSION.with(() -> world), message);
	}
	public static <MSG extends SplatcraftPacket> void sendToTrackers(MSG message, WorldChunk trackedChunk)
	{
		if (trackedChunk.getWorld().getChunkManager() instanceof ServerChunkManager serverChunkManager)
		{
			sendToPlayers(message, serverChunkManager.chunkLoadingManager.getPlayersWatchingChunk(trackedChunk.getPos()));
		}

//        INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> trackedChunk), message);
	}
	public static <MSG extends SplatcraftPacket> void sendToTrackers(MSG message, Entity trackedEntity)
	{
		if (trackedEntity.getWorld().getChunkManager() instanceof ServerChunkManager serverChunkManager)
		{
			serverChunkManager.sendToOtherNearbyPlayers(trackedEntity, CHANNEL.toPacket(NetworkManager.Side.S2C, message, trackedEntity.getRegistryManager()));
		}
//        INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> trackedEntity), message);
	}
	public static <MSG extends SplatcraftPacket> void sendToTrackersAndSelf(MSG message, Entity trackedEntity)
	{
		if (trackedEntity.getWorld().getChunkManager() instanceof ServerChunkManager serverChunkManager)
		{
			serverChunkManager.sendToNearbyPlayers(trackedEntity, CHANNEL.toPacket(NetworkManager.Side.S2C, message, trackedEntity.getRegistryManager()));
		}
	}
	public static <MSG extends SplatcraftPacket> void sendToAll(MSG message)
	{
		CHANNEL.sendToPlayers(GameInstance.getServer().getPlayerManager().getPlayerList(), message);
//		GameInstance.getServer().getPlayerManager().sendToAll(new CustomPayloadS2CPacket(message));
	}
	public static <MSG extends PlayC2SPacket> void sendToServer(MSG message)
	{
		CHANNEL.sendToServer(message);
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
