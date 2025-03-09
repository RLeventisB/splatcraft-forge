package net.splatcraft.registries;

import dev.architectury.registry.CreativeTabRegistry;
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
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;

import java.util.ArrayList;
import java.util.List;

import static net.splatcraft.registries.SplatcraftItems.*;

public class SplatcraftItemGroups
{
	public static final ArrayList<Item> colorTabItems = new ArrayList<>();
	protected static final DeferredRegister<CreativeModeTab> REGISTRY = Splatcraft.deferredRegistryOf(BuiltInRegistries.CREATIVE_MODE_TAB);
	public static final RegistrySupplier<CreativeModeTab> GROUP_GENERAL = REGISTRY.<CreativeModeTab>register(Splatcraft.identifierOf("splatcraft_general"), () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
		.icon(() -> sardiniumBlock.get().getDefaultInstance())
		.title(Component.translatable("itemGroup.splatcraft_general"))
		.displayItems((parameters, output) ->
		{
			//Materials
			output.accept(sardinium.get());
			output.accept(sardiniumBlock.get());
			output.accept(rawSardinium.get());
			output.accept(rawSardiniumBlock.get());
			output.accept(sardiniumOre.get());
			output.accept(powerEgg.get());
			output.accept(powerEggCan.get());
			output.accept(powerEggBlock.get());
			output.accept(emptyInkwell.get());
			output.accept(ammoKnightsScrap.get());
			output.accept(blueprint.get());
			output.accept(kensaPin.get());
			
			//Remotes
			output.accept(stagePad.get());
			output.accept(turfScanner.get());
			output.accept(inkDisruptor.get());
			output.accept(colorChanger.get());
			output.accept(remotePedestal.get());
			
			//Gear
			output.accept(superJumpLure.get());
			output.accept(specialProvider.get());
			output.accept(splatfestBand.get());
			output.accept(clearBand.get());
			output.accept(waxApplicator.get());
			
			//Filters
			output.accept(emptyFilter.get());
			output.accept(pastelFilter.get());
			output.accept(organicFilter.get());
			output.accept(neonFilter.get());
			output.accept(overgrownFilter.get());
			output.accept(midnightFilter.get());
			output.accept(enchantedFilter.get());
			output.accept(creativeFilter.get());
			
			//Crafting Stations
			output.accept(inkVat.get());
			output.accept(weaponWorkbench.get());
			
			//Colored Items
			output.acceptAll(ColorUtils.getColorVariantsForItem(inkwell.get(), true, true, true));
			output.acceptAll(ColorUtils.getColorVariantsForItem(spawnPad.get(), true, true, false));
			output.acceptAll(ColorUtils.getColorVariantsForItem(squidBumper.get(), true, true, false));
			output.acceptAll(ColorUtils.getColorVariantsForItem(inkedWool.get(), true, true, false));
			output.acceptAll(ColorUtils.getColorVariantsForItem(inkedCarpet.get(), true, true, false));
			output.acceptAll(ColorUtils.getColorVariantsForItem(inkedGlass.get(), true, true, false));
			output.acceptAll(ColorUtils.getColorVariantsForItem(inkedGlassPane.get(), true, true, false));
			
			//Decor Blocks
			output.accept(canvas.get());
			output.accept(coralite.get());
			output.accept(coraliteSlab.get());
			output.accept(coraliteStairs.get());
			output.accept(grate.get());
			output.accept(grateRamp.get());
			output.accept(barrierBar.get());
			output.accept(platedBarrierBar.get());
			output.accept(cautionBarrierBar.get());
			output.accept(tarp.get());
			output.accept(glassCover.get());
			output.accept(crate.get());
			output.accept(sunkenCrate.get());
			output.accept(splatSwitch.get());
			
			//Stage Barriers
			output.accept(stageBarrier.get());
			output.accept(stageVoid.get());
			output.acceptAll(ColorUtils.getColorVariantsForItem(allowedColorBarrier.get(), true, true, false));
			output.acceptAll(ColorUtils.getColorVariantsForItem(deniedColorBarrier.get(), true, true, false));
		}).build());
	public static final RegistrySupplier<CreativeModeTab> GROUP_WEAPONS = REGISTRY.register("splatcraft_weapons", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
		.icon(() -> ColorUtils.withInkColor(splattershot.get().getDefaultInstance(), ColorUtils.getOrange()))
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
			
			output.accept(inkClothHelmet.get());
			output.accept(inkClothChestplate.get());
			output.accept(inkClothLeggings.get());
			output.accept(inkClothBoots.get());
		}).build());
	public static final RegistrySupplier<CreativeModeTab> GROUP_COLORS = REGISTRY.<CreativeModeTab>register("splatcraft_colors", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
		.icon(() -> ColorUtils.withInkColor(inkwell.get().getDefaultInstance(), ColorUtils.getOrange()))
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
		CreativeTabRegistry.append(CreativeModeTabs.REDSTONE_BLOCKS, splatSwitch);
		CreativeTabRegistry.append(CreativeModeTabs.REDSTONE_BLOCKS, remotePedestal);
	}
}
