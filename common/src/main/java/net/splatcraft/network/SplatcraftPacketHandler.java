package net.splatcraft.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.GameInstance;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.splatcraft.network.c2s.*;
import net.splatcraft.network.s2c.*;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

@SuppressWarnings("removal")
public class SplatcraftPacketHandler
{
	// i am about to swear for the 10th time but i will look like vivziepop
	// basically networkmanager throws a cast exception or something and there is nothing i can do so i will use the deprecated method that will be soon deleted!! yipee!
//	public static final SplatcraftChannel CHANNEL = new SplatcraftChannel();
	public static void registerMessages()
	{
		//INSTANCE.registerMessage(ID++, PlayerColorPacket.class, SplatcraftPacket::encode, PlayerColorPacket::decode, SplatcraftPacket::consume);
		registerMessage(UpdateEntityInfoPacket.ID, UpdateEntityInfoPacket.class, UpdateEntityInfoPacket::decode);
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
		registerMessage(SquidInputPacket.ID, SquidInputPacket.class, SquidInputPacket::decode);
		registerMessage(CraftWeaponPacket.ID, CraftWeaponPacket.class, CraftWeaponPacket::decode);
		registerMessage(UpdateClientColorsPacket.ID, UpdateClientColorsPacket.class, UpdateClientColorsPacket::decode);
		registerMessage(SendColorRegistryPacket.ID, SendColorRegistryPacket.class, SendColorRegistryPacket::decode);
		registerMessage(UpdateInkOverlayPacket.ID, UpdateInkOverlayPacket.class, UpdateInkOverlayPacket::decode);
		registerMessage(ReleaseChargePacket.ID, ReleaseChargePacket.class, ReleaseChargePacket::decode);
		registerMessage(UpdateChargeStatePacket.ID, UpdateChargeStatePacket.class, UpdateChargeStatePacket::decode);
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
		registerMessage(SendSpecialUsageDataPacket.ID, SendSpecialUsageDataPacket.class, SendSpecialUsageDataPacket::decode);
		
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
		registerMessage(messageType, SplatcraftPacket::encode, decoder, SplatcraftPacket::consume);
	}
	private static <MSG extends SplatcraftPacket> void registerMessage(Class<MSG> messageType, BiConsumer<MSG, RegistryFriendlyByteBuf> encoder, Function<RegistryFriendlyByteBuf, MSG> decoder, BiConsumer<MSG, NetworkManager.PacketContext> messageConsumer)
	{
		CHANNEL.register(
			messageType,
			encoder,
			decoder,
			messageConsumer,
			PlayC2SPacket.class.isAssignableFrom(messageType) ? NetworkManager.Side.C2S : NetworkManager.Side.S2C
		);
	}
	public static <MSG extends PlayS2CPacket> void sendToPlayer(MSG message, ServerPlayer player)
	{
		CHANNEL.sendToPlayer(player, message);
	}
	public static <MSG extends SplatcraftPacket> void sendToPlayers(MSG message, List<ServerPlayer> players)
	{
		CHANNEL.sendToPlayers(players, message);
	}
	public static <MSG extends SplatcraftPacket> void sendToDim(MSG message, ResourceKey<Level> world)
	{
		sendToPlayers(message, GameInstance.getServer().getPlayerList().getPlayers().stream().filter(v -> v.level().dimension() == world).toList());
	}
	public static <MSG extends SplatcraftPacket> void sendToTrackers(MSG message, LevelChunk trackedChunk)
	{
		if (trackedChunk.getLevel().getChunkSource() instanceof ServerChunkCache serverChunkManager)
		{
			sendToPlayers(message, serverChunkManager.chunkMap.getPlayersCloseForSpawning(trackedChunk.getPos()));
		}
	}
	public static <MSG extends SplatcraftPacket> void sendToTrackers(MSG message, Entity trackedEntity)
	{
		if (trackedEntity.level().getChunkSource() instanceof ServerChunkCache serverChunkManager)
		{
			serverChunkManager.broadcast(trackedEntity, CHANNEL.toPacket(NetworkManager.Side.S2C, message, trackedEntity.registryAccess()));
		}
	}
	public static <MSG extends SplatcraftPacket> void sendToTrackersAndSelf(MSG message, Entity trackedEntity)
	{
		if (trackedEntity.level().getChunkSource() instanceof ServerChunkCache serverChunkManager)
		{
			serverChunkManager.broadcastAndSend(trackedEntity, CHANNEL.toPacket(NetworkManager.Side.S2C, message, trackedEntity.registryAccess()));
		}
	}
	public static <MSG extends SplatcraftPacket> void sendToAll(MSG message)
	{
		CHANNEL.sendToPlayers(GameInstance.getServer().getPlayerList().getPlayers(), message);
	}
	public static <MSG extends PlayC2SPacket> void sendToServer(MSG message)
	{
		CHANNEL.sendToServer(message);
	}
}
