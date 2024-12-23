package net.splatcraft.crafting;

import net.minecraft.block.Block;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import net.splatcraft.blocks.InkwellBlock;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

public class SingleUseSubRecipe extends SpecialCraftingRecipe
{
	public SingleUseSubRecipe(CraftingRecipeCategory category)
	{
		super(category);
	}
	@Override
	public boolean matches(CraftingRecipeInput inv, @NotNull World world)
	{
		int sub = 0;
		int inkwell = 0;
		int sardinium = 0;
		
		for (int k = 0; k < inv.getSize(); ++k)
		{
			ItemStack itemstack = inv.getStackInSlot(k);
			if (!itemstack.isEmpty())
			{
				if (Block.getBlockFromItem(itemstack.getItem()) instanceof InkwellBlock)
					++inkwell;
				else if (itemstack.getItem().equals(SplatcraftItems.sardinium.get()))
					++sardinium;
				else
				{
					if (!itemstack.isIn(SplatcraftTags.Items.SUB_WEAPONS))
						return false;
					++sub;
				}
				
				if (inkwell > 1 || sub > 1 || sardinium > 1)
					return false;
			}
		}
		
		return sub == 1 && inkwell == 1 && sardinium == 1;
	}
	@Override
	public @NotNull ItemStack craft(CraftingRecipeInput inv, @NotNull RegistryWrapper.WrapperLookup access)
	{
		
		ItemStack itemstack = ItemStack.EMPTY;
		InkColor color = null;
		
		for (int i = 0; i < inv.getSize(); ++i)
		{
			ItemStack itemstack1 = inv.getStackInSlot(i);
			if (!itemstack1.isEmpty())
			{
				if (itemstack1.isIn(SplatcraftTags.Items.SUB_WEAPONS))
					itemstack = itemstack1;
				else if (Block.getBlockFromItem(itemstack1.getItem()) instanceof InkwellBlock)
					color = ColorUtils.getInkColor(itemstack1);
			}
		}
		
		ItemStack result = ColorUtils.withInkColor(itemstack.copy(), color);
		ColorUtils.withColorLocked(result, color != null);
		result.applyChanges(ComponentChanges.builder().add(SplatcraftComponents.SINGLE_USE, true).build());
		
		return result;
	}
	@Override
	public @NotNull DefaultedList<ItemStack> getRemainder(CraftingRecipeInput inv)
	{
		DefaultedList<ItemStack> restult = DefaultedList.ofSize(inv.getSize(), ItemStack.EMPTY);
		
		for (int i = 0; i < inv.getSize(); ++i)
		{
			ItemStack stack = inv.getStackInSlot(i);
			if (Block.getBlockFromItem(stack.getItem()) instanceof InkwellBlock)
				restult.set(i, new ItemStack(SplatcraftItems.emptyInkwell.get()));
			else if (stack.isIn(SplatcraftTags.Items.SUB_WEAPONS))
				restult.set(i, stack.copy());
		}
		
		return restult;
	}
	@Override
	public boolean fits(int width, int height)
	{
		return width * height >= 3;
	}
	@Override
	public @NotNull RecipeSerializer<?> getSerializer()
	{
		return SplatcraftRecipeTypes.SINGLE_USE_SUB;
	}
}
