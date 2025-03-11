package net.splatcraft.registries;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.InkColorGroups;
import net.splatcraft.items.ColoredBlockItem;
import net.splatcraft.items.InkTankItem;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.subs.SubWeaponItem;
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.platform.Services;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;

import java.util.ArrayList;
import java.util.List;

import static net.splatcraft.registries.SplatcraftItems.*;

public class SplatcraftItemGroups
{
	public static final ArrayList<Item> colorTabItems = new ArrayList<>();
	protected static final DeferredRegister<CreativeModeTab> REGISTRY = Splatcraft.deferredRegistryOf(BuiltInRegistries.CREATIVE_MODE_TAB);
	public static final RegistrySupplier<CreativeModeTab> GROUP_GENERAL = REGISTRY.register("splatcraft_general", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
		.icon(() -> sardiniumBlock.value().getDefaultInstance())
		.title(Component.translatable("itemGroup.splatcraft_general"))
		.displayItems((parameters, output) ->
		{
			//Materials
			output.accept(sardinium.value());
			output.accept(sardiniumBlock.value());
			output.accept(rawSardinium.value());
			output.accept(rawSardiniumBlock.value());
			output.accept(sardiniumOre.value());
			output.accept(powerEgg.value());
			output.accept(powerEggCan.value());
			output.accept(powerEggBlock.value());
			output.accept(emptyInkwell.value());
			output.accept(ammoKnightsScrap.value());
			output.accept(blueprint.value());
			output.accept(kensaPin.value());
			
			//Remotes
			output.accept(stagePad.value());
			output.accept(turfScanner.value());
			output.accept(inkDisruptor.value());
			output.accept(colorChanger.value());
			output.accept(remotePedestal.value());
			
			//Gear
			output.accept(superJumpLure.value());
			output.accept(specialProvider.value());
			output.accept(splatfestBand.value());
			output.accept(clearBand.value());
			output.accept(waxApplicator.value());
			
			//Filters
			output.accept(emptyFilter.value());
			output.accept(pastelFilter.value());
			output.accept(organicFilter.value());
			output.accept(neonFilter.value());
			output.accept(overgrownFilter.value());
			output.accept(midnightFilter.value());
			output.accept(enchantedFilter.value());
			output.accept(creativeFilter.value());
			
			//Crafting Stations
			output.accept(inkVat.value());
			output.accept(weaponWorkbench.value());
			
			//Colored Items
			output.acceptAll(ColorUtils.getColorVariantsForItem(inkwell.value(), true, true, true));
			output.acceptAll(ColorUtils.getColorVariantsForItem(spawnPad.value(), true, true, false));
			output.acceptAll(ColorUtils.getColorVariantsForItem(squidBumper.value(), true, true, false));
			output.acceptAll(ColorUtils.getColorVariantsForItem(inkedWool.value(), true, true, false));
			output.acceptAll(ColorUtils.getColorVariantsForItem(inkedCarpet.value(), true, true, false));
			output.acceptAll(ColorUtils.getColorVariantsForItem(inkedGlass.value(), true, true, false));
			output.acceptAll(ColorUtils.getColorVariantsForItem(inkedGlassPane.value(), true, true, false));
			
			//Decor Blocks
			output.accept(canvas.value());
			output.accept(coralite.value());
			output.accept(coraliteSlab.value());
			output.accept(coraliteStairs.value());
			output.accept(grate.value());
			output.accept(grateRamp.value());
			output.accept(barrierBar.value());
			output.accept(platedBarrierBar.value());
			output.accept(cautionBarrierBar.value());
			output.accept(tarp.value());
			output.accept(glassCover.value());
			output.accept(crate.value());
			output.accept(sunkenCrate.value());
			output.accept(splatSwitch.value());
			
			//Stage Barriers
			output.accept(stageBarrier.value());
			output.accept(stageVoid.value());
			output.acceptAll(ColorUtils.getColorVariantsForItem(allowedColorBarrier.value(), true, true, false));
			output.acceptAll(ColorUtils.getColorVariantsForItem(deniedColorBarrier.value(), true, true, false));
		}).build());
	public static final RegistrySupplier<CreativeModeTab> GROUP_WEAPONS = REGISTRY.register("splatcraft_weapons", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
		.icon(() -> ColorUtils.withInkColor(splattershot.value().getDefaultInstance(), ColorUtils.getOrange()))
		.title(Component.translatable("itemGroup.splatcraft_weapons"))
		.displayItems((parameters, output) ->
		{
			List<WeaponBaseItem<?>> visibleWeapons = weapons.stream().filter(weapon -> !weapon.isSecret).toList();
			output.acceptAll(visibleWeapons.stream().filter(weapon -> !(weapon instanceof SubWeaponItem)).map(Item::getDefaultInstance).toList());
			List<WeaponBaseItem<?>> subWeapons = visibleWeapons.stream().filter(weapon -> weapon instanceof SubWeaponItem).toList();
			output.acceptAll(subWeapons.stream().map(Item::getDefaultInstance).toList());
			output.acceptAll(subWeapons.stream().map(weaponBaseItem ->
			{
				ItemStack stack = weaponBaseItem.getDefaultInstance();
				stack.set(SplatcraftComponents.SINGLE_USE, true);
				return stack;
			}).toList());
			output.acceptAll(InkTankItem.inkTanks.stream().map(Item::getDefaultInstance).toList());
			
			output.accept(inkClothHelmet.value());
			output.accept(inkClothChestplate.value());
			output.accept(inkClothLeggings.value());
			output.accept(inkClothBoots.value());
		}).build());
	public static final RegistrySupplier<CreativeModeTab> GROUP_COLORS = REGISTRY.register("splatcraft_colors", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
		.icon(() -> ColorUtils.withInkColor(inkwell.value().getDefaultInstance(), ColorUtils.getOrange()))
		.title(Component.translatable("itemGroup.splatcraft_colors"))
		.hideTitle()
		.displayItems((parameters, output) ->
		{
			for (InkColor color : InkColorGroups.CREATIVE_TAB_COLORS.getAll())
			{
				for (Item item : colorTabItems)
					output.accept(ColorUtils.withColorLocked(ColorUtils.withInkColor(new ItemStack(item), color), true));
			}
			for (Item item : colorTabItems)
			{
				if (!(item instanceof ColoredBlockItem coloredBlockItem) || coloredBlockItem.matchesColor())
					output.accept(ColorUtils.withInvertedColor(new ItemStack(item), true));
			}
		})
		.build());
	public static void addSplatcraftItemsToVanillaGroups()
	{
		Services.PLATFORM.addItemToVanillaCreativeTab(CreativeModeTabs.REDSTONE_BLOCKS, splatSwitch);
		Services.PLATFORM.addItemToVanillaCreativeTab(CreativeModeTabs.REDSTONE_BLOCKS, remotePedestal);
	}
}
