package net.splatcraft.tileentities;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.splatcraft.blocks.CrateBlock;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CrateTileEntity extends InkColorTileEntity implements LootableInventory
{
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);
    private float health;
    private float maxHealth;
    private RegistryKey<LootTable> lootTable = null;
    private long lootTableSeed;

    public CrateTileEntity(BlockPos pos, BlockState state)
    {
        super(SplatcraftTileEntities.crateTileEntity.get(), pos, state);
    }

    public void ink(InkColor color, float damage)
    {
        if (world != null && world.isClient())
        {
            return;
        }

        setColor(color);
        health -= damage;
        if (health <= 0)
        {
            world.removeBlock(getPos(), false);

            dropInventory();
        }
        else
        {
            world.setBlockState(getPos(), getCachedState().with(CrateBlock.STATE, getState()), 2);
        }
    }

    public void dropInventory()
    {
        if (world != null && world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS))
        {
            getDrops().forEach(stack -> CommonUtils.blockDrop(world, getPos(), stack));
        }
    }

    public List<ItemStack> getDrops()
    {
        return hasLoot() ? CrateBlock.generateLoot(world, this, getCachedState(), 0f) : getInventory();
    }

    public RegistryKey<LootTable> getLootTable()
    {
        return lootTable;
    }

    public void setLootTable(RegistryKey<LootTable> lootTable)
    {
        this.lootTable = lootTable;
    }

    @Override
    public long getLootTableSeed()
    {
        return lootTableSeed;
    }

    @Override
    public void setLootTableSeed(long lootTableSeed)
    {
        this.lootTableSeed = lootTableSeed;
    }

    @Override
    public void readNbt(@NotNull NbtCompound nbt, RegistryWrapper.WrapperLookup lookup)
    {
        super.readNbt(nbt, lookup);

        health = nbt.getFloat("Health");
        maxHealth = nbt.getFloat("MaxHealth");
        if (!readLootTable(nbt))
            Inventories.readNbt(nbt, inventory, lookup);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup)
    {
        nbt.putFloat("Health", health);
        nbt.putFloat("MaxHealth", maxHealth);
        Inventories.writeNbt(nbt, inventory, lookup);

        if (hasLoot())
            nbt.put("LootTable", LootTable.CODEC.encode(lookup.createRegistryLookup().getOptionalEntry(RegistryKeys.LOOT_TABLE, lootTable).get().value(), NbtOps.INSTANCE, nbt).getOrThrow());
        // WHAT THE FUCK please tell me there is an easier way to do this

        super.writeNbt(nbt, lookup);
    }

    @Override
    public int size()
    {
        return getCachedState().getBlock() instanceof CrateBlock && hasLoot() ? 0 : 1;
    }

    private boolean hasLoot()
    {
        return lootTable != null;
    }

    @Override
    public boolean isEmpty()
    {
        return inventory.getFirst().isEmpty();
    }

    @Override
    public @NotNull ItemStack getStack(int index)
    {
        return inventory.get(index);
    }

    @Override
    public @NotNull ItemStack removeStack(int index, int count)
    {
        if (getCachedState().getBlock() instanceof CrateBlock && hasLoot())
        {
            return ItemStack.EMPTY;
        }

        ItemStack itemstack = Inventories.splitStack(inventory, index, count);
        if (!itemstack.isEmpty())
        {
            markDirty();
        }

        return itemstack;
    }

    @Override
    public @NotNull ItemStack removeStack(int index)
    {
        return Inventories.removeStack(inventory, index);
    }

    @Override
    public void setStack(int index, @NotNull ItemStack stack)
    {
        inventory.set(index, stack);
        if (stack.getCount() > getMaxCountPerStack())
        {
            stack.setCount(getMaxCountPerStack());
        }

        markDirty();
    }

    @Override
    public boolean canPlayerUse(@NotNull PlayerEntity player)
    {
        return false;
    }

    @Override
    public void clear()
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
        setColor(InkColor.INVALID);
    }

    public float getMaxHealth()
    {
        return maxHealth;
    }

    public void setMaxHealth(float value)
    {
        maxHealth = value;
    }

    public DefaultedList<ItemStack> getInventory()
    {
        return inventory;
    }

    public int getState()
    {
        if (health == maxHealth)
        {
            setColor(InkColor.INVALID);
        }
        return 4 - Math.round(health * 4 / maxHealth);
    }
}