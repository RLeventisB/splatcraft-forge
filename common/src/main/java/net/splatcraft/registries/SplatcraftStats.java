package net.splatcraft.registries;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.advancement.criterion.Criterion;
import net.minecraft.registry.Registries;
import net.minecraft.stat.StatFormatter;
import net.minecraft.util.Identifier;
import net.splatcraft.Splatcraft;
import net.splatcraft.criteriaTriggers.ChangeInkColorTrigger;
import net.splatcraft.criteriaTriggers.CraftWeaponTrigger;
import net.splatcraft.criteriaTriggers.FallIntoInkTrigger;
import net.splatcraft.criteriaTriggers.ScanTurfTrigger;

public class SplatcraftStats
{
	public static final DeferredRegister<Identifier> STAT_REGISTRY = Splatcraft.deferredRegistryOf(Registries.CUSTOM_STAT);
	public static final Identifier TURF_WARS_WON = register("turf_wars_won", StatFormatter.DEFAULT);
	public static final Identifier BLOCKS_INKED = register("blocks_inked", StatFormatter.DEFAULT);
	public static final Identifier WEAPONS_CRAFTED = register("weapons_crafted", StatFormatter.DEFAULT);
	public static final Identifier INKWELLS_CRAFTED = register("inkwells_crafted", StatFormatter.DEFAULT);
	public static final Identifier SQUID_TIME = register("squid_time", StatFormatter.TIME);
	public static DeferredRegister<Criterion<?>> CRITERION_REGISTRY = Splatcraft.deferredRegistryOf(Registries.CRITERION);
	public static final RegistrySupplier<CraftWeaponTrigger> CRAFT_WEAPON_TRIGGER = CRITERION_REGISTRY.register("craft_weapon", CraftWeaponTrigger::new);
	public static final RegistrySupplier<ChangeInkColorTrigger> CHANGE_INK_COLOR_TRIGGER = CRITERION_REGISTRY.register("change_ink_color", ChangeInkColorTrigger::new);
	public static final RegistrySupplier<ScanTurfTrigger> SCAN_TURF_TRIGGER = CRITERION_REGISTRY.register("scan_turf", ScanTurfTrigger::new);
	public static final RegistrySupplier<FallIntoInkTrigger> FALL_INTO_INK_TRIGGER = CRITERION_REGISTRY.register("fall_into_ink", FallIntoInkTrigger::new);
	private static Identifier register(String key, StatFormatter formatter)
	{
		Identifier resourcelocation = Splatcraft.identifierOf(key);
		STAT_REGISTRY.register(resourcelocation, () -> resourcelocation);
		return resourcelocation;
	}
}
