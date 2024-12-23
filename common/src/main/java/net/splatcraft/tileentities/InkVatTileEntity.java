package net.splatcraft.tileentities;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.splatcraft.blocks.ISplatcraftForgeBlockDummy;
import net.splatcraft.blocks.InkVatBlock;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.items.FilterItem;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.container.InkVatContainer;
import net.splatcraft.tileentities.container.InkVatScreenHandlerContext;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkVatTileEntity extends LockableContainerBlockEntity implements SidedInventory, RecipeInput, ISplatcraftForgeBlockDummy
{
	private static final int[] INPUT_SLOTS = new int[] {0, 1, 2, 3};
	private static final int[] OUTPUT_SLOTS = new int[] {4};
	public int pointer = -1;
	// todo: do item handler thingy
    /*Optional<? extends net.minecraftforge.items.IItemHandler>[] handlers =
        net.minecraftforge.items.wrapper.SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);*/
	private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(5, ItemStack.EMPTY);
	private InkColor color = InkColor.INVALID;
	private int recipeEntries = 0;
	public InkVatTileEntity(BlockPos pos, BlockState state)
	{
		super(SplatcraftTileEntities.inkVatTileEntity.get(), pos, state);
	}
	public static void tick(World world, BlockPos pos, BlockState state, InkVatTileEntity te)
	{
		te.updateRecipeOutput();
		if (!world.isClient())
			world.setBlockState(pos, state.with(InkVatBlock.ACTIVE, te.hasRecipe()), 3);
	}
	@Override
	public int @NotNull [] getAvailableSlots(@NotNull Direction side)
	{
		return side == Direction.UP ? INPUT_SLOTS : OUTPUT_SLOTS;
	}
	@Override
	public boolean canExtract(int index, @NotNull ItemStack itemStackIn, @Nullable Direction direction)
	{
		return isValid(index, itemStackIn);
	}
	@Override
	public boolean canInsert(int index, @NotNull ItemStack stack, @Nullable Direction direction)
	{
		return index == 4;
	}
	@Override
	public int size()
	{
		return inventory.size();
	}
	@Override
	public ItemStack getStackInSlot(int slot)
	{
		return getStack(slot);
	}
	@Override
	public int getSize()
	{
		return size();
	}
	@Override
	public boolean isEmpty()
	{
		return inventory.stream().allMatch(ItemStack::isEmpty);
	}
	@Override
	public @NotNull ItemStack getStack(int index)
	{
		return inventory.get(index);
	}
	public boolean consumeIngredients(int count)
	{
		if (inventory.get(0).getCount() >= count && inventory.get(1).getCount() >= count && inventory.get(2).getCount() >= count)
		{
			removeStack(0, count);
			removeStack(1, count);
			removeStack(2, count);
			return true;
		}
		return false;
	}
	@Override
	public @NotNull ItemStack removeStack(int index, int count)
	{
		if (index == 4 && !consumeIngredients(count))
		{
			return ItemStack.EMPTY;
		}
		
		ItemStack itemstack = Inventories.splitStack(inventory, index, count);
		if (!itemstack.isEmpty())
			markDirty();
		
		return itemstack;
	}
	public void updateRecipeOutput()
	{
		if (hasRecipe())
			setStack(4, ColorUtils.withColorLocked(ColorUtils.withInkColor(new ItemStack(SplatcraftItems.inkwell.get(), Math.min(SplatcraftItems.inkwell.get().getMaxCount(),
				Math.min(Math.min(inventory.get(0).getCount(), inventory.get(1).getCount()), inventory.get(2).getCount()))), getColor()), true));
		else
			setStack(4, ItemStack.EMPTY);
	}
	public boolean hasOmniFilter()
	{
		if (inventory.get(3).getItem() instanceof FilterItem filter)
			return filter.isOmni();
		
		return false;
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
		if (world != null && world.getBlockEntity(getPos()) != this)
			return false;
		return !(player.squaredDistanceTo(getPos().toCenterPos()) > 64.0D);
	}
	@Override
	public void clear()
	{
		inventory.clear();
	}
	public boolean hasRecipe()
	{
		return !inventory.get(0).isEmpty() && !inventory.get(1).isEmpty() && !inventory.get(2).isEmpty() && getColor().isValid();
	}
	public DefaultedList<ItemStack> getInventory()
	{
		return inventory;
	}
	@Override
	public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup)
	{
		nbt.put("Color", color.getNbt());
		nbt.putInt("Pointer", pointer);
		nbt.putInt("RecipeEntries", recipeEntries);
		Inventories.writeNbt(nbt, inventory, lookup);
		super.writeNbt(nbt, lookup);
	}
	@Override
	public @NotNull Text getContainerName()
	{
		return Text.translatable("container.ink_vat");
	}
	@Override
	protected DefaultedList<ItemStack> getHeldStacks()
	{
		return inventory;
	}
	@Override
	protected void setHeldStacks(DefaultedList<ItemStack> inventory)
	{
		this.inventory = inventory;
	}
	@Override
	protected @NotNull ScreenHandler createScreenHandler(int id, @NotNull PlayerInventory player)
	{
		return new InkVatContainer(id, player, new InkVatScreenHandlerContext(world, getPos()), false);
	}
	//Nbt Read
	@Override
	public void readNbt(@NotNull NbtCompound nbt, RegistryWrapper.WrapperLookup lookup)
	{
		super.readNbt(nbt, lookup);
		color = InkColor.getFromNbt(nbt.get("Color"));
		pointer = nbt.getInt("Pointer");
		recipeEntries = nbt.getInt("RecipeEntries");
		
		clear();
		Inventories.readNbt(nbt, inventory, lookup);
	}
	@Override
	public @NotNull NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup lookup)
	{
		return new NbtCompound()
		{{
			writeNbt(this, lookup);
		}};
	}
	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket()
	{
		// Will get tag from #toInitialChunkDataNbt
		return BlockEntityUpdateS2CPacket.create(this);
	}
	@Override
	public void onDataPacket(ClientConnection net, BlockEntityUpdateS2CPacket pkt, RegistryWrapper.WrapperLookup wrapperLookup)
	{
		if (world != null)
		{
			BlockState state = world.getBlockState(getPos());
			world.updateListeners(getPos(), state, state, 2);
			handleUpdateTag(pkt.getNbt(), wrapperLookup);
		}
	}
	@Override
	public boolean isValid(int slot, ItemStack stack)
	{
		return switch (slot)
		{
			case 0 -> stack.isOf(Items.INK_SAC);
			case 1 -> stack.isOf(SplatcraftItems.powerEgg.get());
			case 2 -> stack.isOf(SplatcraftItems.emptyInkwell.get());
			case 3 -> stack.isIn(SplatcraftTags.Items.FILTERS);
			default -> false;
		};
	}
	public void onRedstonePulse()
	{
		if (hasRecipe())
		{
			if (world != null)
				world.updateListeners(getPos(), getCachedState(), getCachedState(), 2);
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
	public void markRemoved()
	{
		super.markRemoved();
        /*for (LazyOptional<? extends IItemHandler> handler : handlers)
        {
            handler.invalidate();
        }*/
	}
	public void setColorAndUpdate(InkColor color)
	{
		boolean changeState = Math.min(color.getColor(), 0) != Math.min(getColor().getColor(), 0);
		setColor(color);
		if (world != null)
		{
			if (changeState)
			{
				world.setBlockState(getPos(), getCachedState().with(InkVatBlock.ACTIVE, hasRecipe()), 2);
			}
			else
			{
				world.updateListeners(getPos(), getCachedState(), getCachedState(), 2);
			}
		}
	}
}