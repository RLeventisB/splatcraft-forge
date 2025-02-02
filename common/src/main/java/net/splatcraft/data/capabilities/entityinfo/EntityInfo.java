package net.splatcraft.data.capabilities.entityinfo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Direction;
import net.splatcraft.handlers.SquidFormHandler.SquidState;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.InkColor;
import net.splatcraft.util.PlayerCharge;
import net.splatcraft.util.action.EntityAction;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class EntityInfo
{
	public static final int HIGHER_STARTUP_DURATION = 10;
	public static final Codec<EntityInfo> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		Codec.INT.optionalFieldOf("dodge_count", 0).forGetter(EntityInfo::getDodgeCount),
		InkColor.RAW_INT_CODEC.optionalFieldOf("color", InkColor.INVALID).forGetter(EntityInfo::getColor),
		Codec.BOOL.optionalFieldOf("is_squid", false).forGetter(EntityInfo::isSquid),
		Codec.BOOL.optionalFieldOf("is_initialized", false).forGetter(EntityInfo::isInitialized),
		Direction.CODEC.optionalFieldOf("climbed_direction").forGetter(EntityInfo::getClimbedDirection),
		ItemStack.CODEC.listOf().optionalFieldOf("match_inventory", DefaultedList.ofSize(41, ItemStack.EMPTY)).forGetter(EntityInfo::getMatchInventory),
		EntityAction.SERIALIZER_CODEC.lenientOptionalFieldOf("entity_action").forGetter(v -> Optional.ofNullable(v.getEntityAction())),
		PlayerCharge.CODEC.lenientOptionalFieldOf("player_charge").forGetter(v -> Optional.ofNullable(v.getPlayerCharge())),
		ItemStack.OPTIONAL_CODEC.fieldOf("ink_band").forGetter(EntityInfo::getInkBand),
		Codec.FLOAT.optionalFieldOf("squid_surge_charge", 0f).forGetter(EntityInfo::getSquidSurgeCharge),
		PlayingData.CODEC.optionalFieldOf("playing_data", new PlayingData(false, 0)).forGetter(EntityInfo::playingData),
		Codec.INT.optionalFieldOf("higher_startup_ticks", 0).forGetter(EntityInfo::getHigherStartupTicks),
		SquidState.CODEC.optionalFieldOf("squid_state", SquidState.SURFACED).forGetter(EntityInfo::getSquidState)
	).apply(inst, EntityInfo::new));
	private int dodgeCount;
	private InkColor color;
	private boolean isSquid = false;
	private boolean initialized = false;
	private Optional<Direction> climbedDirection = Optional.empty();
	private DefaultedList<ItemStack> matchInventory = DefaultedList.of();
	private EntityAction entityAction = null;
	private PlayerCharge playerCharge = null;
	private ItemStack inkBand = ItemStack.EMPTY;
	private float squidSurgeCharge = 0f;
	private PlayingData playingData = PlayingData.DEFAULT.get();
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
	                  List<ItemStack> matchInventory,
	                  Optional<EntityAction> entityAction,
	                  Optional<PlayerCharge> playerCharge,
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
		this.matchInventory = DefaultedList.copyOf(ItemStack.EMPTY, matchInventory.toArray(new ItemStack[0]));
		this.entityAction = entityAction.orElse(null);
		this.playerCharge = playerCharge.orElse(null);
		this.inkBand = inkBand;
		this.squidSurgeCharge = squidSurgeCharge;
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
			playingData = PlayingData.DEFAULT.get();
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
		climbedDirection = direction == null ? Optional.empty() : Optional.of(direction);
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
	public DefaultedList<ItemStack> getMatchInventory()
	{
		return matchInventory;
	}
	public void setMatchInventory(DefaultedList<ItemStack> inventory)
	{
		matchInventory = inventory;
	}
	public EntityAction getEntityAction()
	{
		return entityAction;
	}
	public void setEntityAction(EntityAction action)
	{
		entityAction = action;
	}
	public boolean hasActiveAction()
	{
		return entityAction != null && entityAction.getTime() > 0;
	}
	public PlayerCharge getPlayerCharge()
	{
		return playerCharge;
	}
	public void setPlayerCharge(PlayerCharge charge)
	{
		playerCharge = charge;
	}
	public float getSquidSurgeCharge()
	{
		return squidSurgeCharge;
	}
	public void setSquidSurgeCharge(float squidSurgeCharge)
	{
		this.squidSurgeCharge = squidSurgeCharge;
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
		flagSquidCancel(HIGHER_STARTUP_DURATION);
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
		return playingData != null && playingData.isPlaying;
	}
	public void setPlaying(boolean playing)
	{
		playingData = new PlayingData(playing, playingData != null ? playingData.respawnTime : 0);
	}
	public int getMatchRespawnTimeLeft()
	{
		if (playingData == null)
			playingData = PlayingData.DEFAULT.get();
		return playingData.respawnTime;
	}
	public void setMatchRespawnTimeLeft(int time)
	{
		playingData = new PlayingData(isPlaying(), time);
	}
	public record PlayingData(
		boolean isPlaying,
		int respawnTime
	)
	{
		public static final Codec<PlayingData> CODEC = RecordCodecBuilder.create(
			inst -> inst.group(
				Codec.BOOL.optionalFieldOf("is_playing", false).forGetter(PlayingData::isPlaying),
				Codec.INT.optionalFieldOf("respawn_time", 0).forGetter(PlayingData::respawnTime)
			).apply(inst, PlayingData::new)
		);
		public static final Supplier<PlayingData> DEFAULT = () -> new PlayingData(false, 0);
	}
}
