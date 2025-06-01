package net.splatcraft.entities;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface ISetVelocityExtension
{
	default void setDeltaMovement(Entity thrower, float pitch, float yaw, float pitchOffset, float speed, float inaccuracy)
	{
		setDeltaMovement(thrower, pitch, yaw, pitchOffset, speed, inaccuracy, 0.8f);
	}
	default void setDeltaMovement(Entity thrower, float pitch, float yaw, float pitchOffset, float speed, float inaccuracy, double throwerImpulse)
	{
		pitch += pitchOffset;
		pitch *= Mth.DEG_TO_RAD;
		yaw *= Mth.DEG_TO_RAD;
		double f = -Math.sin(yaw) * Math.cos(pitch);
		double f1 = -Math.sin((pitch));
		double f2 = Math.cos(yaw) * Math.cos(pitch);
		setDeltaMovement(f, f1, f2, speed, inaccuracy);

		if (throwerImpulse == 0)
			return;

		Vec3 posDiff = new Vec3(0, 0, 0);

		if (thrower != null)
		{
			posDiff = thrower.getKnownMovement();
			if (thrower.onGround())
				posDiff.multiply(1, 0, 1);
			posDiff = posDiff.scale(throwerImpulse);
		}

		addDeltaMovement(posDiff);
	}
	default void setDeltaMovement(double x, double y, double z, float speed, float inaccuracy)
	{
		Vec3 shotDirection = calculateShotDirection(x, y, z, inaccuracy);
		onShotDirectionCalculated(shotDirection);
		shotDirection = shotDirection.scale(speed);
		onVelocityCalculated(shotDirection, speed);
	}
	default void onVelocityCalculated(Vec3 velocity, float speed)
	{

	}
	default void setDeltaMovement(float x, float y, float z)
	{
		setDeltaMovement((double) x, y, z);
	}
	default void onShotDirectionCalculated(Vec3 shotDirection)
	{

	}
	default Vec3 calculateShotDirection(double x, double y, double z, float inaccuracy)
	{
		float usedInaccuracy = inaccuracy * Mth.DEG_TO_RAD;
		return new Vec3(x, y, z)
			.yRot((getRandom().nextFloat() * 2f - 1f) * usedInaccuracy)
			.xRot((getRandom().nextFloat() * 2f - 1f) * usedInaccuracy * 0.5625f).normalize();
	}
	void setDeltaMovement(double x, double y, double z);
	void setDeltaMovement(Vec3 vec3);
	void addDeltaMovement(Vec3 vec3);
	RandomSource getRandom();
}
