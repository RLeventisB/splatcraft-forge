package net.splatcraft.registries;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import net.splatcraft.Splatcraft;

public class SplatcraftDamageTypes
{
    public static final ResourceKey<DamageType> ENEMY_INK = ResourceKey.create(Registries.DAMAGE_TYPE, Splatcraft.identifierOf("enemy_ink"));
    public static final ResourceKey<DamageType> INK_SPLAT = ResourceKey.create(Registries.DAMAGE_TYPE, Splatcraft.identifierOf("ink_splat"));
    public static final ResourceKey<DamageType> OUT_OF_STAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Splatcraft.identifierOf("out_of_stage"));
    public static final ResourceKey<DamageType> ROLL_CRUSH = ResourceKey.create(Registries.DAMAGE_TYPE, Splatcraft.identifierOf("roll_crush"));
    public static final ResourceKey<DamageType> WATER = ResourceKey.create(Registries.DAMAGE_TYPE, Splatcraft.identifierOf("water"));

    public static Holder.Reference<DamageType> get(Level world, ResourceKey<DamageType> key)
    {
        return world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key);
    }

    public static DamageSource of(Level world, ResourceKey<DamageType> key)
    {
        return new DamageSource(get(world, key));
    }
}