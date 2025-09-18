package net.splatcraft.util.structs.trajectory;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

interface StartLauncher
{
	Vec3 getInitialVelocity(LivingEntity entity, float partialTicks);
}
interface SimpleStartLauncher extends StartLauncher
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
interface ForcedPitchStartLauncher extends StartLauncher
{
	default Vec3 getInitialVelocity(LivingEntity entity, float partialTicks)
	{
		Vec3 projectileVelocity = Vec3.directionFromRotation(forcedPitch(), entity.getViewYRot(partialTicks)).scale(initialVelocity());
		Vec3 throwerVelocity = entity.getDeltaMovement().scale(throwerImpulse());
		return projectileVelocity.add(throwerVelocity);
	}
	float initialVelocity();
	double throwerImpulse();
	float forcedPitch();
}
