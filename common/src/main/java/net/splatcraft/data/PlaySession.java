package net.splatcraft.data;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.blocks.StageMarkerBlock;
import net.splatcraft.data.capabilities.SaveInfoCapability;
import net.splatcraft.data.capabilities.structs.EntityInfo;
import net.splatcraft.handlers.WeaponHandler;
import net.splatcraft.items.SpecialProviderItem;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.SendPlaySessionEndPacket;
import net.splatcraft.network.s2c.SendPlaySessionUpdatePacket;
import net.splatcraft.platform.Components;
import net.splatcraft.platform.Services;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.tileentities.StageMarkerTileEntity;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public final class PlaySession
{
	public static final long INTRO_DURATION = 15 * 20;
	public static final long END_DURATION = 3 * 20;
	public static final MapCodec<PlaySession> CODEC = new MapCodec<>()
	{
		public static final MapCodec<PlaySession> NO_CUSTOM_DATA_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
			
			UUIDUtil.CODEC.listOf().fieldOf("players").forGetter(v -> v.playerUuids),
			BlockPos.CODEC.listOf().fieldOf("cached_positions").forGetter(v -> v.markerCachedPositions),
			StageGameMode.CODEC.fieldOf("game_mode").forGetter(v -> v.gameMode),
			CodecUtils.hashMapCodec(InkColor.CODEC, TeamScore.CODEC).fieldOf("scores").forGetter(v -> v.scores),
			Codec.BOOL.fieldOf("has_ended").forGetter(v -> v.hasEnded),
			Codec.STRING.fieldOf("stage_id").forGetter(v -> v.stageId),
			Codec.LONG.fieldOf("match_start_time").forGetter(v -> v.matchStartTime),
			Codec.LONG.fieldOf("match_end_time").forGetter(v -> v.matchEndTime),
			Codec.LONG.fieldOf("session_end_time").forGetter(v -> v.sessionEndTime)
		).apply(inst, PlaySession::new));
		@Override
		public <T> RecordBuilder<T> encode(PlaySession input, DynamicOps<T> ops, RecordBuilder<T> prefix)
		{
			NO_CUSTOM_DATA_CODEC.encode(input, ops, prefix);
			if (input.gameMode.customDataCodec != null)
				prefix.add("custom_data", input.gameMode.customDataCodec.encodeStart(ops, input.customData));
			return prefix;
		}
		@Override
		public <T> DataResult<PlaySession> decode(DynamicOps<T> ops, MapLike<T> input)
		{
			AtomicReference<DataResult<PlaySession>> result = new AtomicReference<>(NO_CUSTOM_DATA_CODEC.decode(ops, input));
			result.get().ifSuccess(session ->
			{
				if (session.gameMode.customDataCodec != null)
				{
					DataResult<Object> customDataResult = session.gameMode.customDataCodec.parse(ops, input.get("custom_data"));
					if (customDataResult instanceof DataResult.Error<Object> error)
						result.set(DataResult.error(error.messageSupplier()));
					else
						result.get().getOrThrow().customData = customDataResult.getOrThrow();
				}
			});
			return result.get();
		}
		@Override
		public <T> Stream<T> keys(DynamicOps<T> ops)
		{
			return NO_CUSTOM_DATA_CODEC.keys(ops);
		}
	};
	public static final StreamCodec<ByteBuf, PlaySession> STREAM_CODEC = new StreamCodec<>()
	{
		public static final StreamCodec<ByteBuf, PlaySession> NO_CUSTOM_DATA_CODEC = CodecUtils.streamCodecComposite(
			UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), v -> v.playerUuids,
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), v -> v.markerCachedPositions,
			CodecUtils.createEnumPacketCodec(StageGameMode::values), v -> v.gameMode,
			ByteBufCodecs.map(Object2ObjectOpenHashMap::new, InkColor.STREAM_CODEC, TeamScore.STREAM_CODEC), v -> v.scores,
			ByteBufCodecs.BOOL, v -> v.hasEnded,
			ByteBufCodecs.STRING_UTF8, v -> v.stageId,
			ByteBufCodecs.VAR_LONG, v -> v.matchStartTime,
			ByteBufCodecs.VAR_LONG, v -> v.matchEndTime,
			ByteBufCodecs.VAR_LONG, v -> v.sessionEndTime,
			PlaySession::new
		);
		@Override
		public @NotNull PlaySession decode(@NotNull ByteBuf buf)
		{
			PlaySession session = NO_CUSTOM_DATA_CODEC.decode(buf);
			if (session.gameMode.customDataStreamCodec != null)
				session.customData = session.gameMode.customDataStreamCodec.decode(buf);
			return session;
		}
		@Override
		public void encode(@NotNull ByteBuf buf, @NotNull PlaySession session)
		{
			NO_CUSTOM_DATA_CODEC.encode(buf, session);
			if (session.gameMode.customDataStreamCodec != null)
				session.gameMode.customDataStreamCodec.encode(buf, session.customData);
		}
	};
	public final List<UUID> playerUuids;
	public final List<BlockPos> markerCachedPositions = new ArrayList<>();
	public final Object2ObjectOpenHashMap<InkColor, TeamScore> scores = new Object2ObjectOpenHashMap<>();
	public final StageGameMode gameMode;
	public final String stageId;
	public Object customData;
	private boolean hasEnded;
	private long sessionEndTime, matchStartTime, matchEndTime;
	public boolean hasEnded()
	{
		return hasEnded;
	}
	public PlaySession(Collection<ServerPlayer> players, Stage stage, StageGameMode gameMode)
	{
		playerUuids = players.stream().map(Player::getUUID).toList();
		players.forEach(player ->
		{
			player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 1, false, false));
			WeaponHandler.resetLastGroundedPos(player);
			
			EntityInfo info = Components.ENTITY_INFO.get(player);
			info.setIsSquid(true);
			info.setPlayingStageId(stage.id);
			for (ItemStack providerStack : CommonUtils.getItemsInInventory(player, v -> v.getItem() instanceof SpecialProviderItem))
			{
				providerStack.update(SplatcraftComponents.SPECIAL_PROVIDER_DATA,
					SplatcraftComponents.SpecialProviderData.DEFAULT,
					v -> v.withStoredCharge(0f));
			}
		});
		
		this.gameMode = gameMode;
		stageId = stage.id;
		customData = null;
		recalculateMarkerPositions();
		calculateTimes(gameMode.DEFAULT_TIME_SECONDS, stage.getStageWorld(Services.PLATFORM.getServerInstance()).getGameTime());
		initializeMarkers(false);
	}
	public PlaySession(List<UUID> playerUuids, List<BlockPos> cachedMarkerPos, StageGameMode gameMode, Object2ObjectOpenHashMap<InkColor, TeamScore> scores, boolean ended, String stageId, long matchStartTime, long matchEndTime, long sessionEndTime)
	{
		this(playerUuids, cachedMarkerPos, gameMode, scores, ended, stageId, matchStartTime, matchEndTime, sessionEndTime, null);
	}
	public PlaySession(List<UUID> playerUuids, List<BlockPos> cachedMarkerPos, StageGameMode gameMode, Object2ObjectOpenHashMap<InkColor, TeamScore> scores, boolean ended, String stageId, long matchStartTime, long matchEndTime, long sessionEndTime, Object customData)
	{
		this.playerUuids = playerUuids;
		this.gameMode = gameMode;
		this.stageId = stageId;
		this.hasEnded = ended;
		this.matchStartTime = matchStartTime;
		this.matchEndTime = matchEndTime;
		this.sessionEndTime = sessionEndTime;
		this.scores.putAll(scores);
		this.customData = customData;
		markerCachedPositions.addAll(cachedMarkerPos);
	}
	private void calculateTimes(int gameModeDuration, long now)
	{
		matchStartTime = now + INTRO_DURATION;
		matchEndTime = matchStartTime + gameModeDuration * 20L;
		sessionEndTime = matchEndTime + END_DURATION;
	}
	private void setEndNow(long now)
	{
		matchEndTime = now;
		sessionEndTime = matchEndTime + END_DURATION;
		hasEnded = true;
	}
	public void resetTeamPoints()
	{
		Stage stage = getStage();
		scores.clear();
		for (InkColor color : stage.getTeamIds().stream().map(stage::getTeamColor).toList())
		{
			scores.put(color, new TeamScore());
		}
	}
	public Level getLevelCommon()
	{
		MinecraftServer server = Services.PLATFORM.getServerInstance();
		if (server == null || server.isSingleplayer())
		{
			Level clientWorld = getClientWorld();
			if (clientWorld != null)
				return clientWorld;
		}
		
		return getStage().getStageWorld(server);
	}
	@OnlyIn(Dist.CLIENT)
	private Level getClientWorld()
	{
		return ClientUtils.getClient().level;
	}
	/**
	 * Ticks all the play session related actions.
	 *
	 * @return false if the play session has ended, otherwise, true
	 */
	public boolean tick(@Nullable MinecraftServer server)
	{
		Stage stage = SaveInfoCapability.get().stages().get(stageId);
		if (stage == null)
		{
			end(server, EndReason.STAGE_NOT_FOUND);
			return false;
		}
		Level world = server == null ? getClientWorld() : stage.getStageWorld(server);
		if (world != null)
		{
			if (playerUuids.isEmpty() || (!world.players().isEmpty() && playerUuids.stream().allMatch(v -> world.getPlayerByUUID(v) == null)))
			{
				end(server, EndReason.NO_PLAYERS);
				return false;
			}
			
			long now = world.getGameTime();
			
			if (now < matchStartTime)
				return true;
			
			if (!hasEnded)
			{
				boolean running = gameMode.tick.test(this, world);
				
				if (!running || now > matchEndTime && gameMode.overtimeChecker.test(this, world) == 0)
				{
					setEndNow(now);
					if (!world.isClientSide())
						SplatcraftPacketHandler.sendToDim(new SendPlaySessionUpdatePacket(this), world.dimension());
				}
			}
			if (hasEnded && now > sessionEndTime)
			{
				end(server, EndReason.NORMAL);
				return false;
			}
		}
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
				
				Components.ENTITY_INFO.getOrCreate(plr).setPlayingStageId(null);
			});
			SaveInfoCapability.get().playSessions().remove(stageId);
			SplatcraftPacketHandler.sendToAll(new SendPlaySessionEndPacket(stageId, playerUuids));
		}
	}
	public long getMatchStartTime()
	{
		return matchStartTime;
	}
	public long getMatchEndTime()
	{
		return matchEndTime;
	}
	private void recalculateMarkerPositions()
	{
		markerCachedPositions.clear();
		
		List<BlockPos> builder = new ArrayList<>();
		Stage stage = getStage();
		stage.iterateChunkPos(pos ->
		{
			List<StageMarkerTileEntity> markers = StageMarkerBlock.getMarkersInChunkPos(stage.getStageWorld(Services.PLATFORM.getServerInstance()), pos);
			
			builder.addAll(markers.stream().filter(marker ->
			{
				return marker != null && !builder.contains(marker.getBlockPos());
			}).map(BlockEntity::getBlockPos).toList());
		});
		markerCachedPositions.addAll(builder);
	}
	private void initializeMarkers(boolean check)
	{
		List<BlockPos> list = getMarkersPositions(check);
		for (BlockPos pos : list)
		{
			ChunkPos chunkPos = new ChunkPos(pos);
			BlockEntity blockEntity = getLevelCommon().getChunk(chunkPos.x, chunkPos.z).getBlockEntity(pos);
			if (!(blockEntity instanceof StageMarkerTileEntity marker))
			{
				continue;
			}
			
			marker.setActive(false);
			switch (gameMode)
			{
				case SPLAT_ZONES ->
					marker.setActive(marker.getMarkerType() == StageMarkerTileEntity.MarkerType.SPLAT_ZONE);
			}
		}
	}
	public List<BlockPos> getMarkersPositions()
	{
		return getMarkersPositions(true);
	}
	public List<BlockPos> getMarkersPositions(boolean check)
	{
		if (check && Services.PLATFORM.getServerInstance() != null)
		{
			for (int i = 0; i < markerCachedPositions.size(); i++)
			{
				if (invalidMarker(markerCachedPositions.get(i)))
				{
					recalculateMarkerPositions();
					return markerCachedPositions;
				}
			}
		}
		return markerCachedPositions;
	}
	private boolean invalidMarker(BlockPos pos)
	{
		return pos == null || !(getLevelCommon().getBlockEntity(pos) instanceof StageMarkerTileEntity);
	}
	public List<StageMarkerTileEntity> getMarkers(boolean check)
	{
		Level level = getLevelCommon();
		List<StageMarkerTileEntity> list = new ArrayList<>();
		for (BlockPos v : getMarkersPositions(check))
		{
			StageMarkerTileEntity blockEntity = (StageMarkerTileEntity) level.getBlockEntity(v);
			if (blockEntity != null)
			{
				list.add(blockEntity);
			}
		}
		return list;
	}
	public static Optional<PlaySession> getPlaySession(LivingEntity entity)
	{
		Optional<EntityInfo> infoOptional = Components.ENTITY_INFO.getOptional(entity);
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
	public static ArrayList<PlaySession> getAllPlaySessions()
	{
		return new ArrayList<>(SaveInfoCapability.get().playSessions().values());
	}
	@Override
	public String toString()
	{
		return "PlaySession{" +
			"customData=" + customData +
			", playerUuids=" + playerUuids +
			", markerCachedPositions=" + markerCachedPositions +
			", scores=" + scores +
			", gameMode=" + gameMode +
			", stageId='" + stageId + '\'' +
			", sessionEndTime=" + sessionEndTime +
			", matchStartTime=" + matchStartTime +
			", matchEndTime=" + matchEndTime +
			'}';
	}
	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof PlaySession session)) return false;
		return sessionEndTime == session.sessionEndTime && matchStartTime == session.matchStartTime && matchEndTime == session.matchEndTime && Objects.equals(playerUuids, session.playerUuids) && Objects.equals(markerCachedPositions, session.markerCachedPositions) && Objects.equals(scores, session.scores) && gameMode == session.gameMode && Objects.equals(stageId, session.stageId) && Objects.equals(customData, session.customData);
	}
	@Override
	public int hashCode()
	{
		return Objects.hash(playerUuids, markerCachedPositions, scores, gameMode, stageId, customData, sessionEndTime, matchStartTime, matchEndTime);
	}
	public Stage getStage()
	{
		return Stage.getStage(stageId);
	}
	public record TeamScore(
		int score,
		int penalty
	)
	{
		public static final StreamCodec<ByteBuf, TeamScore> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TeamScore::score,
			ByteBufCodecs.VAR_INT, TeamScore::penalty,
			TeamScore::new
		);
		public TeamScore()
		{
			this(0, 0);
		}
		public static final Codec<TeamScore> CODEC = RecordCodecBuilder.create(
			inst -> inst.group(
				Codec.INT.fieldOf("score").forGetter(TeamScore::score),
				Codec.INT.fieldOf("penalty").forGetter(TeamScore::penalty)
			).apply(inst, TeamScore::new)
		);
		public TeamScore withData(int score, int penalty)
		{
			return new TeamScore(score, penalty);
		}
		public TeamScore withScore(int score)
		{
			return new TeamScore(score, penalty);
		}
		public TeamScore withPenalty(int penalty)
		{
			return new TeamScore(score, penalty);
		}
	}
	public enum EndReason
	{
		NO_PLAYERS,
		NORMAL,
		STAGE_NOT_FOUND,
		FORCED
	}
}
