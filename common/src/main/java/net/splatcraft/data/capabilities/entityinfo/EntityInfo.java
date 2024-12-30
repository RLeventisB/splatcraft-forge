package net.splatcraft.data.capabilities.entityinfo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Direction;
import net.splatcraft.util.*;

import java.util.List;
import java.util.Optional;

public class EntityInfo
{
	public static final Codec<EntityInfo> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		Codec.INT.optionalFieldOf("dodge_count", 0).forGetter(EntityInfo::getDodgeCount),
		InkColor.NUMBER_CODEC.optionalFieldOf("color", InkColor.INVALID).forGetter(EntityInfo::getColor),
		Codec.BOOL.optionalFieldOf("is_squid", false).forGetter(EntityInfo::isSquid),
		Codec.BOOL.optionalFieldOf("is_initialized", false).forGetter(EntityInfo::isInitialized),
		Direction.CODEC.optionalFieldOf("climbed_direction").forGetter(EntityInfo::getClimbedDirection),
		ItemStack.CODEC.listOf().optionalFieldOf("match_inventory", DefaultedList.ofSize(41, ItemStack.EMPTY)).forGetter(EntityInfo::getMatchInventory),
		PlayerCooldown.SERIALIZER_CODEC.codec().lenientOptionalFieldOf("player_cooldown").forGetter(v -> Optional.ofNullable(v.getPlayerCooldown())),
		PlayerCharge.CODEC.lenientOptionalFieldOf("player_charge").forGetter(v -> Optional.ofNullable(v.getPlayerCharge())),
		ItemStack.OPTIONAL_CODEC.fieldOf("ink_band").forGetter(EntityInfo::getInkBand),
		Codec.FLOAT.optionalFieldOf("squid_surge_charge", 0f).forGetter(EntityInfo::getSquidSurgeCharge),
		Codec.BOOL.optionalFieldOf("is_playing_match", false).forGetter(EntityInfo::isPlaying),
		Codec.BOOL.optionalFieldOf("did_squid_cancel_this_tick", false).forGetter(EntityInfo::didSquidCancelThisTick)
	).apply(inst, EntityInfo::new));
	private int dodgeCount;
	private InkColor color;
	private boolean isSquid = false;
	private boolean initialized = false;
	private Optional<Direction> climbedDirection = Optional.empty();
	private DefaultedList<ItemStack> matchInventory = DefaultedList.of();
	private PlayerCooldown playerCooldown = null;
	private PlayerCharge playerCharge = null;
	private ItemStack inkBand = ItemStack.EMPTY;
	private float squidSurgeCharge = 0f;
	private boolean isPlaying;
	private boolean didSquidCancelThisTick;
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
	                  Optional<PlayerCooldown> playerCooldown,
	                  Optional<PlayerCharge> playerCharge,
	                  ItemStack inkBand,
	                  float squidSurgeCharge,
	                  boolean isPlaying,
	                  boolean didSquidCancelThisTick)
	{
		this.dodgeCount = dodgeCount;
		this.color = color;
		this.isSquid = isSquid;
		this.initialized = initialized;
		this.climbedDirection = climbedDirection;
		this.matchInventory = DefaultedList.copyOf(ItemStack.EMPTY, matchInventory.toArray(new ItemStack[0]));
		this.playerCooldown = playerCooldown.orElse(null);
		this.playerCharge = playerCharge.orElse(null);
		this.inkBand = inkBand;
		this.squidSurgeCharge = squidSurgeCharge;
		this.isPlaying = isPlaying;
		this.didSquidCancelThisTick = didSquidCancelThisTick;
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
	public PlayerCooldown getPlayerCooldown()
	{
		return playerCooldown;
	}
	public void setPlayerCooldown(PlayerCooldown cooldown)
	{
		playerCooldown = cooldown;
	}
	public boolean hasPlayerCooldown()
	{
		return playerCooldown != null && playerCooldown.getTime() > 0;
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
	public boolean isPlaying()
	{
		return isPlaying;
	}
	public void setPlaying(boolean playing)
	{
		isPlaying = playing;
	}
	public void flagSquidCancel()
	{
		didSquidCancelThisTick = true;
	}
	public boolean didSquidCancelThisTick()
	{
		return didSquidCancelThisTick;
	}
	public void removeSquidCancelFlag()
	{
		didSquidCancelThisTick = false;
	}
}
