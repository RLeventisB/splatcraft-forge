package net.splatcraft.registries;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.splatcraft.Splatcraft;
import net.splatcraft.criteriaTriggers.ChangeInkColorTrigger;
import net.splatcraft.criteriaTriggers.CraftWeaponTrigger;
import net.splatcraft.criteriaTriggers.FallIntoInkTrigger;
import net.splatcraft.criteriaTriggers.ScanTurfTrigger;
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.platform.RegistrySupplier;

public class SplatcraftStats
{
	public static final DeferredRegister<ResourceLocation> STAT_REGISTRY = Splatcraft.deferredRegistryOf(BuiltInRegistries.CUSTOM_STAT);
	public static final ResourceLocation TURF_WARS_WON = register("turf_wars_won", StatFormatter.DEFAULT);
	public static final ResourceLocation BLOCKS_INKED = register("blocks_inked", StatFormatter.DEFAULT);
	public static final ResourceLocation WEAPONS_CRAFTED = register("weapons_crafted", StatFormatter.DEFAULT);
	public static final ResourceLocation INKWELLS_CRAFTED = register("inkwells_crafted", StatFormatter.DEFAULT);
	public static final ResourceLocation SQUID_TIME = register("squid_time", StatFormatter.TIME);
	public static final DeferredRegister<CriterionTrigger<?>> CRITERION_REGISTRY = Splatcraft.deferredRegistryOf(BuiltInRegistries.TRIGGER_TYPES);
	public static final RegistrySupplier<CraftWeaponTrigger> CRAFT_WEAPON_TRIGGER = CRITERION_REGISTRY.register("craft_weapon", CraftWeaponTrigger::new);
	public static final RegistrySupplier<ChangeInkColorTrigger> CHANGE_INK_COLOR_TRIGGER = CRITERION_REGISTRY.register("change_ink_color", ChangeInkColorTrigger::new);
	public static final RegistrySupplier<ScanTurfTrigger> SCAN_TURF_TRIGGER = CRITERION_REGISTRY.register("scan_turf", ScanTurfTrigger::new);
	public static final RegistrySupplier<FallIntoInkTrigger> FALL_INTO_INK_TRIGGER = CRITERION_REGISTRY.register("fall_into_ink", FallIntoInkTrigger::new);
	private static ResourceLocation register(String key, StatFormatter formatter)
	{
		ResourceLocation resourcelocation = Splatcraft.identifierOf(key);
		STAT_REGISTRY.register(key, () -> resourcelocation);
		return resourcelocation;
	}
}
