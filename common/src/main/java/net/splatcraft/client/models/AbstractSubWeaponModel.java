package net.splatcraft.client.models;

import net.minecraft.client.model.EntityModel;
import net.splatcraft.entities.subs.AbstractSubWeaponEntity;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractSubWeaponModel<T extends AbstractSubWeaponEntity> extends EntityModel<T>
{
	// this is empty most of the time so might as well
	@Override
	public void setupAnim(@NotNull T t, float v, float v1, float v2, float v3, float v4)
	{
	
	}
}
