package net.splatcraft.tileentities;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.blocks.CrateBlock;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

public class CrateTileEntity extends InkColorTileEntity implements Container
{
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(1, ItemStack.EMPTY);
    private float health;
    private float maxHealth;
    private ResourceLocation lootTable = null;

    public CrateTileEntity(BlockPos pos, BlockState state)
    {
        super(SplatcraftTileEntities.crateTileEntity.get(), pos, state);
    }

    public void ink(int color, float damage)
    {
        if (level != null && level.isClientSide())
        {
            return;
        }

        setColor(color);
        health -= damage;
        if (health <= 0)
        {
            level.destroyBlock(getBlockPos(), false);

            dropInventory();
        }
        else
        {
            level.setBlock(getBlockPos(), getBlockState().setValue(CrateBlock.STATE, getState()), 2);
        }
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
    }

    public void dropInventory()
    {
        if (level != null && level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS))
        {
            getDrops().forEach(stack -> CommonUtils.blockDrop(level, getBlockPos(), stack));
        }
    }

    public List<ItemStack> getDrops()
    {
        return hasLoot() ? CrateBlock.generateLoot(level, this, getBlockState(), 0f) : getInventory();
    }

    public ResourceLocation getLootTable()
    {
        return lootTable;
    }

    public void setLootTable(ResourceLocation lootTable)
    {
        this.lootTable = lootTable;
    }

    @Override
    public void load(@NotNull NbtCompound nbt)
    {
        super.load(nbt);

        health = nbt.getFloat("Health");
        maxHealth = nbt.getFloat("MaxHealth");
        ContainerHelper.loadAllItems(nbt, inventory);

        if (nbt.contains("LootTable"))
            lootTable = new ResourceLocation(nbt.getString("LootTable"));
    }

    @Override
    public void saveAdditional(NbtCompound nbt)
    {
        nbt.putFloat("Health", health);
        nbt.putFloat("MaxHealth", maxHealth);
        ContainerHelper.saveAllItems(nbt, inventory);

        if (hasLoot())
            nbt.putString("LootTable", lootTable.toString());

        super.saveAdditional(nbt);
    }

    @Override
    public int getContainerSize()
    {
        return getBlockState().getBlock() instanceof CrateBlock && hasLoot() ? 0 : 1;
    }

    private boolean hasLoot()
    {
        return lootTable != null;
    }

    @Override
    public boolean isEmpty()
    {
        return inventory.isEmpty();
    }

    @Override
    public @NotNull ItemStack getItem(int index)
    {
        return inventory.get(index);
    }

    @Override
    public @NotNull ItemStack removeItem(int index, int count)
    {
        if (getBlockState().getBlock() instanceof CrateBlock && hasLoot())
        {
            return ItemStack.EMPTY;
        }

        ItemStack itemstack = ContainerHelper.removeItem(inventory, index, count);
        if (!itemstack.isEmpty())
        {
            this.setChanged();
        }

        return itemstack;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int index)
    {
        return ContainerHelper.takeItem(inventory, index);
    }

    @Override
    public void setItem(int index, @NotNull ItemStack stack)
    {
        inventory.set(index, stack);
        if (stack.getCount() > this.getMaxStackSize())
        {
            stack.setCount(this.getMaxStackSize());
        }

        this.setChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return false;
    }

    @Override
    public void clearContent()
    {
        inventory.clear();
    }

    public float getHealth()
    {
        return health;
    }

    public void setHealth(float value)
    {
        health = value;
    }

    public void resetHealth()
    {
        setHealth(maxHealth);
        setColor(-1);
    }

    public float getMaxHealth()
    {
        return maxHealth;
    }

    public void setMaxHealth(float value)
    {
        maxHealth = value;
    }

    public NonNullList<ItemStack> getInventory()
    {
        return inventory;
    }

    public int getState()
    {
        if (health == maxHealth)
        {
            setColor(-1);
        }
        return 4 - Math.round(health * 4 / maxHealth);
    }
}