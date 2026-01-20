package net.splatcraft.handlers;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.DynamicDataRecord;
import net.splatcraft.items.weapons.settings.SpecialWeaponRecords;
import net.splatcraft.items.weapons.settings.SpecialWeaponSettings;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateEntityActionOnlyPacket;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.specials.InkStormAction;
import net.splatcraft.util.action.specials.InkjetAction;
import net.splatcraft.util.action.specials.StingRayAction;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

public class SpecialHandler
{
	public static final Map<ResourceLocation, Pair<SpecialExecutorAction, BiPredicate<LivingEntity, ItemStack>>> specialExecutor = new Object2ObjectLinkedOpenHashMap<>();
	public static final Integer DEFAULT_SPECIAL_COST = 200;
	private static Supplier<Map<ResourceLocation, SpecialWeaponSettings<?>>> specialMapSupplier;
	public static void registerSpecials()
	{
		// recalculate the map thingy since we call this when reloading!!!
		specialMapSupplier = Suppliers.memoize(() ->
			Map.ofEntries(DataHandler.WeaponStatsListener.SETTINGS.entrySet().stream()
				.filter(v -> v.getValue() instanceof SpecialWeaponSettings<?>)
				.map(v -> Map.entry(v.getKey().withPath(key -> key.replaceFirst("specials/", "")), (SpecialWeaponSettings) v.getValue()))
				.toArray(Map.Entry[]::new))
		);
		specialExecutor.clear();
		registerSpecialExecutor(SpecialWeaponRecords.StingRayDataRecord.ID, (entity, settings, providerSlot, weaponSlot) ->
		{
			EntityAction.setEntityAction(entity, new StingRayAction(settings, weaponSlot, providerSlot));
		});
		registerSpecialExecutor(SpecialWeaponRecords.InkJetDataRecord.ID, (entity, settings, providerSlot, weaponSlot) ->
		{
			Optional<Vec3> lastGroundedPos = WeaponHandler.getEntityLastGroundedPos(entity);
			lastGroundedPos.ifPresent(vec3 ->
				EntityAction.setEntityAction(entity, new InkjetAction(settings, weaponSlot, providerSlot, vec3)));
		}, (entity, stack) -> WeaponHandler.getEntityLastGroundedPos(entity).isPresent());
		registerSpecialExecutor(SpecialWeaponRecords.InkStormDataRecord.ID, (entity, settings, providerSlot, weaponSlot) ->
		{
			EntityAction.setEntityAction(entity, new InkStormAction(settings, weaponSlot, providerSlot));
		});
	}
	public static void registerSpecialExecutor(ResourceLocation specialId, SpecialExecutorAction delegate)
	{
		specialExecutor.put(specialId, Pair.of(delegate, (a, b) -> true));
	}
	public static void registerSpecialExecutor(ResourceLocation specialId, SpecialExecutorAction delegate, BiPredicate<LivingEntity, ItemStack> predicate)
	{
		specialExecutor.put(specialId, Pair.of(delegate, predicate));
	}
	public static Map<ResourceLocation, SpecialWeaponSettings<?>> getSpecialMap()
	{
		return specialMapSupplier.get();
	}
	public static boolean passesSpecialCost(ItemStack providerStack)
	{
		SplatcraftComponents.SpecialProviderData data = providerStack.get(SplatcraftComponents.SPECIAL_PROVIDER_DATA);

		return data != null && data.storedCharge() >= 1;
	}
	public static boolean passesSpecialConditions(LivingEntity entity, ItemStack providerStack)
	{
		SplatcraftComponents.SpecialProviderData data = providerStack.get(SplatcraftComponents.SPECIAL_PROVIDER_DATA);
		if (data == null || data.specialId().isEmpty())
			return false;

		return specialExecutor.containsKey(data.specialId().get()) && specialExecutor.get(data.specialId().get()).getSecond().test(entity, providerStack);
	}
	public static int getSpecialCost(ItemStack weaponStack, ItemStack providerStack)
	{
		SplatcraftComponents.SpecialProviderData data = providerStack.get(SplatcraftComponents.SPECIAL_PROVIDER_DATA);
		if (data == null || data.specialId().isEmpty())
			return DEFAULT_SPECIAL_COST;

		Optional<ResourceLocation> weaponId = WeaponBaseItem.getWeaponId(weaponStack);
		if (weaponId.isEmpty())
		{
			SpecialWeaponSettings<?> specialSettings = getSpecialSettings(data.specialId().get());
			if (specialSettings == null)
				return DEFAULT_SPECIAL_COST;

			return specialSettings.dataRecord.costData().defaultPoints();
		}

		return getSpecialCost(weaponId.get(), data.specialId().get());
	}
	public static int getSpecialCost(ResourceLocation weaponId, ResourceLocation specialId)
	{
		SpecialWeaponSettings<?> specialSettings = getSpecialSettings(specialId);
		if (specialSettings == null)
			return DEFAULT_SPECIAL_COST;
		return specialSettings.dataRecord.costData().getCost(weaponId);
	}
	public static <T extends DynamicDataRecord<T>> SpecialWeaponSettings<T> getSpecialSettings(ResourceLocation identifier, Class<T> specialDataClass)
	{
		SpecialWeaponSettings settings = getSpecialMap().get(identifier);
		if (settings != null && specialDataClass.isInstance(settings.specialDataRecord))
		{
			return settings;
		}
		return null;
	}
	public static SpecialWeaponSettings<?> getSpecialSettings(ResourceLocation specialId)
	{
		return getSpecialMap().get(specialId);
	}
	public static Pair<EntitySlot, EntitySlot> startUsingSpecial(LivingEntity entity, ResourceLocation specialId, ItemStack providerStack, ItemStack weaponStack)
	{
		EntitySlot providerSlot = EntitySlot.searchAndCreateWithStack(entity, providerStack);
		EntitySlot weaponSlot = EntitySlot.searchAndCreateWithStack(entity, weaponStack);
		startUsingSpecial(entity, specialId, providerSlot, weaponSlot);
		return Pair.of(providerSlot, weaponSlot);
	}
	public static void startUsingSpecial(LivingEntity entity, ResourceLocation specialId, EntitySlot providerSlot, EntitySlot weaponSlot)
	{
		SpecialWeaponSettings settings = getSpecialMap().get(specialId);
		if (settings == null)
			return;

		specialExecutor.get(specialId).getFirst().execute(entity, settings, providerSlot, weaponSlot);
		SplatcraftPacketHandler.sendToTrackersAndSelf(UpdateEntityActionOnlyPacket.create(entity), entity);
	}
	@FunctionalInterface
	public interface SpecialExecutorAction
	{
		void execute(LivingEntity entity, SpecialWeaponSettings settings, EntitySlot providerSlot, EntitySlot weaponSlot);
	}
	@FunctionalInterface
	public interface ResetAction
	{
		ResetAction RESET_FAILED = () ->
		{
		};
		void run();
	}
}
