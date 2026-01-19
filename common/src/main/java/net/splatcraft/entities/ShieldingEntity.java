package net.splatcraft.entities;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface ShieldingEntity
{
	Vec3 collideWithEntity(Entity collider, Vec3 startPos, Vec3 velocity, Vec3 impactPos);
	float getExplosionHitPercent(Entity target, Vec3 explosionCenter);
}
