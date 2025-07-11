package net.splatcraft.mixin.accessors;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(AABB.class)
public interface AABBAccessor
{
	@Invoker
	static Direction invokeGetDirection(AABB aabb, Vec3 start, double[] minDistance, @Nullable Direction facing, double deltaX, double deltaY, double deltaZ)
	{
		throw new AssertionError();
	}
}
