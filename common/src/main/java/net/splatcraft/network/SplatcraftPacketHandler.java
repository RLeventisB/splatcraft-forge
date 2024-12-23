package net.splatcraft.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.GameInstance;
import net.minecraft.entity.Entity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.splatcraft.network.c2s.*;
import net.splatcraft.network.s2c.*;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SplatcraftPacketHandler
{
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
		CustomPayload.Id<MSG> id;
		try
		{
			id = (CustomPayload.Id<MSG>) messageType.getField("ID").get(null);
		}
		catch (IllegalAccessException | NoSuchFieldException e)
		{
			throw new RuntimeException(e);
		}
		if (PlayC2SPacket.class.isAssignableFrom(messageType))
		{
			NetworkManager.registerReceiver(
				NetworkManager.Side.C2S,
				id,
				PacketCodec.ofStatic(
					(buf, value) -> encoder.accept(value, buf),
					decoder::apply
				), messageConsumer::accept);
		}
		else if (PlayS2CPacket.class.isAssignableFrom(messageType))
		{
			NetworkManager.registerReceiver(
				NetworkManager.Side.S2C,
				id,
				PacketCodec.ofStatic(
					(buf, value) -> encoder.accept(value, buf),
					decoder::apply
				), messageConsumer::accept);
		}
		else
		{
			throw new IllegalArgumentException("messageType doesnt implement either PlayC2SPacket or PlayS2CPacket");
		}
//        INSTANCE.registerMessage(ID++, messageType, encoder, decoder, messageConsumer);
	}
	public static <MSG extends PlayS2CPacket> void sendToPlayer(MSG message, ServerPlayerEntity player)
	{
		NetworkManager.sendToPlayer(player, message);
//        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
	}
	public static <MSG extends SplatcraftPacket> void sendToPlayers(MSG message, Iterable<ServerPlayerEntity> players)
	{
		NetworkManager.sendToPlayers(players, message);
//        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
	}
	public static <MSG extends SplatcraftPacket> void sendToDim(MSG message, RegistryKey<World> world)
	{
		GameInstance.getServer().getPlayerManager().sendToDimension(new CustomPayloadS2CPacket(message), world);

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
			serverChunkManager.sendToOtherNearbyPlayers(trackedEntity, new CustomPayloadS2CPacket(message));
		}

//        INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> trackedEntity), message);
	}
	public static <MSG extends SplatcraftPacket> void sendToTrackersAndSelf(MSG message, Entity trackedEntity)
	{
		if (trackedEntity.getWorld().getChunkManager() instanceof ServerChunkManager serverChunkManager)
		{
			serverChunkManager.sendToNearbyPlayers(trackedEntity, NetworkManager.toPacket(NetworkManager.Side.S2C, message, DynamicRegistryManager.EMPTY));
		}
	}
	public static <MSG extends SplatcraftPacket> void sendToAll(MSG message)
	{
		GameInstance.getServer().getPlayerManager().sendToAll(new CustomPayloadS2CPacket(message));
	}
	public static <MSG extends PlayC2SPacket> void sendToServer(MSG message)
	{
		NetworkManager.sendToServer(message);
	}
}
