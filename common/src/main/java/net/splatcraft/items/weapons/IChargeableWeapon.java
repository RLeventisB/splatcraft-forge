package net.splatcraft.items.weapons;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.registries.SplatcraftComponents;

public interface IChargeableWeapon
{
	default float getCharge(ItemStack stack)
	{
		return stack.get(SplatcraftComponents.CHARGE_DATA).charge();
	}
	default float getPreviousCharge(ItemStack stack)
	{
		return stack.get(SplatcraftComponents.CHARGE_DATA).previousCharge();
	}
	int getChargeLevels(ItemStack stack);
	int getStorageTime(ItemStack stack);
	default void retrieveCharge(LivingEntity entity, ItemStack stack, float charge)
	{
		stack.update(SplatcraftComponents.CHARGE_DATA, SplatcraftComponents.ChargeData.DEFAULT, v -> v.withCharge(charge, charge));
	}
	default boolean canStore(ItemStack stack)
	{
		return false;
	}
}
