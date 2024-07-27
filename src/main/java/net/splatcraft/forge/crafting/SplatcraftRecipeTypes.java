package net.splatcraft.forge.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.registries.ForgeRegistries;
import net.splatcraft.forge.Splatcraft;

public class SplatcraftRecipeTypes
{
	public static final RecipeSerializer<InkVatColorRecipe> INK_VAT_COLOR_CRAFTING = new InkVatColorRecipe.InkVatColorSerializer();
	public static final RecipeSerializer<WeaponWorkbenchTab> WEAPON_STATION_TAB = new WeaponWorkbenchTab.WeaponWorkbenchTabSerializer();
	public static final RecipeSerializer<WeaponWorkbenchRecipe> WEAPON_STATION = new WeaponWorkbenchRecipe.Serializer();
	public static final RecipeSerializer<SingleUseSubRecipe> SINGLE_USE_SUB = new SimpleCraftingRecipeSerializer<>(SingleUseSubRecipe::new);
	public static final RecipeSerializer<ShapedRecipe> COLORED_SHAPED_CRAFTING = new ColoredShapedRecipe.Serializer("colored_crafting_shaped");
	public static RecipeType<AbstractWeaponWorkbenchRecipe> WEAPON_STATION_TYPE;
	public static RecipeType<WeaponWorkbenchTab> WEAPON_STATION_TAB_TYPE;
	public static RecipeType<InkVatColorRecipe> INK_VAT_COLOR_CRAFTING_TYPE;
	public static boolean getItem(Player player, Ingredient ingredient, int count, boolean takeItems)
	{
		for (int i = 0; i < player.getInventory().getContainerSize(); ++i)
		{
			ItemStack invStack = player.getInventory().getItem(i);
			if (!takeItems)
			{
				invStack = invStack.copy();
			}
			
			if (ingredient.test(invStack))
			{
				if (count > invStack.getCount())
				{
					count -= invStack.getCount();
					invStack.setCount(0);
				}
				else
				{
					invStack.setCount(invStack.getCount() - count);
					return true;
				}
			}
		}
		return false;
	}
	public static void register()
	{
		INK_VAT_COLOR_CRAFTING_TYPE = RecipeType.simple(new ResourceLocation(Splatcraft.MODID, "ink_vat_color"));
		WEAPON_STATION_TAB_TYPE = RecipeType.simple(new ResourceLocation(Splatcraft.MODID, "weapon_workbench_tab"));
		WEAPON_STATION_TYPE = RecipeType.simple(new ResourceLocation(Splatcraft.MODID, "weapon_workbench"));
		ForgeRegistries.RECIPE_TYPES.register(new ResourceLocation(Splatcraft.MODID, "ink_vat_color"), INK_VAT_COLOR_CRAFTING_TYPE);
		ForgeRegistries.RECIPE_TYPES.register(new ResourceLocation(Splatcraft.MODID, "weapon_workbench_tab"), WEAPON_STATION_TAB_TYPE);
		ForgeRegistries.RECIPE_TYPES.register(new ResourceLocation(Splatcraft.MODID, "weapon_workbench"), WEAPON_STATION_TYPE);
		
		ForgeRegistries.RECIPE_SERIALIZERS.register(new ResourceLocation(Splatcraft.MODID, "ink_vat_color"), INK_VAT_COLOR_CRAFTING);
		ForgeRegistries.RECIPE_SERIALIZERS.register(new ResourceLocation(Splatcraft.MODID, "weapon_workbench_tab"), WEAPON_STATION_TAB);
		ForgeRegistries.RECIPE_SERIALIZERS.register(new ResourceLocation(Splatcraft.MODID, "weapon_workbench"), WEAPON_STATION);
		ForgeRegistries.RECIPE_SERIALIZERS.register(new ResourceLocation(Splatcraft.MODID, "colored_crafting_shaped"), COLORED_SHAPED_CRAFTING);
		ForgeRegistries.RECIPE_SERIALIZERS.register(new ResourceLocation(Splatcraft.MODID, "single_use_sub"), SINGLE_USE_SUB);
	}
}
