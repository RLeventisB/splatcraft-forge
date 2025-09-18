package net.splatcraft.util.structs.trajectory;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public interface TrajectoryProcessor extends StartLauncher, TrajectoryMaker
{
	static TrajectoryProcessor ofFragile(Level level, float initialVelocity, float pitchOffset, float gravity, float margin, double throwerImpulse, Vector3f friction, Predicate<Entity> filter)
	{
		return new FragileTrajectoryProcessor(pitchOffset, initialVelocity, gravity, throwerImpulse, margin, friction, level, filter);
	}
	static TrajectoryProcessor ofFragileIgnoreEntities(BlockGetter level, float initialVelocity, float pitchOffset, float gravity, double throwerImpulse, Vector3f friction)
	{
		return new FragileNoEntityTrajectoryProcessor(pitchOffset, initialVelocity, gravity, throwerImpulse, friction, level);
	}
	static TrajectoryProcessor ofFragileForcedPitch(BlockGetter level, float initialVelocity, float forcedPitch, float gravity, double throwerImpulse, Vector3f friction)
	{
		return new FragileForcedPitchTrajectoryProcessor(forcedPitch, initialVelocity, gravity, throwerImpulse, friction, level);
	}
	static TrajectoryProcessor ofBouncy(BlockGetter level, float initialVelocity, float pitchOffset, float gravity, double throwerImpulse, Vector3f friction, BouncyTrajectoryProcessor.BounceFunction bounceFunction)
	{
		return new BouncyTrajectoryProcessor(pitchOffset, initialVelocity, gravity, throwerImpulse, friction, bounceFunction, level);
	}
	interface FragileTrajectoryBase extends TrajectoryMaker, TrajectoryCollider
	{
		default boolean process(AtomicReference<Vec3> currentPos, AtomicReference<Vec3> currentVelocity)
		{
			Vec3 nextPos = currentPos.get().add(currentVelocity.get());
			currentVelocity.set(currentVelocity.get().add(0, -gravity(), 0).multiply(friction().x, friction().y, friction().z));
			HitResult result = getHitResult(currentPos.get(), nextPos);
			if (result.getType() == HitResult.Type.MISS)
			{
				currentPos.set(nextPos);
				return false;
			}
			currentPos.set(result.getLocation());
			return true;
		}
		HitResult getHitResult(Vec3 currentPos, Vec3 nextPos);
		float gravity();
		Vector3f friction();
	}
	record FragileNoEntityTrajectoryProcessor(float pitchOffset, float initialVelocity, float gravity,
	                                          double throwerImpulse,
	                                          Vector3f friction,
	                                          BlockGetter level) implements TrajectoryProcessor, SimpleStartLauncher, OnlyLevelCollider, FragileTrajectoryBase
	{
		@Override
		public @NotNull HitResult getHitResult(Vec3 currentPos, Vec3 nextPos)
		{
			return OnlyLevelCollider.super.getHitResult(currentPos, nextPos);
		}
	}
	record FragileForcedPitchTrajectoryProcessor(float forcedPitch, float initialVelocity, float gravity,
	                                             double throwerImpulse,
	                                             Vector3f friction,
	                                             BlockGetter level) implements TrajectoryProcessor, ForcedPitchStartLauncher, OnlyLevelCollider, FragileTrajectoryBase
	{
		@Override
		public @NotNull HitResult getHitResult(Vec3 currentPos, Vec3 nextPos)
		{
			return OnlyLevelCollider.super.getHitResult(currentPos, nextPos);
		}
	}
	record FragileTrajectoryProcessor(float pitchOffset, float initialVelocity, float gravity, double throwerImpulse,
	                                  float margin, Vector3f friction,
	                                  Level level,
	                                  Predicate<Entity> filter) implements TrajectoryProcessor, SimpleStartLauncher, LevelEntityCollider, FragileTrajectoryBase
	{
		@Override
		public @NotNull HitResult getHitResult(Vec3 currentPos, Vec3 nextPos)
		{
			return LevelEntityCollider.super.getHitResult(currentPos, nextPos);
		}
	}
	record BouncyTrajectoryProcessor(float pitchOffset, float initialVelocity, float gravity, double throwerImpulse,
	                                 Vector3f friction, BounceFunction bounceFunction,
	                                 BlockGetter level) implements TrajectoryProcessor, SimpleStartLauncher, TrajectoryMaker
	{
		@Override
		public boolean process(AtomicReference<Vec3> currentPos, AtomicReference<Vec3> currentVelocity)
		{
			Vec3 nextPos = currentPos.get().add(currentVelocity.get());
			BlockHitResult result = level.clip(new ClipContext(currentPos.get(), nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
			if (result.getType() == HitResult.Type.MISS)
			{
				currentVelocity.set(currentVelocity.get().add(0, -gravity, 0).multiply(friction.x, friction.y, friction.z));
				currentPos.set(nextPos);
				return false;
			}
			
			Vec3 oldVelocity = currentVelocity.get();
			Vec3 collidedVelocity = result.getLocation().subtract(currentPos.get());
			Vec3 newVelocity = collidedVelocity;
			if (collidedVelocity.lengthSqr() < 0.01)
			{
				currentPos.set(result.getLocation());
				return true;
			}
			
			for (Direction.Axis axis : Direction.Axis.VALUES)
			{
				double newAxisVelocity = axis.choose(collidedVelocity.x, collidedVelocity.y, collidedVelocity.z);
				double oldAxisVelocity = axis.choose(oldVelocity.x, oldVelocity.y, oldVelocity.z);
				if (Mth.equal(newAxisVelocity, oldAxisVelocity))
					continue;
				
				newVelocity = bounceFunction.reflectVelocity(axis, collidedVelocity, oldVelocity);
			}
			currentVelocity.set(newVelocity);
			currentVelocity.set(currentVelocity.get().add(0, -gravity, 0).multiply(friction.x, friction.y, friction.z));
			return false;
		}
		@FunctionalInterface
		public interface BounceFunction
		{
			Vec3 reflectVelocity(Direction.Axis axis, Vec3 newVelocity, Vec3 oldVelocity);
		}
	}
}
