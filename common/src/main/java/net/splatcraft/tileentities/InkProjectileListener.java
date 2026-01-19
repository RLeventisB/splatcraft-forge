package net.splatcraft.tileentities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.entities.InkProjectileEntity;

public interface InkProjectileListener
{
	void onCollide(InkProjectileEntity projectile, BlockPos collidedPos, Vec3 closestPointInsideBox, Vec3 collisionNormal);
}
