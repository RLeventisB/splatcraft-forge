package net.splatcraft.registries;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.InkColorGroups;
import net.splatcraft.data.InkColorRegistry;
import net.splatcraft.items.ColoredBlockItem;
import net.splatcraft.items.InkTankItem;
import net.splatcraft.items.weapons.SubWeaponItem;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;

import java.util.ArrayList;
import java.util.List;

import static net.splatcraft.registries.SplatcraftItems.*;

public class SplatcraftItemGroups
{
	public static final ArrayList<Item> colorTabItems = new ArrayList<>();
	protected static final DeferredRegister<ItemGroup> REGISTRY = Splatcraft.deferredRegistryOf(Registries.ITEM_GROUP);
	public static final RegistrySupplier<ItemGroup> GROUP_GENERAL = REGISTRY.register(Splatcraft.identifierOf("splatcraft_general"), () -> ItemGroup.create(ItemGroup.Row.TOP, 1)
		.icon(() -> sardiniumBlock.get().getDefaultStack())
		.displayName(Text.translatable("itemGroup.splatcraft_general"))
		.entries((parameters, output) ->
		{
			//Materials
			output.add(sardinium.get());
			output.add(sardiniumBlock.get());
			output.add(rawSardinium.get());
			output.add(rawSardiniumBlock.get());
			output.add(sardiniumOre.get());
			output.add(powerEgg.get());
			output.add(powerEggCan.get());
			output.add(powerEggBlock.get());
			output.add(emptyInkwell.get());
			output.add(ammoKnightsScrap.get());
			output.add(blueprint.get());
			output.add(kensaPin.get());
			
			//Remotes
			output.add(stagePad.get());
			output.add(turfScanner.get());
			output.add(inkDisruptor.get());
			output.add(colorChanger.get());
			output.add(remotePedestal.get());
			
			//Gear
			output.add(superJumpLure.get());
			output.add(splatfestBand.get());
			output.add(clearBand.get());
			output.add(waxApplicator.get());
			
			//Filters
			output.add(emptyFilter.get());
			output.add(pastelFilter.get());
			output.add(organicFilter.get());
			output.add(neonFilter.get());
			output.add(overgrownFilter.get());
			output.add(midnightFilter.get());
			output.add(enchantedFilter.get());
			output.add(creativeFilter.get());
			
			//Crafting Stations
			output.add(inkVat.get());
			output.add(weaponWorkbench.get());
			
			//Colored Items
			output.addAll(ColorUtils.getColorVariantsForItem(inkwell.get(), true, true, true));
			output.addAll(ColorUtils.getColorVariantsForItem(spawnPad.get(), true, true, false));
			output.addAll(ColorUtils.getColorVariantsForItem(squidBumper.get(), true, true, false));
			output.addAll(ColorUtils.getColorVariantsForItem(inkedWool.get(), true, true, false));
			output.addAll(ColorUtils.getColorVariantsForItem(inkedCarpet.get(), true, true, false));
			output.addAll(ColorUtils.getColorVariantsForItem(inkedGlass.get(), true, true, false));
			output.addAll(ColorUtils.getColorVariantsForItem(inkedGlassPane.get(), true, true, false));
			
			//Decor Blocks
			output.add(canvas.get());
			output.add(coralite.get());
			output.add(coraliteSlab.get());
			output.add(coraliteStairs.get());
			output.add(grate.get());
			output.add(grateRamp.get());
			output.add(barrierBar.get());
			output.add(platedBarrierBar.get());
			output.add(cautionBarrierBar.get());
			output.add(tarp.get());
			output.add(glassCover.get());
			output.add(crate.get());
			output.add(sunkenCrate.get());
			output.add(splatSwitch.get());
			
			//Stage Barriers
			output.add(stageBarrier.get());
			output.add(stageVoid.get());
			output.addAll(ColorUtils.getColorVariantsForItem(allowedColorBarrier.get(), true, true, false));
			output.addAll(ColorUtils.getColorVariantsForItem(deniedColorBarrier.get(), true, true, false));
		}).build());
	public static final RegistrySupplier<ItemGroup> GROUP_WEAPONS = REGISTRY.register("splatcraft_weapons", () -> ItemGroup.create(ItemGroup.Row.TOP, 1)
		.icon(() -> ColorUtils.setInkColor(splattershot.get().getDefaultStack(), InkColorRegistry.getColorByAliasOrHex("splatcraft:orange")))
		.displayName(Text.translatable("itemGroup.splatcraft_weapons"))
		.entries((parameters, output) ->
		{
			List<WeaponBaseItem<?>> visibleWeapons = weapons.stream().filter(weapon -> !weapon.isSecret).toList();
			output.addAll(visibleWeapons.stream().filter(weapon -> !(weapon instanceof SubWeaponItem)).map(Item::getDefaultStack).toList());
			List<WeaponBaseItem<?>> subWeapons = visibleWeapons.stream().filter(weapon -> weapon instanceof SubWeaponItem).toList();
			output.addAll(subWeapons.stream().map(Item::getDefaultStack).toList());
			output.addAll(subWeapons.stream().map(weaponBaseItem ->
			{
				ItemStack stack = weaponBaseItem.getDefaultStack();
				stack.set(SplatcraftComponents.SINGLE_USE, true);
				return stack;
			}).toList());
			output.addAll(InkTankItem.inkTanks.stream().map(Item::getDefaultStack).toList());
			
			output.add(inkClothHelmet.get());
			output.add(inkClothChestplate.get());
			output.add(inkClothLeggings.get());
			output.add(inkClothBoots.get());
		}).build());
	public static final RegistrySupplier<ItemGroup> GROUP_COLORS = REGISTRY.register("splatcraft_colors", () -> ItemGroup.create(ItemGroup.Row.TOP, 1)
		.icon(() -> ColorUtils.setInkColor(inkwell.get().getDefaultStack(), InkColorRegistry.getColorByAliasOrHex("splatcraft:orange")))
		.displayName(Text.translatable("itemGroup.splatcraft_colors"))
		.noScrollbar()
		.entries((parameters, output) ->
		{
			for (Item item : colorTabItems)
			{
				for (InkColor color : InkColorGroups.CREATIVE_TAB_COLORS.getAll().stream().toList())
					output.add(ColorUtils.setColorLocked(ColorUtils.setInkColor(new ItemStack(item), color), true));
				if (!(item instanceof ColoredBlockItem coloredBlockItem) || coloredBlockItem.matchesColor())
					output.add(ColorUtils.setInverted(new ItemStack(item), true));
			}
		})
		.build());
	public static void addSplatcraftItemsToVanillaGroups()
	{
		CreativeTabRegistry.append(ItemGroups.REDSTONE, splatSwitch.get());
		CreativeTabRegistry.append(ItemGroups.REDSTONE, remotePedestal.get());
	}
}
