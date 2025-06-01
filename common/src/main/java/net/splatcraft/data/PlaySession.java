package net.splatcraft.data;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.SendPlaySessionEndPacket;
import net.splatcraft.util.CodecUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;

public final class PlaySession
{
	public static final Duration INTRO_DURATION = Duration.of(15, ChronoUnit.SECONDS);
	public static final Duration END_DURATION = Duration.of(3, ChronoUnit.SECONDS);
	public static final Codec<PlaySession> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		UUIDUtil.AUTHLIB_CODEC.listOf().fieldOf("players").forGetter(v -> v.playerUuids),
		StageGameMode.CODEC.fieldOf("game_mode").forGetter(v -> v.gameMode),
		Codec.STRING.fieldOf("stage_id").forGetter(v -> v.stageId),
		ExtraCodecs.INSTANT_ISO8601.fieldOf("session_end_instant").forGetter(v -> v.sessionEndInstant)
	).apply(inst, PlaySession::new));
	public static final StreamCodec<ByteBuf, PlaySession> PACKET_CODEC = StreamCodec.composite(
		UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), v -> v.playerUuids,
		CodecUtils.createEnumPacketCodec(StageGameMode::values), v -> v.gameMode,
		ByteBufCodecs.STRING_UTF8, v -> v.stageId,
		CodecUtils.Codecs.INSTANT_PACKET_CODEC, v -> v.sessionEndInstant,
		PlaySession::new);
	public final List<UUID> playerUuids;
	public final StageGameMode gameMode;
	public final String stageId;
	// todo: implement pausing for singleplayer, but at the same time who will pause a match on singleplayer????, or have one ig
	// i put instants because they are pretty much epoch seconds and so if someone receives the
	// packet for when the server starts a play session but the receiver get a lag spike, they wont be delayed
	// and the match timer wont desync, HOWEVER since they are received from the system clock ig they are different by region??
	public final Instant sessionEndInstant;
	private final Supplier<Instant> matchStartInstantSupplier, matchEndInstantSupplier;
	public PlaySession(Collection<ServerPlayer> players, Stage stage, StageGameMode gameMode)
	{
		playerUuids = players.stream().map(Player::getUUID).toList();
		players.forEach(player ->
		{
			player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 1, false, false));
			EntityInfoCapability.getOptional(player).ifPresent(info ->
			{
				info.setIsSquid(true);
				info.setPlayingStageId(stage.id);
			});
		});

		this.gameMode = gameMode;
		stageId = stage.id;
		sessionEndInstant = Instant.now().plus(INTRO_DURATION).plusSeconds(gameMode.DEFAULT_TIME_SECONDS).plus(END_DURATION);
		matchStartInstantSupplier = Suppliers.memoize(() -> sessionEndInstant.minus(END_DURATION).minusSeconds(gameMode.DEFAULT_TIME_SECONDS));
		matchEndInstantSupplier = Suppliers.memoize(() -> sessionEndInstant.minus(END_DURATION));
	}
	public PlaySession(List<UUID> playerUuids, StageGameMode gameMode, String stageId, Instant sessionEndInstant)
	{
		this.playerUuids = playerUuids;
		this.gameMode = gameMode;
		this.stageId = stageId;
		this.sessionEndInstant = sessionEndInstant;
		matchStartInstantSupplier = Suppliers.memoize(() -> sessionEndInstant.minus(END_DURATION).minusSeconds(gameMode.DEFAULT_TIME_SECONDS));
		matchEndInstantSupplier = Suppliers.memoize(() -> sessionEndInstant.minus(END_DURATION));
	}
	public static Optional<PlaySession> getPlaySession(LivingEntity entity)
	{
		Optional<EntityInfo> infoOptional = EntityInfoCapability.getOptional(entity);
		return infoOptional.flatMap(entityInfo -> getPlaySession(entity, entityInfo));
	}
	public static Optional<PlaySession> getPlaySession(LivingEntity entity, EntityInfo info)
	{
		PlaySession result = null;
		if (info.isPlaying())
		{
			PlaySession session = SaveInfoCapability.get().playSessions().get(info.getPlayingStageId());
			if (session != null && session.playerUuids.contains(entity.getUUID()))
				result = session;
		}
		return Optional.ofNullable(result);
	}
	/**
	 * Ticks all the play session related actions.
	 *
	 * @return false if the play session has ended, otherwise, true
	 */
	public boolean tick(MinecraftServer server)
	{
		Stage stage = SaveInfoCapability.get().stages().get(stageId);
		if (stage == null)
		{
			end(server, EndReason.STAGE_NOT_FOUND);
			return false;
		}
		ServerLevel world = server != null ? stage.getStageWorld(server) : null;
		if (server != null)
		{
			if (playerUuids.isEmpty() || playerUuids.stream().allMatch(v -> world.getPlayerByUUID(v) == null))
			{
				end(server, EndReason.NO_PLAYERS);
				return false;
			}
		}
		if (Instant.now().isAfter(sessionEndInstant) && !gameMode.overtimeChecker.test(this, world))
		{
			end(server, EndReason.NORMAL);
			return false;
		}
		gameMode.tick.consume(this, world);
		return true;
	}
	public void end(MinecraftServer server, EndReason endReason)
	{
		if (server != null)
		{
			Stage stage = SaveInfoCapability.get().stages().get(stageId);
			ServerLevel world = stage.getStageWorld(server);
			gameMode.onEnd.consume(this, world);

			playerUuids.forEach(uuid ->
			{
				if (world == null)
					return;

				Player plr = world.getPlayerByUUID(uuid);
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
	public Instant getMatchStartInstant()
	{
		return matchStartInstantSupplier.get();
	}
	public Instant getMatchEndInstant()
	{
		return matchEndInstantSupplier.get();
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
		STAGE_NOT_FOUND,
		FORCED
	}
}
