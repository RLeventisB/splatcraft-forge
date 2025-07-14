package net.splatcraft.util.action;

import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public interface ActionThatSetsSquid extends EntityAction
{
	Optional<Boolean> isSquid(LivingEntity entity);
}
