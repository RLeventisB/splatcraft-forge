package net.splatcraft.items.weapons.settings;

import net.splatcraft.util.WeaponTooltip;

import java.util.List;

public interface DynamicDataRecord<Self>
{
	Self convertSelf();
	default <T extends DynamicDataRecord<T>> void addTooltips(List<WeaponTooltip<SubWeaponSettings<T>>> weaponTooltips)
	{
	
	}
}
