package net.splatcraft.handlers;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.items.weapons.settings.AbstractWeaponSettings;
import net.splatcraft.items.weapons.settings.DynamicDataRecord;
import net.splatcraft.items.weapons.settings.SpecialWeaponRecords;
import net.splatcraft.items.weapons.settings.SpecialWeaponSettings;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.specials.StingRayAction;
import org.apache.commons.lang3.function.TriConsumer;

import java.util.Map;

public class SpecialHandler
{
	public static final Map<ResourceLocation, TriConsumer<LivingEntity, SpecialWeaponSettings, EntitySlot>> specialExecutor = new Object2ObjectLinkedOpenHashMap<>();
	private static Supplier<Map<ResourceLocation, SpecialWeaponSettings<?>>> specialMapSupplier;
	public static void registerSpecials()
	{
		// recalculate the map thingy since we call this when reloading!!!
		specialMapSupplier = Suppliers.memoize(() ->
			Map.ofEntries(DataHandler.WeaponStatsListener.SETTINGS.entrySet().stream()
				.filter(v -> v.getValue() instanceof SpecialWeaponSettings<?>)
				.map(v -> Map.entry(v.getKey(), (SpecialWeaponSettings) v.getValue()))
				.toArray(Map.Entry[]::new))
		);
		specialExecutor.clear();
		registerSpecialExecutor(SpecialWeaponRecords.StingRayDataRecord.ID, (entity, settings, slot) ->
		{
			EntityAction.setEntityAction(entity, new StingRayAction(settings, CommonUtils.getSlot(entity), slot));
		});
	}
	public static void registerSpecialExecutor(ResourceLocation specialId, TriConsumer<LivingEntity, SpecialWeaponSettings, EntitySlot> delegate)
	{
		specialExecutor.put(specialId.withPrefix("specials/"), delegate);
	}
	public static Map<ResourceLocation, SpecialWeaponSettings<?>> getSpecialMap()
	{
		return specialMapSupplier.get();
	}
	public static boolean passesSpecialCost(ItemStack weaponStack, ItemStack providerStack, ResourceLocation specialId)
	{
		SplatcraftComponents.SpecialProviderData data = providerStack.get(SplatcraftComponents.SPECIAL_PROVIDER_DATA);
		
		int specialPoints = data == null ? 0 : data.storedPoints();
		SpecialWeaponSettings<?> settings = getSpecialSettings(specialId);
		int requiredSpecialPoints = getRequiredSpecialPoints(weaponStack, data, settings);
		return specialPoints >= requiredSpecialPoints;
	}
	public static int getRequiredSpecialPoints(ItemStack weaponStack, ItemStack providerStack)
	{
		SplatcraftComponents.SpecialProviderData data = providerStack.get(SplatcraftComponents.SPECIAL_PROVIDER_DATA);
		return getRequiredSpecialPoints(weaponStack, data, data == null || data.specialId().isEmpty() ? null : getSpecialSettings(data.specialId().get()));
	}
	public static int getRequiredSpecialPoints(ItemStack weaponStack, SplatcraftComponents.SpecialProviderData data, SpecialWeaponSettings<?> settings)
	{
		int requiredSpecialPoints;
		if (data == null || data.pointsPerSpecialOverride().isEmpty())
		{
			if (settings == null)
				requiredSpecialPoints = 200;
			else
				requiredSpecialPoints = settings.dataRecord.costData().getCost(weaponStack);
		}
		else
		{
			requiredSpecialPoints = data.pointsPerSpecialOverride().get();
		}
		return requiredSpecialPoints;
	}
	public static int getSpecialPoints(ItemStack providerStack)
	{
		SplatcraftComponents.SpecialProviderData data = providerStack.get(SplatcraftComponents.SPECIAL_PROVIDER_DATA);
		return data == null ? 0 : data.storedPoints();
	}
	public static <T extends DynamicDataRecord<T>> SpecialWeaponSettings<T> getSpecialSettings(ResourceLocation identifier, Class<T> specialDataClass)
	{
		AbstractWeaponSettings<?, ?> settings = DataHandler.WeaponStatsListener.SETTINGS.get(identifier);
		if (settings instanceof SpecialWeaponSettings specialSettings && specialDataClass.isInstance(specialSettings.specialDataRecord))
		{
			return specialSettings;
		}
		return null;
	}
	public static SpecialWeaponSettings<?> getSpecialSettings(ResourceLocation specialId)
	{
		AbstractWeaponSettings<?, ?> settings = DataHandler.WeaponStatsListener.SETTINGS.get(specialId);
		if (settings instanceof SpecialWeaponSettings specialSettings)
		{
			return specialSettings;
		}
		return null;
	}
	public static EntitySlot startUsingSpecial(LivingEntity entity, ResourceLocation specialId)
	{
		return startUsingSpecial(entity, specialId, ItemStack.EMPTY);
	}
	public static EntitySlot startUsingSpecial(LivingEntity entity, ResourceLocation specialId, ItemStack providerStack)
	{
		if (!providerStack.isEmpty())
			SplatcraftComponents.applyToComponentIfContains(providerStack, SplatcraftComponents.SPECIAL_PROVIDER_DATA, v -> v.withStoredPoints(0));
		
		EntitySlot slot = EntitySlot.createFor(entity, providerStack);
		startUsingSpecial(entity, specialId, slot);
		return slot;
	}
	public static void startUsingSpecial(LivingEntity entity, ResourceLocation specialId, EntitySlot slot)
	{
		AbstractWeaponSettings<?, ?> settings = DataHandler.WeaponStatsListener.SETTINGS.get(specialId);
		if (settings instanceof SpecialWeaponSettings specialSettings)
		{
			specialExecutor.get(specialId).accept(entity, specialSettings, slot);
		}
	}
}
