package net.splatcraft.network;

import commonnetwork.api.Network;
import commonnetwork.networking.data.PacketContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.splatcraft.network.c2s.*;
import net.splatcraft.network.s2c.*;
import net.splatcraft.platform.Services;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SplatcraftPacketHandler
{
	public static void registerMessages()
	{
		registerMessage(UpdateEntityInfoPacket.ID, UpdateEntityInfoPacket.class, UpdateEntityInfoPacket::decode);
		registerMessage(VoidedChargePacket.ID, VoidedChargePacket.class, VoidedChargePacket::decode);
		registerMessage(UpdateEntityActionOnlyPacket.ID, UpdateEntityActionOnlyPacket.class, UpdateEntityActionOnlyPacket::decode);
		registerMessage(PlayerColorPacket.ID, PlayerColorPacket.class, PlayerColorPacket::decode);
		registerMessage(PlayerSetSquidC2SPacket.ID, PlayerSetSquidC2SPacket.class, PlayerSetSquidC2SPacket::decode);
		registerMessage(PlayerSetSquidS2CPacket.ID, PlayerSetSquidS2CPacket.class, PlayerSetSquidS2CPacket::decode);
		registerMessage(UpdateBooleanGamerulesPacket.ID, UpdateBooleanGamerulesPacket.class, UpdateBooleanGamerulesPacket::decode);
		registerMessage(UpdateIntGamerulesPacket.ID, UpdateIntGamerulesPacket.class, UpdateIntGamerulesPacket::decode);
		registerMessage(RequestEntityInfoPacket.ID, RequestEntityInfoPacket.class, RequestEntityInfoPacket::decode);
		registerMessage(SendScanTurfResultsPacket.ID, SendScanTurfResultsPacket.class, SendScanTurfResultsPacket::decode);
		registerMessage(UpdateColorScoresPacket.ID, UpdateColorScoresPacket.class, UpdateColorScoresPacket::decode);
		registerMessage(UpdateBlockColorPacket.ID, UpdateBlockColorPacket.class, UpdateBlockColorPacket::decode);
		registerMessage(DodgeRollPacket.ID, DodgeRollPacket.class, DodgeRollPacket::decode);
		
		registerMessage(SendSquidSurgePacket.ID, SendSquidSurgePacket.class, SendSquidSurgePacket::decode);
		registerMessage(UpdateSquidSurgePacket.ID, UpdateSquidSurgePacket.class, UpdateSquidSurgePacket::decode);
		
		registerMessage(CraftWeaponPacket.ID, CraftWeaponPacket.class, CraftWeaponPacket::decode);
		registerMessage(SendColorRegistryPacket.ID, SendColorRegistryPacket.class, SendColorRegistryPacket::decode);
		registerMessage(UpdateInkOverlayPacket.ID, UpdateInkOverlayPacket.class, UpdateInkOverlayPacket::decode);
		registerMessage(SwapSlotWithOffhandPacket.ID, SwapSlotWithOffhandPacket.class, SwapSlotWithOffhandPacket::decode);
		registerMessage(UpdateStageListPacket.ID, UpdateStageListPacket.class, UpdateStageListPacket::decode);
		registerMessage(UpdateWeaponSettingsPacket.ID, UpdateWeaponSettingsPacket.class, UpdateWeaponSettingsPacket::decode);
		registerMessage(UpdateInkPacket.ID, UpdateInkPacket.class, UpdateInkPacket::decode);
		registerMessage(DeleteInkPacket.ID, DeleteInkPacket.class, DeleteInkPacket::decode);
		registerMessage(WatchInkPacket.ID, WatchInkPacket.class, WatchInkPacket::decode);
		registerMessage(SendJumpLureDataPacket.ID, SendJumpLureDataPacket.class, SendJumpLureDataPacket::decode);
		registerMessage(SendPlayerDeathMatchPacket.ID, SendPlayerDeathMatchPacket.class, SendPlayerDeathMatchPacket::decode);
		registerMessage(SendPlayerRespawnMatchPacket.ID, SendPlayerRespawnMatchPacket.class, SendPlayerRespawnMatchPacket::decode);
		registerMessage(SendPlaySessionCreationPacket.ID, SendPlaySessionCreationPacket.class, SendPlaySessionCreationPacket::decode);
		registerMessage(SendPlaySessionEndPacket.ID, SendPlaySessionEndPacket.class, SendPlaySessionEndPacket::decode);
		registerMessage(UseJumpLurePacket.ID, UseJumpLurePacket.class, UseJumpLurePacket::decode);
		registerMessage(RequestSpecialUsageDataPacket.ID, RequestSpecialUsageDataPacket.class, RequestSpecialUsageDataPacket::decode);
		registerMessage(UpdateInputPacket.ID, UpdateInputPacket.class, UpdateInputPacket::decode);
		registerMessage(SendSpecialUsageDataPacket.ID, SendSpecialUsageDataPacket.class, SendSpecialUsageDataPacket::decode);
		registerMessage(SendSquidDisablePacket.ID, SendSquidDisablePacket.class, SendSquidDisablePacket::decode);
		registerMessage(SendEnemyInkDamagePacket.ID, SendEnemyInkDamagePacket.class, SendEnemyInkDamagePacket::decode);
		
		//Stage Pad packets
		registerMessage(SuperJumpToStagePacket.ID, SuperJumpToStagePacket.class, SuperJumpToStagePacket::decode);
		registerMessage(SendStageWarpDataToPadPacket.ID, SendStageWarpDataToPadPacket.class, SendStageWarpDataToPadPacket::decode);
		registerMessage(RequestUpdateStageSpawnPadsPacket.ID, RequestUpdateStageSpawnPadsPacket.class, RequestUpdateStageSpawnPadsPacket::decode);
		registerMessage(RequestWarpDataPacket.ID, RequestWarpDataPacket.class, RequestWarpDataPacket::decode);
		registerMessage(CreateOrEditStagePacket.ID, CreateOrEditStagePacket.class, CreateOrEditStagePacket::decode);
		registerMessage(NotifyStageCreatePacket.ID, NotifyStageCreatePacket.class, NotifyStageCreatePacket::decode);
		registerMessage(RequestTurfScanPacket.ID, RequestTurfScanPacket.class, RequestTurfScanPacket::decode);
		registerMessage(RequestClearInkPacket.ID, RequestClearInkPacket.class, RequestClearInkPacket::decode);
		registerMessage(RequestSetStageRulePacket.ID, RequestSetStageRulePacket.class, RequestSetStageRulePacket::decode);
	}
	private static <MSG extends SplatcraftPacket> void registerMessage(CustomPacketPayload.Type<? extends CustomPacketPayload> id, Class<MSG> messageType, Function<RegistryFriendlyByteBuf, MSG> decoder)
	{
		registerMessage(id, messageType, SplatcraftPacket::encode, decoder, SplatcraftPacket::consume);
	}
	private static <MSG extends SplatcraftPacket> void registerMessage(CustomPacketPayload.Type<? extends CustomPacketPayload> id, Class<MSG> messageType, BiConsumer<MSG, RegistryFriendlyByteBuf> encoder, Function<RegistryFriendlyByteBuf, MSG> decoder, BiConsumer<MSG, PacketContext<MSG>> messageConsumer)
	{
		StreamCodec<RegistryFriendlyByteBuf, MSG> codec = StreamCodec.of((a, b) -> encoder.accept(b, a), decoder::apply);
		Network.registerPacket(id, messageType, codec, (v) -> messageConsumer.accept(v.message(), v));
	}
	public static <MSG extends PlayS2CPacket> void sendToPlayer(MSG message, ServerPlayer player)
	{
		Network.getNetworkHandler().sendToClient(message, player);
	}
	public static <MSG extends PlayS2CPacket> void sendToPlayers(MSG message, List<ServerPlayer> players)
	{
		Network.getNetworkHandler().sendToClients(message, players);
	}
	public static <MSG extends PlayS2CPacket> void sendToDim(MSG message, ResourceKey<Level> world)
	{
		sendToPlayers(message, Services.PLATFORM.getServerInstance().getPlayerList().getPlayers().stream().filter(v -> v.level().dimension() == world).toList());
	}
	public static <MSG extends PlayS2CPacket> void sendToTrackers(MSG message, LevelChunk trackedChunk)
	{
		sendToTrackers(message, trackedChunk.getLevel(), trackedChunk.getPos());
	}
	public static <MSG extends PlayS2CPacket> void sendToTrackers(MSG message, Level level, ChunkPos trackedChunkPos)
	{
		if (level.getChunkSource() instanceof ServerChunkCache serverChunkManager)
		{
			sendToPlayers(message, serverChunkManager.chunkMap.getPlayersCloseForSpawning(trackedChunkPos));
		}
	}
	public static <MSG extends PlayS2CPacket> void sendToTrackers(MSG message, Entity trackedEntity)
	{
		if (trackedEntity.level().getChunkSource() instanceof ServerChunkCache serverChunkManager)
		{
			ChunkMap.TrackedEntity what = serverChunkManager.chunkMap.entityMap.get(trackedEntity.getId());
			if (what != null)
				sendToPlayers(message, what.seenBy.stream().map(ServerPlayerConnection::getPlayer).toList());
		}
	}
	public static <MSG extends PlayS2CPacket> void sendToTrackersAndSelf(MSG message, Entity trackedEntity)
	{
		sendToTrackers(message, trackedEntity);
		if (trackedEntity instanceof ServerPlayer serverPlayer)
		{
			sendToPlayer(message, serverPlayer);
		}
	}
	public static <MSG extends PlayS2CPacket> void sendToAll(MSG message)
	{
		Network.getNetworkHandler().sendToAllClients(message, Services.PLATFORM.getServerInstance());
	}
	public static <MSG extends PlayC2SPacket> void sendToServer(MSG message)
	{
		Network.getNetworkHandler().sendToServer(message);
	}
}
