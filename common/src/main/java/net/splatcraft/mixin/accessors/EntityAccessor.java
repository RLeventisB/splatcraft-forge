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
		// this is here only because when hot swapping, sometimes the mixin stops working and
		// uses the code that is inside this method (previously an AssertionError)
		// however! the access transformer doesnt work on prod!!! because god knows why!!!!
		// so on release, the invoker is used but in debug, when hot reloading, the access transformer is used
		return Entity.getInputVector(movementInput, speed, yaw);
	}
	@Invoker
	Vec3 invokeCollide(Vec3 vec);
}
