package net.splatcraft.tileentities.container;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
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

public class InkVatContainer extends AbstractContainerMenu
{
	private final InkVatScreenHandlerContext context;
	private List<InkColor> recipes = Lists.newArrayList();
	public InkVatContainer(final int windowId, final Inventory inventory, InkVatScreenHandlerContext context, boolean updateSelectedRecipe)
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
	public InkVatContainer(final int windowId, final Inventory inv)
	{
		this(windowId, inv, (InkVatScreenHandlerContext) ContainerLevelAccess.NULL, true);
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
		return !te.getItem(0).isEmpty() && !te.getItem(1).isEmpty() && !te.getItem(2).isEmpty();
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
			for (RecipeHolder<InkVatColorRecipe> recipe : te.getLevel().getRecipeManager().getRecipesFor(SplatcraftRecipeTypes.INK_VAT_COLOR_CRAFTING_TYPE, input, te.getLevel()))
			{
				if (recipe.value().matches(input, te.getLevel()))
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
		
		for (Map.Entry<ResourceLocation, InkColor> color : InkColorRegistry.REGISTRY.entrySet())
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
	public boolean clickMenuButton(@NotNull Player playerIn, int id)
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
		return (InkVatTileEntity) context.evaluate(Level::getBlockEntity).orElse(null);
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
		
		if (context.evaluate((Level t, BlockPos u) -> t.isClientSide()).get())
		{
			SplatcraftPacketHandler.sendToServer(new UpdateBlockColorPacket(context.pos(), color, pointer));
		}
		else if (block.getBlockState().getBlock() instanceof InkVatBlock inkVatBlock)
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
		
		broadcastChanges();
	}
	private boolean isIndexInBounds(int i)
	{
		return i >= 0 && i < recipes.size();
	}
	@Override
	public boolean stillValid(@NotNull Player playerIn)
	{
		return stillValid(context, playerIn, SplatcraftBlocks.inkVat.get());
	}
	@Override
	public @NotNull ItemStack quickMoveStack(@NotNull Player playerIn, int index)
	{
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = slots.get(index);
		
		if (slot.hasItem())
		{
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.copy();
			
			if (index == 4)
			{
				NonNullList<ItemStack> inv = getItems();
				int countA = inv.get(0).getCount();
				int countB = inv.get(1).getCount();
				int countC = inv.get(2).getCount();
				int itemCount = Math.min(Math.max(0, Math.min(countA, Math.min(countB, countC))), new ItemStack(SplatcraftBlocks.inkwell.get()).getMaxStackSize());
				itemstack1.setCount(itemCount);
				
				if (moveItemStackTo(itemstack1, 5, slots.size(), true) && itemCount > 0)
				{
					InkVatTileEntity te = getBlock();
					
					te.removeItem(0, itemCount);
					te.removeItem(1, itemCount);
					te.removeItem(2, itemCount);
					playerIn.awardStat(SplatcraftStats.INKWELLS_CRAFTED, itemCount);
				}
				return ItemStack.EMPTY;
			}
			else if (index < 4)
			{
				if (!moveItemStackTo(itemstack1, 5, slots.size(), true))
				{
					return ItemStack.EMPTY;
				}
			}
			else if (!moveItemStackTo(itemstack1, 0, 5, false))
			{
				return ItemStack.EMPTY;
			}
			
			if (itemstack1.isEmpty())
			{
				slot.setByPlayer(ItemStack.EMPTY);
			}
			else
			{
				slot.setChanged();
			}
		}
		
		return itemstack;
	}
	static class SlotInput extends Slot
	{
		final ItemStack validItem;
		public SlotInput(ItemStack validItem, Container inventoryIn, int index, int xPosition, int yPosition)
		{
			super(inventoryIn, index, xPosition, yPosition);
			this.validItem = validItem;
		}
		@Override
		public boolean mayPlace(@NotNull ItemStack stack)
		{
			if (!validItem.isDamageableItem())
			{
				return ItemStack.matches(validItem, stack);
			}
			else
			{
				return !stack.isEmpty() && validItem.is(stack.getItem());
			}
		}
	}
	static class SlotOutput extends Slot
	{
		Player player;
		public SlotOutput(Player player, Container inventoryIn, int index, int xPosition, int yPosition)
		{
			super(inventoryIn, index, xPosition, yPosition);
			this.player = player;
		}
		@Override
		public boolean mayPlace(@NotNull ItemStack stack)
		{
			return false;
		}
		@Override
		public @NotNull ItemStack remove(int amount)
		{
			player.awardStat(SplatcraftStats.INKWELLS_CRAFTED, amount);
			return super.remove(amount);
		}
	}
	class SlotFilter extends Slot
	{
		public SlotFilter(Container inventoryIn, int index, int xPosition, int yPosition)
		{
			super(inventoryIn, index, xPosition, yPosition);
		}
		@Override
		public boolean mayPlace(ItemStack stack)
		{
			return stack.is(SplatcraftTags.Items.FILTERS);
		}
		@Override
		public void setChanged()
		{
			super.setChanged();
			updateAvailableRecipes();
		}
	}
}
