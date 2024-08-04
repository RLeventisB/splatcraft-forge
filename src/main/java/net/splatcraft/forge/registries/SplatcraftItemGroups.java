package net.splatcraft.forge.registries;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.items.ColoredBlockItem;
import net.splatcraft.forge.util.ColorUtils;
import net.splatcraft.forge.util.InkColor;

import java.util.ArrayList;
import java.util.List;

import static net.splatcraft.forge.registries.SplatcraftItems.*;

public class SplatcraftItemGroups
{
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Splatcraft.MODID);
	public static final RegistryObject<CreativeModeTab> GENERAL_TAB = CREATIVE_MODE_TABS.register("splatcraft_general",
		() -> CreativeModeTab.builder().icon(() -> new ItemStack(sardiniumBlock.get()))
			.title(Component.translatable("itemGroup.splatcraft_general"))
			.displayItems((pParameters, pOutput) ->
			{
				for (ItemLike item : SplatcraftItemGroups.general)
				{
					pOutput.accept(item);
				}
			})
			.build());
	public static final RegistryObject<CreativeModeTab> WEAPONS_TAB = CREATIVE_MODE_TABS.register("splatcraft_weapons",
		() -> CreativeModeTab.builder().icon(() -> new ItemStack(splattershot.get()))
			.title(Component.translatable("itemGroup.splatcraft_weapons"))
			.displayItems((pParameters, pOutput) ->
			{
				for (ItemLike item : SplatcraftItemGroups.weapons)
				{
					pOutput.accept(item);
				}
			})
			.build());
	public static final RegistryObject<CreativeModeTab> COLORS_TAB = CREATIVE_MODE_TABS.register("splatcraft_colors",
		() -> CreativeModeTab.builder().icon(() -> new ItemStack(inkwell.get()))
			.title(Component.translatable("itemGroup.splatcraft_colors"))
			.displayItems((pParameters, pOutput) ->
			{
				for (ItemLike item : SplatcraftItemGroups.colors)
				{
					for (InkColor color : SplatcraftInkColors.REGISTRY.get().getValues().stream().sorted().toList())
						pOutput.accept(ColorUtils.setColorLocked(ColorUtils.setInkColor(new ItemStack(item), color.getColor()), true));
					if (!(item instanceof ColoredBlockItem coloredBlockItem) || coloredBlockItem.matchesColor())
						pOutput.accept(ColorUtils.setInverted(new ItemStack(item), true));
				}
			})
			.withSearchBar(90)
			.build());
	public static List<ItemLike> general = new ArrayList<>(), weapons = new ArrayList<>(), colors = new ArrayList<>();
	public static void addGeneralItem(Item item)
	{
		general.add(item);
	}
	public static void addWeaponItem(Item item)
	{
		weapons.add(item);
	}
}
