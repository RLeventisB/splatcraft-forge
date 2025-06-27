package net.splatcraft.items.weapons.settings;

import net.splatcraft.util.WeaponTooltip;

import java.util.List;

public interface DynamicDataRecord<Self>
{
	Self convertSelf();
	default <T extends DynamicDataRecord<T>, S extends DynamicWeaponSettings<S, ?, T, ?>> void addTooltips(List<WeaponTooltip<S>> weaponTooltips)
	{

	}
}
