package net.splatcraft.util.structs.trajectory;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

interface TrajectoryLauncher
{
	Vec3 getInitialVelocity(LivingEntity entity, float partialTicks);
}
interface SimpleTrajectoryLauncher extends TrajectoryLauncher
{
	default Vec3 getInitialVelocity(LivingEntity entity, float partialTicks)
	{
		Vec3 projectileVelocity = Vec3.directionFromRotation(entity.getViewXRot(partialTicks) + pitchOffset(), entity.getViewYRot(partialTicks)).scale(initialVelocity());
		Vec3 throwerVelocity = entity.getDeltaMovement().scale(throwerImpulse());
		return projectileVelocity.add(throwerVelocity);
	}
	float initialVelocity();
	double throwerImpulse();
	float pitchOffset();
}
interface HorizontalModTrajectoryLauncher extends TrajectoryLauncher
{
	default Vec3 getInitialVelocity(LivingEntity entity, float partialTicks)
	{
		float throwXRot = Mth.clamp(entity.getViewXRot(partialTicks) + pitchOffset(), -89, 89);
		Vec3 projectileVelocity = Vec3.directionFromRotation(throwXRot, entity.getViewYRot(partialTicks)).scale(initialVelocity());
		
		double yVelocity = projectileVelocity.y();
		projectileVelocity = projectileVelocity.multiply(1, 0, 1).normalize().scale(initialVelocity()).add(0, yVelocity, 0);
		
		Vec3 throwerVelocity = entity.getDeltaMovement().scale(throwerImpulse());
		return projectileVelocity.add(throwerVelocity);
	}
	float initialVelocity();
	double throwerImpulse();
	float pitchOffset();
}
