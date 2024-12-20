package net.splatcraft.data.capabilities.playerinfo;

import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Direction;
import net.splatcraft.util.*;

import java.util.Optional;

public class EntityInfo
{
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

    public NbtCompound writeNBT(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
    {
        nbt.putInt("DodgeCount", getDodgeCount());
        nbt.put("Color", getColor().getNbt());
        nbt.putBoolean("IsSquid", isSquid());
        nbt.putBoolean("SquidCancel", didSquidCancelThisTick);
        nbt.putString("InkType", getInkType().getSerializedName());
        nbt.putBoolean("Initialized", initialized);
        nbt.putFloat("SquidSurgeCharge", getSquidSurgeCharge());
        climbedDirection.ifPresent(direction -> nbt.putInt("ClimbedDirection", direction.getId()));

        if (!inkBand.isEmpty())
            nbt.put("InkBand", getInkBand().encode(registryLookup));

        if (!matchInventory.isEmpty())
        {
            NbtCompound invNBT = new NbtCompound();
            Inventories.writeNbt(invNBT, matchInventory, registryLookup);
            nbt.put("MatchInventory", invNBT);
        }

        if (playerCooldown != null)
        {
            NbtCompound cooldownNBT = new NbtCompound();
            playerCooldown.writeNBT(cooldownNBT, registryLookup);

            if (playerCooldown.getClass() != PlayerCooldown.class)
            {
                Identifier key = PlayerCooldown.REGISTRY.getId(playerCooldown.getClass());
                if (key != null)
                {
                    cooldownNBT.putString("CooldownClass", key.toString());
                }
            }

            nbt.put("PlayerCooldown", cooldownNBT);
        }
        nbt.putBoolean("Playing", isPlaying);

        return nbt;
    }

    public void readNBT(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
    {
        setDodgeCount(nbt.getInt("DodgeCount"));
        setColor(InkColor.getFromNbt(nbt.get("Color")));
        setIsSquid(nbt.getBoolean("IsSquid"));
        didSquidCancelThisTick = nbt.getBoolean("SquidCancel");
        setInitialized(nbt.getBoolean("Initialized"));
        setSquidSurgeCharge(nbt.getFloat("SquidSurgeCharge"));

        if (nbt.contains("InkBand"))
            setInkBand(ItemStack.fromNbtOrEmpty(registryLookup, nbt.getCompound("InkBand")));
        else setInkBand(ItemStack.EMPTY);

        if (nbt.contains("MatchInventory"))
        {
            DefaultedList<ItemStack> nbtInv = DefaultedList.ofSize(41, ItemStack.EMPTY);
            Inventories.readNbt(nbt.getCompound("MatchInventory"), nbtInv, registryLookup);
            setMatchInventory(nbtInv);
        }

        if (nbt.contains("PlayerCooldown"))
        {
            setPlayerCooldown(PlayerCooldown.readNBT(registryLookup, nbt.getCompound("PlayerCooldown")));
        }

        isPlaying = nbt.getBoolean("Playing");
        climbedDirection = nbt.contains("ClimbedDirection") ? Optional.of(Direction.byId(nbt.getInt("ClimbedDirection"))) : Optional.empty();
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
