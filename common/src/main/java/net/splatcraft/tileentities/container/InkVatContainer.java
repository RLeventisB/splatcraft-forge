package net.splatcraft.tileentities.container;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.splatcraft.blocks.InkVatBlock;
import net.splatcraft.crafting.InkVatColorRecipe;
import net.splatcraft.crafting.InkVatRecipeInput;
import net.splatcraft.crafting.SplatcraftRecipeTypes;
import net.splatcraft.data.InkColorRegistry;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.UpdateBlockColorPacket;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftStats;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.InkVatTileEntity;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class InkVatContainer extends ScreenHandler
{
	private final InkVatScreenHandlerContext context;
	private List<InkColor> recipes = Lists.newArrayList();
	public InkVatContainer(final int windowId, final PlayerInventory inventory, InkVatScreenHandlerContext context, boolean updateSelectedRecipe)
	{
		super(SplatcraftTileEntities.inkVatContainer.get(), windowId);
		this.context = context;
		
		addSlot(new SlotInput(new ItemStack(Items.INK_SAC), inventory, 0, 26, 70));
		addSlot(new SlotInput(new ItemStack(SplatcraftItems.powerEgg.get()), inventory, 1, 46, 70));
		addSlot(new SlotInput(new ItemStack(SplatcraftBlocks.emptyInkwell.get()), inventory, 2, 92, 82));
		addSlot(new SlotFilter(inventory, 3, 36, 89));
		addSlot(new SlotOutput(inventory.player, inventory, 4, 112, 82));
		
		for (int xx = 0; xx < 9; xx++)
		{
			for (int yy = 0; yy < 3; yy++)
			{
				addSlot(new Slot(inventory, xx + yy * 9 + 9, 8 + xx * 18, 126 + yy * 18));
			}
		}
		for (int xx = 0; xx < 9; xx++)
		{
			addSlot(new Slot(inventory, xx, 8 + xx * 18, 184));
		}
		
		if (updateSelectedRecipe)
		{
			updateSelectedRecipe();
		}
	}
	public InkVatContainer(final int windowId, final PlayerInventory inv)
	{
		this(windowId, inv, (InkVatScreenHandlerContext) ScreenHandlerContext.EMPTY, true);
	}

//    private static InkVatTileEntity getBlockEntity(PlayerInventory inventory, RegistryByteBuf buffer)
//    {
//        Objects.requireNonNull(inventory);
//        Objects.requireNonNull(buffer);
//
//        final BlockEntity te = inventory.player.getWorld().getBlockEntity(buffer.readBlockPos());
//
//        if (te instanceof InkVatTileEntity tileEntity)
//        {
//            return tileEntity;
//        }
//        throw new IllegalStateException("TileEntity is not correct " + te);
//    }
	public static List<InkColor> getRecipeList(InkVatTileEntity te)
	{
		return hasIngredients(te) ? getAvailableRecipes(te) : Collections.emptyList();
	}
	public static boolean hasIngredients(InkVatTileEntity te)
	{
		return !te.getStack(0).isEmpty() && !te.getStack(1).isEmpty() && !te.getStack(2).isEmpty();
	}
	public static List<InkColor> sortRecipeList(List<InkColor> list)
	{
		list.sort((o1, o2) ->
		{
			if (o1 != null)
			{
				if (o2 != null)
				{
					return o1.compareTo(o2);
				}
				return -1;
			}
			else if (o2 != null)
			{
				return 1;
			}
			return 0;
		});
		
		return list;
	}
	public static List<InkColor> getAvailableRecipes(InkVatTileEntity te)
	{
		List<InkColor> recipes = Lists.newArrayList();
		if (te.hasOmniFilter())
		{
			recipes = getOmniList();
		}
		else
		{
			InkVatRecipeInput input = new InkVatRecipeInput(te.getInventory());
			for (RecipeEntry<InkVatColorRecipe> recipe : te.getWorld().getRecipeManager().getAllMatches(SplatcraftRecipeTypes.INK_VAT_COLOR_CRAFTING_TYPE, input, te.getWorld()))
			{
				if (recipe.value().matches(input, te.getWorld()))
				{
					recipes.add(recipe.value().getOutputColor());
				}
			}
		}
		
		return recipes;
	}
	public static List<InkColor> getOmniList()
	{
		List<InkColor> list = Lists.newArrayList();
		list.addAll(InkVatColorRecipe.getOmniList());
		
		for (Map.Entry<Identifier, InkColor> color : InkColorRegistry.REGISTRY.entrySet())
		{
			InkColor c = color.getValue();
			if (!list.contains(c))
			{
				list.add(c);
			}
		}
		
		return list;
	}
	@Override
	public boolean onButtonClick(@NotNull PlayerEntity playerIn, int id)
	{
		if (isIndexInBounds(id))
		{
			getBlock().pointer = id;
			updateRecipeResult();
		}
		
		return true;
	}
	private InkVatTileEntity getBlock()
	{
		return (InkVatTileEntity) context.get(World::getBlockEntity).orElse(null);
	}
	public void updateSelectedRecipe()
	{
		InkVatTileEntity block = getBlock();
		InkColor teColor = block.getColor();
		
		updateInkVatColor(block.pointer, block.pointer == -1 ? InkColor.INVALID : teColor);
	}
	public void updateInkVatColor(int pointer, InkColor color)
	{
		InkVatTileEntity block = getBlock();
		block.pointer = pointer;
		
		if (context.get((World t, BlockPos u) -> t.isClient()).get())
		{
			SplatcraftPacketHandler.sendToServer(new UpdateBlockColorPacket(context.pos(), color, pointer));
		}
		else if (block.getCachedState().getBlock() instanceof InkVatBlock inkVatBlock)
		{
			inkVatBlock.setColor(context.world(), context.pos(), color);
		}
	}
	public int getSelectedRecipe()
	{
		return getBlock().pointer;
	}
	public List<InkColor> getRecipeList()
	{
		return hasIngredients(getBlock()) ? recipes : Collections.emptyList();
	}
	public List<InkColor> sortRecipeList()
	{
		return sortRecipeList(getRecipeList());
	}
	private void updateAvailableRecipes()
	{
		InkVatTileEntity block = getBlock();
		block.pointer = -1;
		block.setColorAndUpdate(InkColor.INVALID);
		
		recipes = getAvailableRecipes(block);
		block.setRecipeEntries(recipes.size());
	}
	private void updateRecipeResult()
	{
		InkVatTileEntity te = getBlock();
		
		if (!recipes.isEmpty() && isIndexInBounds(te.pointer))
		{
			te.setColorAndUpdate(recipes.get(te.pointer));
		}
		else
		{
			te.setColorAndUpdate(InkColor.INVALID);
		}
		
		sendContentUpdates();
	}
	private boolean isIndexInBounds(int i)
	{
		return i >= 0 && i < recipes.size();
	}
	@Override
	public boolean canUse(@NotNull PlayerEntity playerIn)
	{
		return canUse(context, playerIn, SplatcraftBlocks.inkVat.get());
	}
	@Override
	public @NotNull ItemStack quickMove(@NotNull PlayerEntity playerIn, int index)
	{
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = slots.get(index);
		
		if (slot.hasStack())
		{
			ItemStack itemstack1 = slot.getStack();
			itemstack = itemstack1.copy();
			
			if (index == 4)
			{
				DefaultedList<ItemStack> inv = getStacks();
				int countA = inv.get(0).getCount();
				int countB = inv.get(1).getCount();
				int countC = inv.get(2).getCount();
				int itemCount = Math.min(Math.max(0, Math.min(countA, Math.min(countB, countC))), new ItemStack(SplatcraftBlocks.inkwell.get()).getMaxCount());
				itemstack1.setCount(itemCount);
				
				if (insertItem(itemstack1, 5, slots.size(), true) && itemCount > 0)
				{
					InkVatTileEntity te = getBlock();
					
					te.removeStack(0, itemCount);
					te.removeStack(1, itemCount);
					te.removeStack(2, itemCount);
					playerIn.increaseStat(SplatcraftStats.INKWELLS_CRAFTED, itemCount);
				}
				return ItemStack.EMPTY;
			}
			else if (index < 4)
			{
				if (!insertItem(itemstack1, 5, slots.size(), true))
				{
					return ItemStack.EMPTY;
				}
			}
			else if (!insertItem(itemstack1, 0, 5, false))
			{
				return ItemStack.EMPTY;
			}
			
			if (itemstack1.isEmpty())
			{
				slot.setStack(ItemStack.EMPTY);
			}
			else
			{
				slot.markDirty();
			}
		}
		
		return itemstack;
	}
	static class SlotInput extends Slot
	{
		final ItemStack validItem;
		public SlotInput(ItemStack validItem, Inventory inventoryIn, int index, int xPosition, int yPosition)
		{
			super(inventoryIn, index, xPosition, yPosition);
			this.validItem = validItem;
		}
		@Override
		public boolean canInsert(@NotNull ItemStack stack)
		{
			if (!validItem.isDamageable())
			{
				return ItemStack.areEqual(validItem, stack);
			}
			else
			{
				return !stack.isEmpty() && validItem.isOf(stack.getItem());
			}
		}
	}
	static class SlotOutput extends Slot
	{
		PlayerEntity player;
		public SlotOutput(PlayerEntity player, Inventory inventoryIn, int index, int xPosition, int yPosition)
		{
			super(inventoryIn, index, xPosition, yPosition);
			this.player = player;
		}
		@Override
		public boolean canInsert(@NotNull ItemStack stack)
		{
			return false;
		}
		@Override
		public @NotNull ItemStack takeStack(int amount)
		{
			player.increaseStat(SplatcraftStats.INKWELLS_CRAFTED, amount);
			return super.takeStack(amount);
		}
	}
	class SlotFilter extends Slot
	{
		public SlotFilter(Inventory inventoryIn, int index, int xPosition, int yPosition)
		{
			super(inventoryIn, index, xPosition, yPosition);
		}
		@Override
		public boolean canInsert(ItemStack stack)
		{
			return stack.isIn(SplatcraftTags.Items.FILTERS);
		}
		@Override
		public void markDirty()
		{
			super.markDirty();
			updateAvailableRecipes();
		}
	}
}
