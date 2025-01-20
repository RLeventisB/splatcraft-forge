package net.splatcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.splatcraft.entities.InkDropEntity;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.entities.subs.AbstractSubWeaponEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Predicate;

public abstract class ProjectileMixin
{
	@Mixin(ProjectileUtil.class)
	public abstract static class InkProjectileDataMixin
	{
		@Unique
		private static Vec3d splatcraft$hitPos = new Vec3d(0, 0, 0);
		@Inject(method = "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;F)Lnet/minecraft/util/hit/EntityHitResult;", at = @At(value = "INVOKE_ASSIGN", shift = At.Shift.AFTER, target = "Lnet/minecraft/util/math/Vec3d;squaredDistanceTo(Lnet/minecraft/util/math/Vec3d;)D"))
		private static void splatcraft$obtainHitLocation(World world, Entity entity2, Vec3d pStartVec, Vec3d pEndVec, Box pBoundingBox, Predicate<Entity> pFilter, float pInflationAmount, CallbackInfoReturnable<EntityHitResult> cir, @Local(ordinal = 0) double d0, @Local Optional<Vec3d> optional, @Local(ordinal = 1) double d1)
		{
			if (d1 < d0 && (entity2 instanceof InkProjectileEntity || entity2 instanceof InkDropEntity))
			{
				splatcraft$hitPos = optional.get();
			}
		}
		@Inject(method = "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;F)Lnet/minecraft/util/hit/EntityHitResult;", at = @At(value = "RETURN"), cancellable = true)
		private static void splatcraft$addHitLocation(World pLevel, Entity pProjectile, Vec3d pStartVec, Vec3d pEndVec, Box pBoundingBox, Predicate<Entity> pFilter, float pInflationAmount, CallbackInfoReturnable<EntityHitResult> cir, @Local(ordinal = 1) Entity entity)
		{
			if (entity != null && (pProjectile instanceof InkProjectileEntity || pProjectile instanceof InkDropEntity || pProjectile instanceof AbstractSubWeaponEntity<?>))
			{
				cir.setReturnValue(new EntityHitResult(entity, splatcraft$hitPos));
				cir.cancel();
			}
		}
	}
}
