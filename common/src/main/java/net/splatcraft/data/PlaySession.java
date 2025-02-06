package net.splatcraft.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Uuids;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.World;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.SendPlaySessionEndPacket;
import net.splatcraft.util.CodecUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class PlaySession
{
	public static final Codec<PlaySession> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		Uuids.CODEC.listOf().fieldOf("players").forGetter(v -> v.playerUuids),
		StageGameMode.CODEC.fieldOf("game_mode").forGetter(v -> v.gameMode),
		Codec.STRING.fieldOf("stage_id").forGetter(v -> v.stageId),
		Codecs.INSTANT.fieldOf("session_end_instant").forGetter(v -> v.sessionEndInstant),
		RegistryKey.createCodec(RegistryKeys.WORLD).fieldOf("world").forGetter(v -> v.worldKey)
	).apply(inst, PlaySession::new));
	public static final PacketCodec<ByteBuf, PlaySession> PACKET_CODEC = PacketCodec.tuple(
		Uuids.PACKET_CODEC.collect(PacketCodecs.toList()), v -> v.playerUuids,
		CodecUtils.createEnumPacketCodec(StageGameMode::values), v -> v.gameMode,
		PacketCodecs.STRING, v -> v.stageId,
		CodecUtils.Codecs.INSTANT_PACKET_CODEC, v -> v.sessionEndInstant,
		RegistryKey.createPacketCodec(RegistryKeys.WORLD), v -> v.worldKey,
		PlaySession::new);
	public final List<UUID> playerUuids;
	public final StageGameMode gameMode;
	public final String stageId;
	public Instant sessionEndInstant;
	public RegistryKey<World> worldKey;
	public PlaySession(World world, Collection<ServerPlayerEntity> players, Stage stage, StageGameMode gameMode)
	{
		playerUuids = players.stream().map(PlayerEntity::getUuid).toList();
		players.forEach(player ->
		{
			EntityInfoCapability.getOptional(player).ifPresent(info -> info.setPlayingStageId(stage.id));
		});
		
		this.gameMode = gameMode;
		stageId = stage.id;
		worldKey = world.getRegistryKey();
		sessionEndInstant = Instant.now().plus(gameMode.DEFAULT_TIME_SECONDS, ChronoUnit.SECONDS);
	}
	public PlaySession(List<UUID> playerUuids, StageGameMode gameMode, String stageId, Instant sessionEndInstant, RegistryKey<World> worldKey)
	{
		this.playerUuids = playerUuids;
		this.gameMode = gameMode;
		this.stageId = stageId;
		this.sessionEndInstant = sessionEndInstant;
		this.worldKey = worldKey;
	}
	/**
	 * Ticks all the play session related actions.
	 *
	 * @return false if the play session has ended, otherwise, true
	 */
	public boolean tick(MinecraftServer server)
	{
		if (server != null)
		{
			if (playerUuids.isEmpty() || playerUuids.stream().allMatch(v -> server.getWorld(worldKey).getPlayerByUuid(v) == null))
			{
				end(server, EndReason.NO_PLAYERS);
				return false;
			}
		}
		if (Instant.now().isAfter(sessionEndInstant) && !gameMode.overtimeChecker.apply(this))
		{
			end(server, EndReason.NORMAL);
			return false;
		}
		gameMode.tick.accept(this);
		return true;
	}
	public void end(MinecraftServer server, EndReason endReason)
	{
		gameMode.onEnd.accept(this);
		if (server != null)
		{
			playerUuids.forEach(uuid ->
			{
				ServerWorld world = server.getWorld(worldKey);
				if (world == null)
					return;
				
				PlayerEntity plr = world.getPlayerByUuid(uuid);
				if (plr == null)
					return;
				
				EntityInfoCapability.getOptional(plr).ifPresent(info -> info.setPlayingStageId(null));
			});
			SaveInfoCapability.get().playSessions().remove(stageId);
			SplatcraftPacketHandler.sendToAll(new SendPlaySessionEndPacket(stageId, playerUuids));
		}
	}
	@Override
	public boolean equals(Object obj)
	{
		if (obj == this) return true;
		return obj instanceof PlaySession that && Objects.equals(playerUuids, that.playerUuids) &&
			Objects.equals(stageId, that.stageId) &&
			Objects.equals(gameMode, that.gameMode);
	}
	@Override
	public int hashCode()
	{
		return Objects.hash(playerUuids, stageId, gameMode);
	}
	@Override
	public String toString()
	{
		return "PlaySession[" +
			"players=" + playerUuids + ", " +
			"stageId=" + stageId + ", " +
			"gameMode=" + gameMode + ']';
	}
	public enum EndReason
	{
		NO_PLAYERS,
		NORMAL,
		FORCED
	}
}
