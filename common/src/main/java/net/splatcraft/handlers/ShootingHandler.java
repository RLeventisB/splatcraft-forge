package net.splatcraft.handlers;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.items.weapons.WeaponBaseItem;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

public class ShootingHandler
{
	public static boolean isDoingShootingAction(LivingEntity entity)
	{
		boolean mainhandPrevent = entity.getMainHandItem().getItem() instanceof WeaponBaseItem<?> weaponItem && weaponItem.preventsChanging(entity.getMainHandItem());
		boolean offhandPrevent = entity.getOffhandItem().getItem() instanceof WeaponBaseItem<?> weaponItem && weaponItem.preventsChanging(entity.getOffhandItem());
		return mainhandPrevent || offhandPrevent;
	}
	public static boolean isDoingShootingActionOnBothHands(LivingEntity entity)
	{
		boolean mainhandPrevent = entity.getMainHandItem().getItem() instanceof WeaponBaseItem<?> weaponItem && weaponItem.preventsChanging(entity.getMainHandItem());
		boolean offhandPrevent = entity.getOffhandItem().getItem() instanceof WeaponBaseItem<?> weaponItem && weaponItem.preventsChanging(entity.getOffhandItem());
		return mainhandPrevent && offhandPrevent;
	}
	public static <C> void tickShootingComponent(@NotNull ItemStack stack, DataComponentType<C> componentType, C componentDefault, UnaryOperator<C> o)
	{
		stack.update(componentType, componentDefault, o);
	}
}