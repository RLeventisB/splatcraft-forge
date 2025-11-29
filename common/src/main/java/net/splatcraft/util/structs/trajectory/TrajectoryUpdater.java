package net.splatcraft.util.structs.trajectory;

import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicReference;

public interface TrajectoryUpdater
{
	boolean process(AtomicReference<Vec3> currentPos, AtomicReference<Vec3> currentVelocity);
}
