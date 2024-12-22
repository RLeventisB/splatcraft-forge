package net.splatcraft.registries;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.advancement.criterion.Criterion;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.splatcraft.Splatcraft;
import net.splatcraft.criteriaTriggers.ChangeInkColorTrigger;
import net.splatcraft.criteriaTriggers.CraftWeaponTrigger;
import net.splatcraft.criteriaTriggers.FallIntoInkTrigger;
import net.splatcraft.criteriaTriggers.ScanTurfTrigger;

public class SplatcraftStats
{
    public static final Identifier TURF_WARS_WON = Splatcraft.identifierOf("turf_wars_won");;
    public static final Identifier BLOCKS_INKED = Splatcraft.identifierOf("blocks_inked");
    public static final Identifier WEAPONS_CRAFTED = Splatcraft.identifierOf("weapons_crafted");
    public static final Identifier INKWELLS_CRAFTED = Splatcraft.identifierOf("inkwells_crafted");
    public static final Identifier SQUID_TIME = Splatcraft.identifierOf("squid_time");
    public static DeferredRegister<Criterion<?>> CRITERION_REGISTRY = Splatcraft.deferredRegistryOf(Registries.CRITERION);
    public static final RegistrySupplier<CraftWeaponTrigger> CRAFT_WEAPON_TRIGGER = CRITERION_REGISTRY.register("craft_weapon", CraftWeaponTrigger::new);
    public static final RegistrySupplier<ChangeInkColorTrigger> CHANGE_INK_COLOR_TRIGGER = CRITERION_REGISTRY.register("change_ink_color", ChangeInkColorTrigger::new);
    public static final RegistrySupplier<ScanTurfTrigger> SCAN_TURF_TRIGGER = CRITERION_REGISTRY.register("scan_turf", ScanTurfTrigger::new);
    public static final RegistrySupplier<FallIntoInkTrigger> FALL_INTO_INK_TRIGGER = CRITERION_REGISTRY.register("fall_into_ink", FallIntoInkTrigger::new);
}
