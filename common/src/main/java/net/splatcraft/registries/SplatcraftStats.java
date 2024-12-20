package net.splatcraft.registries;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.advancement.criterion.Criterion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.stat.StatFormatter;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.splatcraft.Splatcraft;
import net.splatcraft.criteriaTriggers.ChangeInkColorTrigger;
import net.splatcraft.criteriaTriggers.CraftWeaponTrigger;
import net.splatcraft.criteriaTriggers.FallIntoInkTrigger;
import net.splatcraft.criteriaTriggers.ScanTurfTrigger;

public class SplatcraftStats
{
    public static final Identifier TURF_WARS_WON = register("turf_wars_won", StatFormatter.DEFAULT);
    public static final Identifier BLOCKS_INKED = register("blocks_inked", StatFormatter.DEFAULT);
    public static final Identifier WEAPONS_CRAFTED = register("weapons_crafted", StatFormatter.DEFAULT);
    public static final Identifier INKWELLS_CRAFTED = register("inkwells_crafted", StatFormatter.DEFAULT);
    public static final Identifier SQUID_TIME = register("squid_time", StatFormatter.TIME);
    public static DeferredRegister<Criterion<?>> REGISTRY = Splatcraft.deferredRegistryOf(Registries.CRITERION);
    public static final RegistrySupplier<CraftWeaponTrigger> CRAFT_WEAPON_TRIGGER = REGISTRY.register("craft_weapon", CraftWeaponTrigger::new);
    public static final RegistrySupplier<ChangeInkColorTrigger> CHANGE_INK_COLOR_TRIGGER = REGISTRY.register("change_ink_color", ChangeInkColorTrigger::new);
    public static final RegistrySupplier<ScanTurfTrigger> SCAN_TURF_TRIGGER = REGISTRY.register("scan_turf", () -> new ScanTurfTrigger());
    public static final RegistrySupplier<FallIntoInkTrigger> FALL_INTO_INK_TRIGGER = REGISTRY.register("fall_into_ink", FallIntoInkTrigger::new);

    public static void register()
    {
    }

    private static Identifier register(String key, StatFormatter formatter)
    {
        Identifier resourcelocation = Splatcraft.identifierOf(key);
        Registry.register(Registries.CUSTOM_STAT, Splatcraft.identifierOf(key), resourcelocation);
        Stats.CUSTOM.getOrCreateStat(resourcelocation, formatter);
        return resourcelocation;
    }
}
