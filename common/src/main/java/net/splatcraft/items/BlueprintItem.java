package net.splatcraft.items;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.items.weapons.*;
import net.splatcraft.items.weapons.subs.SubWeaponItem;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class BlueprintItem extends Item
{
	public static final HashMap<String, Predicate<Item>> weaponPools = new HashMap<>()
	{{
		put("shooters", instanceOf(ShooterItem.class));
		put("blasters", instanceOf(BlasterItem.class));
		put("rollers", instanceOf(RollerItem.class));
		put("chargers", instanceOf(ChargerItem.class));
		put("sloshers", instanceOf(SlosherItem.class));
		put("splatlings", instanceOf(SplatlingItem.class));
		put("dualies", instanceOf(DualieItem.class));
		put("sub_weapons", instanceOf(SubWeaponItem.class));
		put("ink_tanks", instanceOf(InkTankItem.class));
		put("wildcard", item -> true);
	}};
	public BlueprintItem()
	{
		super(new Settings().maxCount(16));
	}
	public static Predicate<Item> instanceOf(Class<? extends Item> clazz)
	{
		return clazz::isInstance;
	}
	public static ItemStack addToAdvancementPool(ItemStack blueprint, Identifier... advancementIds)
	{
		return addToAdvancementPool(blueprint, Arrays.stream(advancementIds));
	}
	public static ItemStack setPoolFromWeaponType(ItemStack blueprint, String weaponType)
	{
		if (!weaponPools.containsKey(weaponType))
			return blueprint;
		
		List<String> pools = blueprint.getOrDefault(SplatcraftComponents.BLUEPRINT_WEAPONS, new ArrayList<>());
		pools.add(weaponType);
		blueprint.set(SplatcraftComponents.BLUEPRINT_WEAPONS, pools);
		
		return blueprint;
	}
	public static ItemStack addToAdvancementPool(ItemStack blueprint, Stream<Identifier> advancementIds)
	{
		List<Identifier> pool = blueprint.getOrDefault(SplatcraftComponents.BLUEPRINT_ADVANCEMENTS, new ArrayList<>());
		
		advancementIds.forEach(pool::add);
		blueprint.set(SplatcraftComponents.BLUEPRINT_ADVANCEMENTS, pool);
		
		return blueprint;
	}
	public static List<AdvancementEntry> getAdvancementPool(World world, ItemStack blueprint)
	{
		List<AdvancementEntry> output = new ArrayList<>();
		
		if (blueprint.contains(SplatcraftComponents.BLUEPRINT_ADVANCEMENTS))
		{
			blueprint.get(SplatcraftComponents.BLUEPRINT_ADVANCEMENTS).forEach(
				name ->
				{
					AdvancementEntry entry = world.getServer().getAdvancementLoader().get(name);
					
					if (entry != null)
						output.add(entry);
				}
			);
		}
		if (blueprint.contains(SplatcraftComponents.BLUEPRINT_WEAPONS))
		{
			for (Predicate<Item> weaponPoolPredicate : blueprint.get(SplatcraftComponents.BLUEPRINT_WEAPONS).stream().map(weaponPools::get).toList())
			{
				for (var tank : InkTankItem.inkTanks)
				{
					tryAddItemToPool(world, weaponPoolPredicate, tank, output);
				}
				for (var weapon : SplatcraftItems.weapons)
				{
					tryAddItemToPool(world, weaponPoolPredicate, weapon, output);
				}
				// yeah i rewrote this into normal code because i couldn't understand it
/*
                SplatcraftItems.weapons.stream().
                    filter(weaponPools.get(type).and(item ->
                        !Objects.equals(Registries.ITEM.getId(item), SplatcraftTags.Items.BLUEPRINT_EXCLUDED.id())))
                    .map(Registries.ITEM::getId).map(holder ->
                        Identifier.of(holder.getNamespace(), "unlocks/" + holder.getPath())).map(world.getServer().getAdvancementLoader()::get)
                    .filter(Objects::nonNull).forEach(output::add);
*/
			}
		}
		
		return output;
	}
	private static void tryAddItemToPool(World world, Predicate<Item> weaponPoolPredicate, Item item, List<AdvancementEntry> output)
	{
		if (weaponPoolPredicate.test(item) && !item.getDefaultStack().isIn(SplatcraftTags.Items.BLUEPRINT_EXCLUDED))
		{
			Identifier itemIdentifier = item.getRegistryEntry().registryKey().getValue();
			Identifier identifier = itemIdentifier.withPrefixedPath("unlocks/");
			AdvancementEntry advancementEntry = world.getServer().getAdvancementLoader().get(identifier);
			if (advancementEntry != null)
			{
				output.add(advancementEntry);
			}
		}
	}
	@Environment(EnvType.CLIENT)
	@Override
	public void appendTooltip(@NotNull ItemStack stack, @Nullable TooltipContext context, @NotNull List<Text> components, @NotNull TooltipType type)
	{
		super.appendTooltip(stack, context, components, type);
		
		if (stack.contains(DataComponentTypes.HIDE_TOOLTIP))
			return;
		
		if (stack.contains(SplatcraftComponents.BLUEPRINT_ADVANCEMENTS))
		{
			components.add(Text.translatable("item.splatcraft.blueprint.tooltip"));
			return;
		}
		
		if (stack.contains(SplatcraftComponents.BLUEPRINT_WEAPONS))
		{
			components.add(Text.translatable("item.splatcraft.blueprint.tooltip"));
			stack.get(SplatcraftComponents.BLUEPRINT_WEAPONS).forEach((weaponType) ->
				components.add(Text.translatable("item.splatcraft.blueprint.tooltip." + weaponType)
					.setStyle(Style.EMPTY.withColor(Formatting.BLUE).withItalic(false)))
			);
			return;
		}
		
		components.add(Text.translatable("item.splatcraft.blueprint.tooltip.empty"));
	}
	@Override
	public @NotNull TypedActionResult<ItemStack> use(@NotNull World world, @NotNull PlayerEntity player, @NotNull Hand hand)
	{
		if (!(player instanceof ServerPlayerEntity serverPlayer))
			return super.use(world, player, hand);
		
		ItemStack stack = player.getStackInHand(hand);
		
		List<AdvancementEntry> pool = getAdvancementPool(world, stack);
		int count = pool.size();
		
		if (count > 0)
		{
			pool.removeIf(advancement -> serverPlayer.getAdvancementTracker().getProgress(advancement).isDone());
			
			if (!pool.isEmpty())
			{
				AdvancementEntry advancement = pool.get(world.random.nextInt(pool.size()));
				
				for (String key : serverPlayer.getAdvancementTracker().getProgress(advancement).getUnobtainedCriteria())
					serverPlayer.getAdvancementTracker().grantCriterion(advancement, key);
				
				if (advancement.value().display().isPresent() && !advancement.value().display().get().shouldShowToast())
					player.sendMessage(Text.translatable("status.blueprint.unlock", advancement.value().display().get().getTitle()), true);
				
				stack.decrement(1);
				return TypedActionResult.consume(stack);
			}
			
			player.sendMessage(Text.translatable("status.blueprint.already_unlocked" + (count > 1 ? "" : ".single")), true);
			return super.use(world, player, hand);
		}
		
		player.sendMessage(Text.translatable("status.blueprint.invalid"), true);
		return super.use(world, player, hand);
	}
}
