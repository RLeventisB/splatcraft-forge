package net.splatcraft.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.splatcraft.Splatcraft;
import org.jetbrains.annotations.NotNull;

public class SplatcraftArmorMaterial implements ArmorMaterial
{
    private final int durability;
    private final int damageReduction;
    private final int enchantability;
    private final SoundEvent soundEvent;
    private final Ingredient repairMaterial;
    private final String name;
    private final float toughness;
    private final float knockbackResistance;

    public SplatcraftArmorMaterial(String name, int durability, int damageReduction, float toughness, float knockbackResistance, int enchantability, SoundEvent equipSound, Ingredient repairMaterial)
    {
        this.name = new ResourceLocation(Splatcraft.MODID, name).toString();
        this.durability = durability;
        this.damageReduction = damageReduction;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.enchantability = enchantability;
        this.soundEvent = equipSound;
        this.repairMaterial = repairMaterial;
    }

    public SplatcraftArmorMaterial(String name, SoundEvent equipSound, int armor, float toughness, float knockbackResistance)
    {
        this(name, -1, armor, toughness, knockbackResistance, 0, equipSound, null);
    }

    public SplatcraftArmorMaterial(String name, SplatcraftArmorMaterial parent)
    {
        this(name, parent.soundEvent, parent.damageReduction, parent.toughness, parent.knockbackResistance);
    }

    @Override
    public int getDurabilityForType(@NotNull ArmorItem.Type slotIn)
    {
        return durability;
    }

    @Override
    public int getDefenseForType(@NotNull ArmorItem.Type slotIn)
    {
        return damageReduction;
    }

    @Override
    public int getEnchantmentValue()
    {
        return enchantability;
    }

    @Override
    public @NotNull SoundEvent getEquipSound()
    {
        return soundEvent;
    }

    @Override
    public @NotNull Ingredient getRepairIngredient()
    {
        return repairMaterial;
    }

    @Override
    public @NotNull String getName()
    {
        return name;
    }

    @Override
    public float getToughness()
    {
        return toughness;
    }

    @Override
    public float getKnockbackResistance()
    {
        return knockbackResistance;
    }
}
