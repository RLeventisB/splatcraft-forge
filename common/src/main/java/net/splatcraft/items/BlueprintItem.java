package net.splatcraft.items;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
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
		super(new Properties().stacksTo(16));
	}
	public static Predicate<Item> instanceOf(Class<? extends Item> clazz)
	{
		return clazz::isInstance;
	}
	public static ItemStack addToAdvancementPool(ItemStack blueprint, ResourceLocation... advancementIds)
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
	public static ItemStack addToAdvancementPool(ItemStack blueprint, Stream<ResourceLocation> advancementIds)
	{
		List<ResourceLocation> pool = blueprint.getOrDefault(SplatcraftComponents.BLUEPRINT_ADVANCEMENTS, new ArrayList<>());
		
		advancementIds.forEach(pool::add);
		blueprint.set(SplatcraftComponents.BLUEPRINT_ADVANCEMENTS, pool);
		
		return blueprint;
	}
	public static List<AdvancementHolder> getAdvancementPool(Level world, ItemStack blueprint)
	{
		List<AdvancementHolder> output = new ArrayList<>();
		
		if (blueprint.has(SplatcraftComponents.BLUEPRINT_ADVANCEMENTS))
		{
			blueprint.get(SplatcraftComponents.BLUEPRINT_ADVANCEMENTS).forEach(
				name ->
				{
					AdvancementHolder entry = world.getServer().getAdvancements().get(name);
					
					if (entry != null)
						output.add(entry);
				}
			);
		}
		if (blueprint.has(SplatcraftComponents.BLUEPRINT_WEAPONS))
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
	private static void tryAddItemToPool(Level world, Predicate<Item> weaponPoolPredicate, Item item, List<AdvancementHolder> output)
	{
		if (weaponPoolPredicate.test(item) && !item.getDefaultInstance().is(SplatcraftTags.Items.BLUEPRINT_EXCLUDED))
		{
			ResourceLocation itemIdentifier = item.builtInRegistryHolder().key().location();
			ResourceLocation identifier = itemIdentifier.withPrefix("unlocks/");
			AdvancementHolder advancementEntry = world.getServer().getAdvancements().get(identifier);
			if (advancementEntry != null)
			{
				output.add(advancementEntry);
			}
		}
	}
	@Environment(EnvType.CLIENT)
	@Override
	public void appendHoverText(@NotNull ItemStack stack, @Nullable TooltipContext context, @NotNull List<Component> components, @NotNull TooltipFlag type)
	{
		super.appendHoverText(stack, context, components, type);
		
		if (stack.has(DataComponents.HIDE_TOOLTIP))
			return;
		
		if (stack.has(SplatcraftComponents.BLUEPRINT_ADVANCEMENTS))
		{
			components.add(Component.translatable("item.splatcraft.blueprint.tooltip"));
			return;
		}
		
		if (stack.has(SplatcraftComponents.BLUEPRINT_WEAPONS))
		{
			components.add(Component.translatable("item.splatcraft.blueprint.tooltip"));
			stack.get(SplatcraftComponents.BLUEPRINT_WEAPONS).forEach((weaponType) ->
				components.add(Component.translatable("item.splatcraft.blueprint.tooltip." + weaponType)
					.setStyle(Style.EMPTY.withColor(ChatFormatting.BLUE).withItalic(false)))
			);
			return;
		}
		
		components.add(Component.translatable("item.splatcraft.blueprint.tooltip.empty"));
	}
	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player player, @NotNull InteractionHand hand)
	{
		if (!(player instanceof ServerPlayer serverPlayer))
			return super.use(world, player, hand);
		
		ItemStack stack = player.getItemInHand(hand);
		
		List<AdvancementHolder> pool = getAdvancementPool(world, stack);
		int count = pool.size();
		
		if (count > 0)
		{
			pool.removeIf(advancement -> serverPlayer.getAdvancements().getOrStartProgress(advancement).isDone());
			
			if (!pool.isEmpty())
			{
				AdvancementHolder advancement = pool.get(world.random.nextInt(pool.size()));
				
				for (String key : serverPlayer.getAdvancements().getOrStartProgress(advancement).getRemainingCriteria())
					serverPlayer.getAdvancements().award(advancement, key);
				
				if (advancement.get().display().isPresent() && !advancement.get().display().get().shouldShowToast())
					player.displayClientMessage(Component.translatable("status.blueprint.unlock", advancement.get().display().get().getTitle()), true);
				
				stack.shrink(1);
				return InteractionResultHolder.consume(stack);
			}
			
			player.displayClientMessage(Component.translatable("status.blueprint.already_unlocked" + (count > 1 ? "" : ".single")), true);
			return super.use(world, player, hand);
		}
		
		player.displayClientMessage(Component.translatable("status.blueprint.invalid"), true);
		return super.use(world, player, hand);
	}
}
