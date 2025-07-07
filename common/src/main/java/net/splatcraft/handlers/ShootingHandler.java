package net.splatcraft.handlers;

import net.minecraft.world.entity.LivingEntity;
import net.splatcraft.items.weapons.WeaponBaseItem;

public class ShootingHandler
{
	public static boolean isDoingShootingAction(LivingEntity entity)
	{
		return WeaponHandler.getUsingWeaponHand(entity).isPresent();
	}
	public static boolean isDoingShootingActionOnBothHands(LivingEntity entity)
	{
		boolean mainhandPrevent = entity.getMainHandItem().getItem() instanceof WeaponBaseItem<?> weaponItem && weaponItem.preventsChanging(entity.getMainHandItem(), entity);
		boolean offhandPrevent = entity.getOffhandItem().getItem() instanceof WeaponBaseItem<?> weaponItem && weaponItem.preventsChanging(entity.getOffhandItem(), entity);
		return mainhandPrevent && offhandPrevent;
	}
}