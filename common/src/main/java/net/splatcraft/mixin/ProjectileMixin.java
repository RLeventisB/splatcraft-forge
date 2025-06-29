package net.splatcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
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
		private static Vec3 splatcraft$hitPos = new Vec3(0, 0, 0);
		@Inject(method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;", at = @At(value = "INVOKE_ASSIGN", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"))
		private static void splatcraft$obtainHitLocation(Level world, Entity entity2, Vec3 pStartVec, Vec3 pEndVec, AABB pBoundingBox, Predicate<Entity> pFilter, float pInflationAmount, CallbackInfoReturnable<EntityHitResult> cir, @Local(ordinal = 0) double d0, @Local Optional<Vec3> optional, @Local(ordinal = 1) double d1)
		{
			if (d1 < d0 && splatcraft$isEntityThatRequiresHitpos(entity2))
			{
				splatcraft$hitPos = optional.get();
			}
		}
		@Unique
		private static boolean splatcraft$isEntityThatRequiresHitpos(Entity entity)
		{
			return entity instanceof InkProjectileEntity || entity instanceof InkDropEntity || entity instanceof AbstractSubWeaponEntity<?>;
		}
		@Inject(method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;", at = @At(value = "RETURN"), cancellable = true)
		private static void splatcraft$addHitLocation(Level world, Entity pProjectile, Vec3 pStartVec, Vec3 pEndVec, AABB pBoundingBox, Predicate<Entity> pFilter, float pInflationAmount, CallbackInfoReturnable<EntityHitResult> cir, @Local(ordinal = 1) Entity entity)
		{
			if (entity != null && splatcraft$isEntityThatRequiresHitpos(pProjectile) && splatcraft$hitPos != null)
			{
				cir.setReturnValue(new EntityHitResult(entity, splatcraft$hitPos));
				splatcraft$hitPos = null;
				cir.cancel();
			}
		}
	}
}
