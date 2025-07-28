package net.splatcraft.data.capabilities.structs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.data.PlaySession;
import net.splatcraft.data.capabilities.SaveInfoCapability;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.InkBlockUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

public record PlayerInfo(
	boolean isInitialized,
	Object2ObjectMap<Integer, ItemStack> matchInventory,
	Optional<PlayingData> playingData,
	ItemStack inkBand
)
{
	public static final MapCodec<PlayerInfo> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
		Codec.BOOL.optionalFieldOf("is_initialized", false).forGetter(PlayerInfo::isInitialized),
		CodecUtils.hashMapCodec(Codec.STRING.comapFlatMap(v -> CodecUtils.exceptionCatchDataResult(() -> Integer.decode(v)), Object::toString), ItemStack.CODEC).optionalFieldOf("match_inventory", new Object2ObjectOpenHashMap<>(41)).forGetter(PlayerInfo::matchInventory),
		PlayingData.CODEC.optionalFieldOf("playing_data").forGetter(PlayerInfo::playingData),
		ItemStack.OPTIONAL_CODEC.fieldOf("ink_band").forGetter(PlayerInfo::inkBand)
	).apply(inst, PlayerInfo::new));
	public static final Codec<PlayerInfo> CODEC = MAP_CODEC.codec();
	public PlayerInfo()
	{
		this(false, new Object2ObjectOpenHashMap<>(), Optional.empty(), ItemStack.EMPTY);
	}
	public PlayerInfo setInitialized(boolean init)
	{
		if (Objects.equals(isInitialized, init))
			return this;
		return new PlayerInfo(init, matchInventory, playingData, inkBand);
	}
	public PlayerInfo setMatchInventory(Object2ObjectOpenHashMap<Integer, ItemStack> matchInventory)
	{
		if (Objects.equals(this.matchInventory, matchInventory))
			return this;
		
		return new PlayerInfo(isInitialized, matchInventory, playingData, inkBand);
	}
	public PlayerInfo setPlayingData(Optional<PlayingData> playingData)
	{
		if (Objects.equals(this.playingData, playingData))
			return this;
		
		if (playingData.isPresent() && playingData.get().playingStageId == null)
			return new PlayerInfo(isInitialized, matchInventory, Optional.empty(), inkBand);
		
		return new PlayerInfo(isInitialized, matchInventory, playingData, inkBand);
	}
	public PlayerInfo setInkBand(ItemStack stack)
	{
		if (Objects.equals(this.inkBand, stack))
			return this;
		return new PlayerInfo(isInitialized, matchInventory, playingData, stack);
	}
	public InkBlockUtils.InkType getInkType()
	{
		return InkBlockUtils.getInkTypeFromStack(inkBand);
	}
	public MatchState calculateMatchState(LivingEntity entity)
	{
		if (isPlaying())
		{
			PlaySession session = SaveInfoCapability.get().playSessions().get(getPlayingStageId());
			if (session != null && session.playerUuids.contains(entity.getUUID()))
			{
				long now = entity.level().getGameTime();
				if (now < session.getMatchStartTime())
					return MatchState.INTRO;
				else if (now > session.getMatchEndTime() && session.hasEnded())
					return MatchState.SEEING_RESULTS;
				else if (isMatchRespawning())
					return MatchState.RESPAWNING;
				return MatchState.PLAYING;
			}
		}
		return MatchState.NOT_PLAYING;
	}
	public boolean isPlaying()
	{
		return playingData.isPresent() && playingData.get().playingStageId != null;
	}
	public String getPlayingStageId()
	{
		return playingData.map(data -> data.playingStageId).orElse(null);
	}
	public boolean isMatchRespawning()
	{
		return playingData.isPresent() && playingData.get().isMatchRespawning();
	}
	public int getMatchRespawnTimeLeft()
	{
		return playingData.map(data -> data.respawnData & 0x7fffffff).orElse(0);
	}
	public PlayerInfo updateOrCreatePlayingData(UnaryOperator<PlayingData> operator)
	{
		PlayingData data = playingData.orElse(PlayingData.DEFAULT);
		return setPlayingData(Optional.ofNullable(operator.apply(data)));
	}
	public PlayerInfo setPlayingStageId(String stageId)
	{
		return updateOrCreatePlayingData(v -> v.setPlayingStageId(stageId));
	}
	public PlayerInfo setIsMatchRespawning(boolean isMatchRespawning)
	{
		return updateOrCreatePlayingData(v -> v.setIsMatchRespawning(isMatchRespawning));
	}
	public PlayerInfo setMatchRespawnTimeLeft(int time)
	{
		return updateOrCreatePlayingData(v -> v.setMatchRespawnTimeLeft(time));
	}
	public enum MatchState
	{
		NOT_PLAYING(false, false, false),
		PLAYING(false, false, true),
		INTRO(true, true, true),
		RESPAWNING(true, true, true),
		SEEING_RESULTS(true, true, true);
		public final boolean movementDisabled, modifiesCamera, playing;
		MatchState(boolean movementDisabled, boolean modifiesCamera, boolean playing)
		{
			this.movementDisabled = movementDisabled;
			this.modifiesCamera = modifiesCamera;
			this.playing = playing;
		}
	}
	public record PlayingData(
		String playingStageId,
		int respawnData // leftmost bit is used as a flag
	)
	{
		public static final Codec<PlayingData> CODEC = RecordCodecBuilder.create(
			inst -> inst.group(
				Codec.STRING.optionalFieldOf("playing_stage_id").forGetter(playingData1 -> Optional.ofNullable(playingData1.playingStageId())),
				Codec.INT.optionalFieldOf("respawn_time", 0).forGetter(PlayingData::respawnData)
			).apply(inst, (Optional<String> playingStageId, Integer respawnTime) -> new PlayingData(playingStageId.orElse(null), respawnTime))
		);
		public static final PlayingData DEFAULT = new PlayingData(null, 0);
		public PlayingData setPlayingStageId(String playingStageId)
		{
			if (Objects.equals(this.playingStageId, playingStageId))
				return this;
			return new PlayingData(playingStageId, respawnData);
		}
		public PlayingData setRespawnData(int respawnData)
		{
			if (Objects.equals(this.respawnData, respawnData))
				return this;
			return new PlayingData(playingStageId, respawnData);
		}
		public PlayingData setIsMatchRespawning(boolean respawning)
		{
			final int lastBitActive = 0x80000000;
			int newState;
			if (respawning)
				newState = respawnData | lastBitActive;
			else
				newState = respawnData & ~lastBitActive;
			return setRespawnData(newState);
		}
		public PlayingData setMatchRespawnTimeLeft(int time)
		{
			return setRespawnData(respawnData & 0x80000000 | time & 0x7FFFFFFF);
		}
		public boolean isMatchRespawning()
		{
			return (respawnData & 0x80000000) != 0;
		}
		public int getMatchRespawnTimeLeft()
		{
			return respawnData & 0x7fffffff;
		}
	}
}
