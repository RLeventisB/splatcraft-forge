package net.splatcraft.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.splatcraft.blocks.CrateBlock;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CrateTileEntity extends InkColorTileEntity implements RandomizableContainer
{
	private final NonNullList<ItemStack> inventory = NonNullList.withSize(1, ItemStack.EMPTY);
	private float health;
	private float maxHealth;
	private ResourceKey<LootTable> lootTable = null;
	private long lootTableSeed;
	public CrateTileEntity(BlockPos pos, BlockState state)
	{
		super(SplatcraftTileEntities.crateTileEntity.get(), pos, state);
	}
	public void ink(InkColor color, float damage)
	{
		if (level != null && level.isClientSide())
		{
			return;
		}

		setColor(color);
		health -= damage;
		if (health <= 0)
		{
			level.removeBlock(getBlockPos(), false);

			dropInventory();
		}
		else
		{
			level.setBlock(getBlockPos(), getBlockState().setValue(CrateBlock.STATE, getState()), 2);
		}
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
	public ResourceKey<LootTable> getLootTable()
	{
		return lootTable;
	}
	public void setLootTable(ResourceKey<LootTable> lootTable)
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
	public void loadAdditional(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider lookup)
	{
		super.loadAdditional(nbt, lookup);

		health = nbt.getFloat("Health");
		maxHealth = nbt.getFloat("MaxHealth");
		if (!tryLoadLootTable(nbt))
			ContainerHelper.loadAllItems(nbt, inventory, lookup);

		if (nbt.contains("LootTable"))
		{
			ResourceKey.codec(Registries.LOOT_TABLE).parse(NbtOps.INSTANCE, nbt.get("LootTable"))
				.ifSuccess(encoded -> lootTable = encoded);
		}
	}
	@Override
	public void saveAdditional(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider lookup)
	{
		nbt.putFloat("Health", health);
		nbt.putFloat("MaxHealth", maxHealth);
		ContainerHelper.saveAllItems(nbt, inventory, lookup);

		if (hasLoot())
		{
			ResourceKey.codec(Registries.LOOT_TABLE).encodeStart(NbtOps.INSTANCE, lootTable)
				.ifSuccess(encoded -> nbt.put("LootTable", encoded));
		}

		super.saveAdditional(nbt, lookup);
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
		return inventory.getFirst().isEmpty();
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
			setChanged();
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
		if (stack.getCount() > getMaxStackSize())
		{
			stack.setCount(getMaxStackSize());
		}

		setChanged();
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
	public NonNullList<ItemStack> getInventory()
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