package net.splatcraft.mixin.accessors;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor
{
	@Invoker
	static Vec3 invokeGetInputVector(Vec3 movementInput, float speed, float yaw)
	{
		throw new AssertionError();
	}
	@Invoker
	Vec3 invokeCollide(Vec3 vec);
}
