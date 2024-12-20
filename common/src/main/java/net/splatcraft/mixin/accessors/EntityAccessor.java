package net.splatcraft.mixin.accessors;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor
{
    @Invoker
    static Vec3d invokeMovementInputToVelocity(Vec3d pRelative, float pMotionScaler, float pFacing)
    {
        throw new AssertionError();
    }
}
