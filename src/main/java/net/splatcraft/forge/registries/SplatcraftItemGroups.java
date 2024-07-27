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

import static net.splatcraft.forge.registries.SplatcraftItems.sardiniumBlock;
import static net.splatcraft.forge.registries.SplatcraftItems.splattershot;

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
		() -> CreativeModeTab.builder().icon(() -> new ItemStack(splattershot.get()))
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
			.withSearchBar()
			.build());
	public static List<ItemLike> general = new ArrayList<>(), weapons = new ArrayList<>(), colors = new ArrayList<>();
	//	public static void register()
//	{
//		createTab("splatcraft_general", () -> general, new ItemStack(sardiniumBlock.get()), v -> v);
//		createTab("splatcraft_weapons", () -> weapons, ColorUtils.setInkColor(new ItemStack(splattershot.get()), ColorUtils.ORANGE), v -> v);
//		createTab("splatcraft_colors", () -> colors, ColorUtils.setInkColor(new ItemStack(inkwell.get()), ColorUtils.ORANGE), CreativeModeTab.Builder::withSearchBar);
//
//		for (Item item : colorTabItems)
//		{
//			for (InkColor color : SplatcraftInkColors.REGISTRY.get().getValues().stream().sorted().toList())
//				colors.add(ColorUtils.setColorLocked(ColorUtils.setInkColor(new ItemStack(item), color.getColor()), true));
//			if (!(item instanceof ColoredBlockItem coloredBlockItem) || coloredBlockItem.matchesColor())
//				colors.add(ColorUtils.setInverted(new ItemStack(item), true));
//		}
//	}
	public static void addGeneralItem(Item item)
	{
		general.add(item);
	}
	public static void addWeaponItem(Item item)
	{
		weapons.add(item);
	}
	//	private static void createTab(String name, Supplier<List<ItemLike>> itemList, ItemStack icon, Function<CreativeModeTab.Builder, CreativeModeTab.Builder> extraModifications)
//	{
//		CreativeModeTab.Builder builder = CreativeModeTab.builder().withTabsBefore(CreativeModeTabs.HOTBAR)
//			.icon(() -> icon)
//			.title(Component.translatable("itemGroup." + name))
//			.displayItems((pParameters, pOutput) ->
//			{
//				for (ItemLike item : itemList.get())
//				{
//					pOutput.accept(item);
//				}
//			});
//		builder = extraModifications.apply(builder);
//		Registry.register(ForgeRegistries.CREATIVE_MODE_TAB, ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation(name)),
//			builder.build()
//		);
//	}
	public static final ArrayList<Item> colorTabItems = new ArrayList<>();
}
