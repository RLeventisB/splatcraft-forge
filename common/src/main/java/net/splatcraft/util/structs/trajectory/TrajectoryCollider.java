package net.splatcraft.util.structs.trajectory;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Predicate;

public interface TrajectoryCollider
{
	HitResult getHitResult(Vec3 currentPos, Vec3 nextPos);
}
interface OnlyLevelCollider extends TrajectoryCollider
{
	@NotNull
	default HitResult getHitResult(Vec3 currentPos, Vec3 nextPos)
	{
		return level().clip(new ClipContext(currentPos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
	}
	BlockGetter level();
}
interface LevelEntityCollider extends TrajectoryCollider
{
	@NotNull
	default HitResult getHitResult(Vec3 currentPos, Vec3 nextPos)
	{
		AABB aabb = AABB.ofSize(currentPos, margin(), margin(), margin()).expandTowards(nextPos.subtract(currentPos)).inflate(1.0);
		Optional<Vec3> collisionPoint = Optional.empty();
		double minDist = Double.POSITIVE_INFINITY;
		for (LivingEntity entity : level().getEntities(EntityTypeTest.forClass(LivingEntity.class), aabb, filter()))
		{
			AABB entityAabb = entity.getBoundingBox().inflate(0.3f);
			Optional<Vec3> clip = entityAabb.clip(currentPos, nextPos);
			if (clip.isPresent())
			{
				double distance = clip.get().distanceTo(currentPos);
				if (!(distance < minDist))
					continue;
				
				collisionPoint = clip;
				minDist = distance;
			}
		}
		
		if (collisionPoint.isPresent())
			return new EntityHitResult(null, collisionPoint.get());
		
		return level().clip(new ClipContext(currentPos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
	}
	Predicate<Entity> filter();
	float margin();
	Level level();
}

