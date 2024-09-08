package net.splatcraft.forge.data.capabilities.playerinfo;

import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.forge.util.ColorUtils;
import net.splatcraft.forge.util.InkBlockUtils;
import net.splatcraft.forge.util.PlayerCharge;
import net.splatcraft.forge.util.PlayerCooldown;

import java.util.Optional;

public class PlayerInfo
{
    private int dodgeCount;
    private int color;
    private boolean isSquid = false;
    private boolean initialized = false;
    private Optional<Direction> climbedDirection = Optional.empty();
    private NonNullList<ItemStack> matchInventory = NonNullList.create();
    private PlayerCooldown playerCooldown = null;
    private PlayerCharge playerCharge = null;
    private ItemStack inkBand = ItemStack.EMPTY;
    private float squidSurgeCharge = 0f;
    private boolean isPlaying;

    public PlayerInfo(int defaultColor)
    {
        color = defaultColor;
    }

    public PlayerInfo()
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

    public int getColor()
    {
        return color;
    }

    public void setColor(int color)
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

    public NonNullList<ItemStack> getMatchInventory()
    {
        return matchInventory;
    }

    public void setMatchInventory(NonNullList<ItemStack> inventory)
    {
        this.matchInventory = inventory;
    }

    public PlayerCooldown getPlayerCooldown()
    {
        return playerCooldown;
    }

    public void setPlayerCooldown(PlayerCooldown cooldown)
    {
        this.playerCooldown = cooldown;
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

    public void setSquidSurgeCharge(float squidSurgeCharge)
    {
        this.squidSurgeCharge = squidSurgeCharge;
    }

    public float getSquidSurgeCharge()
    {
        return squidSurgeCharge;
    }

    public int getDodgeCount()
    {
        return dodgeCount;
    }

    public void setDodgeCount(int dodgeCount)
    {
        this.dodgeCount = dodgeCount;
    }

    public void setPlaying(boolean playing)
    {
        isPlaying = playing;
    }

    public boolean isPlaying()
    {
        return isPlaying;
    }

    public CompoundTag writeNBT(CompoundTag nbt)
    {
        nbt.putInt("DodgeCount", getDodgeCount());
        nbt.putInt("Color", getColor());
        nbt.putBoolean("IsSquid", isSquid());
        nbt.putString("InkType", getInkType().getSerializedName());
        nbt.putBoolean("Initialized", initialized);
        nbt.putFloat("SquidSurgeCharge", getSquidSurgeCharge());
        climbedDirection.ifPresent(direction -> nbt.putInt("ClimbedDirection", direction.get3DDataValue()));

        if (!inkBand.isEmpty())
            nbt.put("InkBand", getInkBand().serializeNBT());

        if (!matchInventory.isEmpty())
        {
            CompoundTag invNBT = new CompoundTag();
            ContainerHelper.saveAllItems(invNBT, matchInventory);
            nbt.put("MatchInventory", invNBT);
        }

        if (playerCooldown != null)
        {
            CompoundTag cooldownNBT = new CompoundTag();
            playerCooldown.writeNBT(cooldownNBT);

            if (playerCooldown.getClass() != PlayerCooldown.class)
            {
                ResourceLocation key = PlayerCooldown.REGISTRY.get().getKey(playerCooldown.getClass());
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

    public void readNBT(CompoundTag nbt)
    {
        setDodgeCount(nbt.getInt("DodgeCount"));
        setColor(ColorUtils.getColorFromNbt(nbt));
        setIsSquid(nbt.getBoolean("IsSquid"));
        setInitialized(nbt.getBoolean("Initialized"));
        setSquidSurgeCharge(nbt.getFloat("SquidSurgeCharge"));

        if (nbt.contains("InkBand"))
            setInkBand(ItemStack.of(nbt.getCompound("InkBand")));
        else setInkBand(ItemStack.EMPTY);

        if (nbt.contains("MatchInventory"))
        {
            NonNullList<ItemStack> nbtInv = NonNullList.withSize(41, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(nbt.getCompound("MatchInventory"), nbtInv);
            setMatchInventory(nbtInv);
        }

        if (nbt.contains("PlayerCooldown"))
        {
            setPlayerCooldown(PlayerCooldown.readNBT(nbt.getCompound("PlayerCooldown")));
        }

        isPlaying = nbt.getBoolean("Playing");
        climbedDirection = nbt.contains("ClimbedDirection") ? Optional.of(Direction.from3DDataValue(nbt.getInt("ClimbedDirection"))) : Optional.empty();
    }
}
