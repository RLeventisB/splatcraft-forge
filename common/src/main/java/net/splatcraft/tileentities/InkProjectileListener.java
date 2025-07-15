package net.splatcraft.tileentities;

import net.minecraft.world.phys.BlockHitResult;
import net.splatcraft.entities.InkProjectileEntity;
import org.jetbrains.annotations.NotNull;

public interface InkProjectileListener
{
	void onCollide(InkProjectileEntity projectile, @NotNull BlockHitResult result);
}
