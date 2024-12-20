package net.splatcraft.registries;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;
import net.splatcraft.Splatcraft;

public class SplatcraftDamageTypes
{
    public static final RegistryKey<DamageType> ENEMY_INK = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Splatcraft.identifierOf("enemy_ink"));
    public static final RegistryKey<DamageType> INK_SPLAT = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Splatcraft.identifierOf("ink_splat"));
    public static final RegistryKey<DamageType> OUT_OF_STAGE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Splatcraft.identifierOf("out_of_stage"));
    public static final RegistryKey<DamageType> ROLL_CRUSH = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Splatcraft.identifierOf("roll_crush"));
    public static final RegistryKey<DamageType> WATER = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Splatcraft.identifierOf("water"));

    public static RegistryEntry.Reference<DamageType> get(World world, RegistryKey<DamageType> key)
    {
        return world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(key);
    }

    public static DamageSource of(World world, RegistryKey<DamageType> key)
    {
        return new DamageSource(get(world, key));
    }
}