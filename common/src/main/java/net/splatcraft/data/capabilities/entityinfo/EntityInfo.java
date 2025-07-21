package net.splatcraft.data.capabilities.entityinfo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.data.PlaySession;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.handlers.SquidFormHandler.SquidState;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.EntityStoredCharge;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.structs.InkColor;

import java.util.Optional;

public class EntityInfo
{
	public static final int SQUID_LAG_DURATION = 10;
	public static final float MIN_SQUID_SURGE_CHARGE = 7;
	public static final float MAX_SQUID_SURGE_CHARGE = 20;
	public static final float SQUID_SURGE_USAGE_OFFSET = 100;
	public static final float SQUID_SURGE_ENDLAG = 20;
	public static final MapCodec<EntityInfo> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
		Codec.INT.optionalFieldOf("dodge_count", 0).forGetter(EntityInfo::getDodgeCount),
		InkColor.RAW_INT_CODEC.optionalFieldOf("color", InkColor.INVALID).forGetter(EntityInfo::getColor),
		Codec.BOOL.optionalFieldOf("is_squid", false).forGetter(EntityInfo::isSquid),
		Codec.BOOL.optionalFieldOf("is_initialized", false).forGetter(EntityInfo::isInitialized),
		Direction.CODEC.optionalFieldOf("climbed_direction").forGetter(EntityInfo::getClimbedDirection),
		CodecUtils.hashMapCodec(Codec.STRING.comapFlatMap(v -> CodecUtils.exceptionCatchDataResult(() -> Integer.decode(v)), Object::toString), ItemStack.CODEC).optionalFieldOf("match_inventory", new Object2ObjectOpenHashMap<>(41)).forGetter(EntityInfo::getMatchInventory),
		EntityAction.SERIALIZER_CODEC.lenientOptionalFieldOf("entity_action").forGetter(v -> Optional.ofNullable(v.getEntityAction())),
		EntityStoredCharge.CODEC.lenientOptionalFieldOf("entity_charge").forGetter(EntityInfo::getStoredCharge),
		ItemStack.OPTIONAL_CODEC.fieldOf("ink_band").forGetter(EntityInfo::getInkBand),
		Codec.FLOAT.optionalFieldOf("squid_surge_charge", 0f).forGetter(EntityInfo::getSquidSurgeState),
		PlayingData.CODEC.optionalFieldOf("playing_data", PlayingData.DEFAULT).forGetter(EntityInfo::playingData),
		Codec.INT.optionalFieldOf("higher_startup_ticks", 0).forGetter(EntityInfo::getHigherStartupTicks),
		SquidState.CODEC.optionalFieldOf("squid_state", SquidState.SURFACED).forGetter(EntityInfo::getSquidState)
	).apply(inst, EntityInfo::new));
	public static final Codec<EntityInfo> CODEC = MAP_CODEC.codec();
	private int dodgeCount;
	private InkColor color;
	private boolean isSquid = false;
	private boolean initialized = false;
	private Optional<Direction> climbedDirection = Optional.empty();
	private Object2ObjectOpenHashMap<Integer, ItemStack> matchInventory = new Object2ObjectOpenHashMap<>();
	private EntityAction entityAction = null;
	private Optional<EntityStoredCharge> storedCharge = Optional.empty();
	private ItemStack inkBand = ItemStack.EMPTY;
	private float squidSurgeState = 0;
	private PlayingData playingData = PlayingData.DEFAULT;
	private int higherStartupTicks;
	private SquidState squidState = SquidState.SURFACED;
	public EntityInfo(InkColor defaultColor)
	{
		color = defaultColor;
	}
	public EntityInfo()
	{
		this(ColorUtils.getRandomStarterColor());
	}
	public EntityInfo(int dodgeCount,
	                  InkColor color,
	                  boolean isSquid,
	                  boolean initialized,
	                  Optional<Direction> climbedDirection,
	                  Object2ObjectOpenHashMap<Integer, ItemStack> matchInventory,
	                  Optional<EntityAction> entityAction,
	                  Optional<EntityStoredCharge> playerCharge,
	                  ItemStack inkBand,
	                  float squidSurgeCharge,
	                  PlayingData playingData,
	                  int higherStartupTicks,
	                  SquidState squidState)
	{
		this.dodgeCount = dodgeCount;
		this.color = color;
		this.isSquid = isSquid;
		this.initialized = initialized;
		this.climbedDirection = climbedDirection;
		this.matchInventory = matchInventory;
		this.entityAction = entityAction.orElse(null);
		this.storedCharge = playerCharge;
		this.inkBand = inkBand;
		this.squidSurgeState = squidSurgeCharge;
		this.playingData = playingData;
		this.higherStartupTicks = higherStartupTicks;
		this.squidState = squidState;
	}
	public SquidState getSquidState()
	{
		return squidState;
	}
	public void setSquidState(SquidState squidState)
	{
		this.squidState = squidState;
	}
	private PlayingData playingData()
	{
		if (playingData == null)
			playingData = PlayingData.DEFAULT;
		return playingData;
	}
	public boolean isInitialized()
	{
		return initialized;
	}
	public void setInitialized(boolean init)
	{
		initialized = init;
	}
	public InkColor getColor()
	{
		return color;
	}
	public void setColor(InkColor color)
	{
		this.color = color;
	}
	public boolean isSquid()
	{
		return isSquid;
	}
	public void setIsSquid(boolean isSquid)
	{
		this.isSquid = isSquid;
	}
	public Optional<Direction> getClimbedDirection()
	{
		return climbedDirection;
	}
	public void setClimbedDirection(Direction direction)
	{
		climbedDirection = Optional.ofNullable(direction);
	}
	public void setClimbedDirection(Optional<Direction> direction)
	{
		climbedDirection = direction;
	}
	public ItemStack getInkBand()
	{
		return inkBand;
	}
	public void setInkBand(ItemStack stack)
	{
		inkBand = stack;
	}
	public InkBlockUtils.InkType getInkType()
	{
		return InkBlockUtils.getInkTypeFromStack(inkBand);
	}
	public Object2ObjectOpenHashMap<Integer, ItemStack> getMatchInventory()
	{
		return matchInventory;
	}
	public void setMatchInventory(Object2ObjectOpenHashMap<Integer, ItemStack> inventory)
	{
		matchInventory = inventory;
	}
	public EntityAction getEntityAction()
	{
		return entityAction;
	}
	public void setEntityAction(EntityAction action, LivingEntity entity)
	{
		if (entityAction != null)
			entityAction.beforeEnd(entity);
		entityAction = action;
	}
	public boolean hasActiveAction()
	{
		return entityAction != null && entityAction.getTime() > 0;
	}
	public Optional<EntityStoredCharge> getStoredCharge()
	{
		return storedCharge;
	}
	public void setStoredCharge(EntityStoredCharge charge)
	{
		storedCharge = Optional.ofNullable(charge);
	}
	public void setStoredCharge(Optional<EntityStoredCharge> charge)
	{
		storedCharge = charge;
	}
	public boolean isDoingSquidSurge()
	{
		return squidSurgeState >= SQUID_SURGE_USAGE_OFFSET;
	}
	public boolean canChargeSquidSurge()
	{
		return squidSurgeState >= 0 && squidSurgeState < SQUID_SURGE_USAGE_OFFSET;
	}
	public float getSquidSurgeState()
	{
		return squidSurgeState;
	}
	public void setSquidSurgeState(float squidSurgeState)
	{
		this.squidSurgeState = squidSurgeState;
	}
	public float getSquidSurgeCharge()
	{
		return squidSurgeState > SQUID_SURGE_USAGE_OFFSET ? 0 : squidSurgeState;
	}
	public float getSquidSurgePower()
	{
		return squidSurgeState - SQUID_SURGE_USAGE_OFFSET;
	}
	public void chargeSquidSurge()
	{
		if (squidSurgeState < MAX_SQUID_SURGE_CHARGE)
			this.squidSurgeState = Math.min(squidSurgeState + 1, MAX_SQUID_SURGE_CHARGE);
	}
	public boolean flagSquidSurgeUsage()
	{
		if (squidSurgeState < MIN_SQUID_SURGE_CHARGE)
			return false;
		
		squidSurgeState = SQUID_SURGE_USAGE_OFFSET + squidSurgeState;
		return true;
	}
	public void flagSquidSurgeEnd()
	{
		squidSurgeState = -SQUID_SURGE_ENDLAG;
	}
	public int getDodgeCount()
	{
		return dodgeCount;
	}
	public void setDodgeCount(int dodgeCount)
	{
		this.dodgeCount = dodgeCount;
	}
	private int getHigherStartupTicks()
	{
		return higherStartupTicks;
	}
	public void flagSquidCancel()
	{
		flagSquidCancel(SQUID_LAG_DURATION);
	}
	public void flagSquidCancel(int frames)
	{
		higherStartupTicks = frames;
	}
	public boolean hasHigherStartup()
	{
		return higherStartupTicks > 0;
	}
	public void resetHigherStartup()
	{
		higherStartupTicks = 0;
	}
	public void reduceSquidAnimationTick()
	{
		if (higherStartupTicks > 0)
			higherStartupTicks--;
	}
	public boolean isPlaying()
	{
		return playingData != null && playingData.playingStageId != null;
	}
	public String getPlayingStageId()
	{
		return playingData != null ? playingData.playingStageId : null;
	}
	public void setPlayingStageId(String stageId)
	{
		playingData = new PlayingData(stageId, playingData != null ? (playingData.respawnData & 0x7FFFFFFF) : 0);
	}
	public int getMatchRespawnTimeLeft()
	{
		if (playingData == null)
			playingData = PlayingData.DEFAULT;
		
		return playingData.respawnData & 0x7fffffff;
	}
	public void setMatchRespawnTimeLeft(int time)
	{
		playingData = new PlayingData(getPlayingStageId(), (playingData.respawnData & 0x80000000) | (time & 0x7FFFFFFF));
	}
	public boolean isMatchRespawning()
	{
		return playingData != null && (playingData.respawnData & 0x80000000) != 0;
	}
	public MatchState getMatchState(LivingEntity entity)
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
	public void setIsMatchRespawning(boolean respawning)
	{
		final int lastBitActive = 0x80000000;
		if (playingData != null)
		{
			if (respawning)
				playingData = new PlayingData(playingData.playingStageId, playingData.respawnData | lastBitActive);
			else
				playingData = new PlayingData(playingData.playingStageId, playingData.respawnData & 0x7FFFFFFF);
		}
		else
			playingData = new PlayingData(null, respawning ? lastBitActive : 0);
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
	}
}
