package net.splatcraft.platform.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.splatcraft.platform.event.types.SimpleEvent;

public interface EntityEvents
{
	@FunctionalInterface
	public interface LivingDeath extends SimpleEvent.Bi<LivingEntity, DamageSource>
	{
		EventResult invoke(LivingEntity entity, DamageSource deathSource);
	}
}

