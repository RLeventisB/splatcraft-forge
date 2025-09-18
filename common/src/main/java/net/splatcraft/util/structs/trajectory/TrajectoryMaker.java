package net.splatcraft.util.structs.trajectory;

import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicReference;

public interface TrajectoryMaker
{
	boolean process(AtomicReference<Vec3> currentPos, AtomicReference<Vec3> currentVelocity);
}
