package net.splatcraft.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.blocks.InkVatBlock;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.dummys.ISplatcraftForgeBlockDummy;
import net.splatcraft.dummys.ISplatcraftForgeBlockEntityDummy;
import net.splatcraft.items.FilterItem;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.container.InkVatContainer;
import net.splatcraft.tileentities.container.InkVatScreenHandlerContext;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkVatTileEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeInput, ISplatcraftForgeBlockDummy, ISplatcraftForgeBlockEntityDummy
{
	private static final int[] INPUT_SLOTS = new int[]{0, 1, 2, 3};
	private static final int[] OUTPUT_SLOTS = new int[]{4};
	public int pointer = -1;
	// todo: do item handler thingy
    /*Optional<? extends net.minecraftforge.items.IItemHandler>[] handlers =
        net.minecraftforge.items.wrapper.SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);*/
	private NonNullList<ItemStack> inventory = NonNullList.withSize(5, ItemStack.EMPTY);
	private InkColor color = InkColor.INVALID;
	private int recipeEntries = 0;
	public InkVatTileEntity(BlockPos pos, BlockState state)
	{
		super(SplatcraftTileEntities.inkVatTileEntity.get(), pos, state);
	}
	public static void tick(Level world, BlockPos pos, BlockState state, InkVatTileEntity te)
	{
		te.updateRecipeOutput();
		if (!world.isClientSide())
			world.setBlock(pos, state.setValue(InkVatBlock.ACTIVE, te.hasRecipe()), 3);
	}
	@Override
	public int @NotNull [] getSlotsForFace(@NotNull Direction side)
	{
		return side == Direction.UP ? INPUT_SLOTS : OUTPUT_SLOTS;
	}
	@Override
	public boolean canTakeItemThroughFace(int index, @NotNull ItemStack itemStackIn, @Nullable Direction direction)
	{
		return canPlaceItem(index, itemStackIn);
	}
	@Override
	public boolean canPlaceItemThroughFace(int index, @NotNull ItemStack stack, @Nullable Direction direction)
	{
		return index == 4;
	}
	@Override
	public int getContainerSize()
	{
		return inventory.size();
	}
	@Override
	public @NotNull ItemStack getItem(int slot)
	{
		return inventory.get(slot);
	}
	@Override
	public int size()
	{
		return getContainerSize();
	}
	@Override
	public boolean isEmpty()
	{
		return inventory.stream().allMatch(ItemStack::isEmpty);
	}
	public boolean consumeIngredients(int count)
	{
		if (inventory.get(0).getCount() >= count && inventory.get(1).getCount() >= count && inventory.get(2).getCount() >= count)
		{
			removeItem(0, count);
			removeItem(1, count);
			removeItem(2, count);
			return true;
		}
		return false;
	}
	@Override
	public @NotNull ItemStack removeItem(int index, int count)
	{
		if (index == 4 && !consumeIngredients(count))
		{
			return ItemStack.EMPTY;
		}

		ItemStack itemstack = ContainerHelper.removeItem(inventory, index, count);
		if (!itemstack.isEmpty())
			setChanged();

		return itemstack;
	}
	public void updateRecipeOutput()
	{
		if (hasRecipe())
			setItem(4, ColorUtils.withColorLocked(ColorUtils.withInkColor(new ItemStack(SplatcraftItems.inkwell.get(), Math.min(SplatcraftItems.inkwell.get().getDefaultMaxStackSize(),
				Math.min(Math.min(inventory.get(0).getCount(), inventory.get(1).getCount()), inventory.get(2).getCount()))), getColor()), true));
		else
			setItem(4, ItemStack.EMPTY);
	}
	public boolean hasOmniFilter()
	{
		if (inventory.get(3).getItem() instanceof FilterItem filter)
			return filter.isOmni();

		return false;
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
		if (level != null && level.getBlockEntity(getBlockPos()) != this)
			return false;
		return !(player.distanceToSqr(getBlockPos().getCenter()) > 64.0D);
	}
	@Override
	public void clearContent()
	{
		inventory.clear();
	}
	public boolean hasRecipe()
	{
		return !inventory.get(0).isEmpty() && !inventory.get(1).isEmpty() && !inventory.get(2).isEmpty() && getColor().isValid();
	}
	public NonNullList<ItemStack> getInventory()
	{
		return inventory;
	}
	@Override
	public void saveAdditional(CompoundTag nbt, HolderLookup.@NotNull Provider lookup)
	{
		nbt.put("Color", color.getNbt());
		nbt.putInt("Pointer", pointer);
		nbt.putInt("RecipeEntries", recipeEntries);
		ContainerHelper.saveAllItems(nbt, inventory, lookup);
		super.saveAdditional(nbt, lookup);
	}
	@Override
	public @NotNull Component getDefaultName()
	{
		return Component.translatable("container.ink_vat");
	}
	@Override
	protected @NotNull NonNullList<ItemStack> getItems()
	{
		return inventory;
	}
	@Override
	protected void setItems(@NotNull NonNullList<ItemStack> inventory)
	{
		this.inventory = inventory;
	}
	@Override
	protected @NotNull AbstractContainerMenu createMenu(int id, @NotNull Inventory player)
	{
		return new InkVatContainer(id, player, new InkVatScreenHandlerContext(level, getBlockPos()), false);
	}
	//Nbt Read
	@Override
	public void loadAdditional(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider lookup)
	{
		super.loadAdditional(nbt, lookup);
		color = InkColor.getFromNbt(nbt.get("Color"));
		pointer = nbt.getInt("Pointer");
		recipeEntries = nbt.getInt("RecipeEntries");

		clearContent();
		ContainerHelper.loadAllItems(nbt, inventory, lookup);
	}
	@Override
	public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider lookup)
	{
		return new CompoundTag()
		{{
			saveAdditional(this, lookup);
		}};
	}
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket()
	{
		// Will get tag from #toInitialChunkDataNbt
		return ClientboundBlockEntityDataPacket.create(this);
	}
	@Override
	public void phOnDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider wrapperLookup)
	{
		if (level != null)
		{
			BlockState state = level.getBlockState(getBlockPos());
			level.sendBlockUpdated(getBlockPos(), state, state, 2);
			phHandleUpdateTag(pkt.getTag(), wrapperLookup);
		}
	}
	@Override
	public boolean canPlaceItem(int slot, @NotNull ItemStack stack)
	{
		return switch (slot)
		{
			case 0 -> stack.is(Items.INK_SAC);
			case 1 -> stack.is(SplatcraftItems.powerEgg.get());
			case 2 -> stack.is(SplatcraftItems.emptyInkwell.get());
			case 3 -> stack.is(SplatcraftTags.Items.FILTERS);
			default -> false;
		};
	}
	public void onRedstonePulse()
	{
		if (hasRecipe())
		{
			if (level != null)
				level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
			if (pointer != -1 && recipeEntries > 0)
			{
				pointer = (pointer + 1) % recipeEntries;
				setColor(InkVatContainer.sortRecipeList(InkVatContainer.getAvailableRecipes(this)).get(pointer));
			}
		}
	}
	public InkColor getColor()
	{
		return color;
	}
	public void setColor(InkColor color)
	{
		this.color = color;
	}
	public void setRecipeEntries(int v)
	{
		recipeEntries = v;
	}

    /*@Override
    public <T> net.minecraftforge.common.util.@NotNull LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.@NotNull Capability<T> capability, @Nullable Direction facing)
    {
        if (!isRemoved() && facing != null && capability == ForgeCapabilities.ITEM_HANDLER)
        {
            if (facing == Direction.UP)
            {
                return handlers[0].cast();
            }
            else if (facing == Direction.DOWN)
            {
                return handlers[1].cast();
            }
            else
            {
                return handlers[2].cast();
            }
        }
        return super.getCapability(capability, facing);
    }*/
	/**
	 * invalidates a tile entity
	 */
	@Override
	public void setRemoved()
	{
		super.setRemoved();
        /*for (LazyOptional<? extends IItemHandler> handler : handlers)
        {
            handler.invalidate();
        }*/
	}
	public void setColorAndUpdate(InkColor color)
	{
		boolean changeState = Math.min(color.getColor(), 0) != Math.min(getColor().getColor(), 0);
		setColor(color);
		if (level != null)
		{
			if (changeState)
			{
				level.setBlock(getBlockPos(), getBlockState().setValue(InkVatBlock.ACTIVE, hasRecipe()), 2);
			}
			else
			{
				level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
			}
		}
	}
}