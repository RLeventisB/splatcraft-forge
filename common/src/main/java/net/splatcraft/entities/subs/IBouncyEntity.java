package net.splatcraft.entities.subs;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;

public interface IBouncyEntity
{
	static Optional<Pair<Vec3, Direction>> aabbClipWithDirection(AABB aabb, Vec3 from, Vec3 to)
	{
		double[] distance = new double[] {(double) 1.0F};
		double deltaX = to.x - from.x;
		double deltaY = to.y - from.y;
		double deltaZ = to.z - from.z;
		Direction direction = AABB.getDirection(aabb, from, distance, null, deltaX, deltaY, deltaZ);
		if (direction == null)
		{
			return Optional.empty();
		}
		else
		{
			double d3 = distance[0];
			return Optional.of(Pair.of(from.add(d3 * deltaX, d3 * deltaY, d3 * deltaZ), direction));
		}
	}
	Vec3 collide(Vec3 vec3);
	private Pair<EntityHitResult, Direction> getEntityHit(Entity thisEntity, Vec3 startPos, Vec3 deltaMovement, Predicate<Entity> hitFilter)
	{
		double minDistance = Double.MAX_VALUE;
		Direction dir = null;
		Entity entity = null;
		Vec3 endVec = startPos.add(deltaMovement);
		
		for (Entity entity1 : thisEntity.level().getEntities(thisEntity, thisEntity.getBoundingBox().expandTowards(deltaMovement).inflate(1.0f), hitFilter))
		{
			AABB aabb = entity1.getBoundingBox().inflate(0.5);
			Optional<Pair<Vec3, Direction>> clipData = aabbClipWithDirection(aabb, startPos, endVec);
			if (clipData.isPresent())
			{
				double distance = startPos.distanceToSqr(clipData.get().getFirst());
				if (distance < minDistance)
				{
					entity = entity1;
					dir = clipData.get().getSecond();
					minDistance = distance;
				}
			}
		}
		
		return entity == null ? null : Pair.of(new EntityHitResult(entity), dir);
	}
	default Vec3 onHitEntity(EntityHitResult result, Vec3 velocity, @Nullable Direction hitDirecion)
	{
		return velocity;
	}
	default Direction.Axis[] getReflectionOrder()
	{
		return new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z, Direction.Axis.Y};
	}
	default Pair<Vec3, Vec3> doBounceLogic(Entity entity, Vec3 velocity, Predicate<Entity> hitFilter, boolean bounceOnHit)
	{
		// todo: this behaves weirdly on corners (prioritizing the z axis) but i dont want to recreate the whole collide method
		Vec3 collidedVelocity = collide(velocity);
		
		for (Direction.Axis axis : getReflectionOrder())
		{
			velocity = doBounce(axis, collidedVelocity, velocity);
		}
		
		Pair<EntityHitResult, Direction> hitResult = getEntityHit(entity, entity.position(), velocity, hitFilter);
		if (hitResult != null)
			velocity = onHitEntity(hitResult.getFirst(), velocity, bounceOnHit ? hitResult.getSecond() : null);
		
		return Pair.of(collidedVelocity, velocity);
	}
	default Vec3 doBounce(Direction.Axis axis, Vec3 newVelocity, Vec3 oldVelocity)
	{
		if (didCollisionOnAxis(axis, newVelocity, oldVelocity))
		{
			return reflectVelocity(axis, newVelocity, oldVelocity);
		}
		return oldVelocity;
	}
	default boolean didCollisionOnAxis(Direction.Axis axis, Vec3 newVelocity, Vec3 oldVelocity)
	{
		double newAxisVelocity = axis.choose(newVelocity.x, newVelocity.y, newVelocity.z);
		double oldAxisVelocity = axis.choose(oldVelocity.x, oldVelocity.y, oldVelocity.z);
		return !Mth.equal(newAxisVelocity, oldAxisVelocity);
	}
	default Vec3 reflectVelocity(Direction.Axis axis, Vec3 newVelocity, Vec3 oldVelocity)
	{
		Vec3 reflectedVelocity = oldVelocity.multiply(reflectionCoefficient(oldVelocity));
		
		return oldVelocity.with(axis, axis.choose(reflectedVelocity.x, reflectedVelocity.y, reflectedVelocity.z));
	}
	default Vec3 reflectionCoefficient(Vec3 velocity)
	{
		return new Vec3(
			-1,
			-1,
			-1
		);
	}
}
