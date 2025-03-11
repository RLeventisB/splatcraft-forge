package net.splatcraft.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.splatcraft.blocks.InkwellBlock;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

public class SingleUseSubRecipe extends CustomRecipe
{
	public SingleUseSubRecipe(CraftingBookCategory category)
	{
		super(category);
	}
	@Override
	public boolean matches(CraftingInput inv, @NotNull Level world)
	{
		int sub = 0;
		int inkwell = 0;
		int sardinium = 0;
		
		for (int k = 0; k < inv.size(); ++k)
		{
			ItemStack itemstack = inv.getItem(k);
			if (!itemstack.isEmpty())
			{
				if (Block.byItem(itemstack.getItem()) instanceof InkwellBlock)
					++inkwell;
				else if (itemstack.getItem().equals(SplatcraftItems.sardinium.value()))
					++sardinium;
				else
				{
					if (!itemstack.is(SplatcraftTags.Items.SUB_WEAPONS))
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
	public @NotNull ItemStack assemble(CraftingInput inv, @NotNull HolderLookup.Provider access)
	{
		ItemStack itemstack = ItemStack.EMPTY;
		InkColor color = null;
		
		for (int i = 0; i < inv.size(); ++i)
		{
			ItemStack itemstack1 = inv.getItem(i);
			if (!itemstack1.isEmpty())
			{
				if (itemstack1.is(SplatcraftTags.Items.SUB_WEAPONS))
					itemstack = itemstack1;
				else if (Block.byItem(itemstack1.getItem()) instanceof InkwellBlock)
					color = ColorUtils.getInkColor(itemstack1);
			}
		}
		
		ItemStack result = ColorUtils.withInkColor(itemstack.copy(), color);
		ColorUtils.withColorLocked(result, color != null);
		result.applyComponentsAndValidate(DataComponentPatch.builder().set(SplatcraftComponents.SINGLE_USE, true).build());
		
		return result;
	}
	@Override
	public @NotNull NonNullList<ItemStack> getRemainingItems(CraftingInput inv)
	{
		NonNullList<ItemStack> restult = NonNullList.withSize(inv.size(), ItemStack.EMPTY);
		
		for (int i = 0; i < inv.size(); ++i)
		{
			ItemStack stack = inv.getItem(i);
			if (Block.byItem(stack.getItem()) instanceof InkwellBlock)
				restult.set(i, new ItemStack(SplatcraftItems.emptyInkwell.value()));
			else if (stack.is(SplatcraftTags.Items.SUB_WEAPONS))
				restult.set(i, stack.copy());
		}
		
		return restult;
	}
	@Override
	public boolean canCraftInDimensions(int width, int height)
	{
		return width * height >= 3;
	}
	@Override
	public @NotNull RecipeSerializer<?> getSerializer()
	{
		return SplatcraftRecipeTypes.SINGLE_USE_SUB;
	}
}
