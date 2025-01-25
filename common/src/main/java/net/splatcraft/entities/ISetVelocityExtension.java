package net.splatcraft.entities;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public interface ISetVelocityExtension
{
	default void setVelocity(Entity thrower, float pitch, float yaw, float pitchOffset, float speed, float inaccuracy)
	{
		setVelocity(thrower, pitch, yaw, pitchOffset, speed, inaccuracy, 0.8f);
	}
	default void setVelocity(Entity thrower, float pitch, float yaw, float pitchOffset, float speed, float inaccuracy, double throwerImpulse)
	{
		double f = -Math.sin(yaw * MathHelper.RADIANS_PER_DEGREE) * Math.cos(pitch * MathHelper.RADIANS_PER_DEGREE);
		double f1 = -Math.sin((pitch + pitchOffset) * MathHelper.RADIANS_PER_DEGREE);
		double f2 = Math.cos(yaw * MathHelper.RADIANS_PER_DEGREE) * Math.cos(pitch * MathHelper.RADIANS_PER_DEGREE);
		setVelocity(f, f1, f2, speed, inaccuracy);
		
		Vec3d posDiff = new Vec3d(0, 0, 0);
		
		if (thrower != null)
		{
			posDiff = thrower.getMovement();
			if (thrower.isOnGround())
				posDiff.multiply(1, 0, 1);
			posDiff = posDiff.multiply(throwerImpulse);
		}
		
		addVelocity(posDiff);
	}
	default void setVelocity(double x, double y, double z, float speed, float inaccuracy)
	{
		Vec3d shotDirection = calculateShotDirection(x, y, z, inaccuracy);
		onShotDirectionCalculated(shotDirection);
		shotDirection = shotDirection.multiply(speed);
		onVelocityCalculated(shotDirection, speed);
	}
	default void onVelocityCalculated(Vec3d velocity, float speed)
	{
	
	}
	default void setVelocity(float x, float y, float z)
	{
		setVelocity((double) x, y, z);
	}
	default void onShotDirectionCalculated(Vec3d shotDirection)
	{
	
	}
	default Vec3d calculateShotDirection(double x, double y, double z, float inaccuracy)
	{
		float usedInaccuracy = inaccuracy * MathHelper.RADIANS_PER_DEGREE;
		return new Vec3d(x, y, z)
			.rotateY((getRandom().nextFloat() * 2f - 1f) * usedInaccuracy)
			.rotateX((getRandom().nextFloat() * 2f - 1f) * usedInaccuracy * 0.5625f).normalize();
	}
	void setVelocity(double x, double y, double z);
	void setVelocity(Vec3d vec3);
	void addVelocity(Vec3d vec3);
	Random getRandom();
}
