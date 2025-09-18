package net.splatcraft.util.action.specials;

import net.minecraft.world.entity.LivingEntity;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.structs.trajectory.TrajectoryProcessor;

public interface ActionWithThrowable extends EntityAction
{
	TrajectoryProcessor getTrajectory(LivingEntity entity, float partialTicks);
}
