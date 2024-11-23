package net.splatcraft.forge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.forge.entities.InkDropEntity;
import net.splatcraft.forge.entities.InkProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;

public abstract class ProjectileMixin
{
    @Mixin(ProjectileUtil.class)
    public abstract static class InkProjectileDataMixin
    {
        @Unique
        private static Vec3 splatcraft$hitPos = new Vec3(0, 0, 0);

        @WrapOperation(method = "getHitResultOnMoveVector", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getHitResult(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/Entity;Ljava/util/function/Predicate;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/phys/HitResult;"))
        private static HitResult splatcraft$applyDeltaTime(Vec3 pStartVec, Entity pProjectile, Predicate<Entity> pFilter, Vec3 pEndVecOffset, Level pLevel, Operation<HitResult> original)
        {
            if (pProjectile instanceof InkProjectileEntity || pProjectile instanceof InkDropEntity)
                return original.call(pStartVec, pProjectile, pFilter, pEndVecOffset.scale(InkProjectileEntity.MixinTimeDelta), pLevel);
            return original.call(pStartVec, pProjectile, pFilter, pEndVecOffset, pLevel);
        }

        @Inject(method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE_ASSIGN", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"))
        private static void splatcraft$addHitLocation(Level pLevel, Entity pProjectile, Vec3 pStartVec, Vec3 pEndVec, AABB pBoundingBox, Predicate<Entity> pFilter, float pInflationAmount, CallbackInfoReturnable<EntityHitResult> cir, double d0, Entity entity, Iterator var10, Entity entity1, AABB aabb, Optional<Vec3> optional, double d1)
        {
            if (d1 < d0 && (pProjectile instanceof InkProjectileEntity || pProjectile instanceof InkDropEntity))
            {
                splatcraft$hitPos = optional.get();
            }
        }

        @Inject(method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "RETURN"), cancellable = true)
        private static void splatcraft$addHitLocation(Level pLevel, Entity pProjectile, Vec3 pStartVec, Vec3 pEndVec, AABB pBoundingBox, Predicate<Entity> pFilter, float pInflationAmount, CallbackInfoReturnable<EntityHitResult> cir, double d0, Entity entity)
        {
            if (entity != null && (pProjectile instanceof InkProjectileEntity || pProjectile instanceof InkDropEntity))
            {
                cir.setReturnValue(new EntityHitResult(entity, splatcraft$hitPos));
                cir.cancel();
            }
        }
    }

    @Mixin(ThrowableProjectile.class)
    public static class InkProjectileMovementMixin
    {
        @Unique
        private boolean splatcraft$isSplatcraftEntity()
        {
            ThrowableProjectile projectile = (ThrowableProjectile) (Object) this;
            return projectile instanceof InkProjectileEntity | projectile instanceof InkDropEntity;
        }

        // ok this is only because i dont like minecraft applying 0.99 to the velocity this is easily removable
        @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"))
        private Vec3 splatcraft$cancelMinimalAcceleration(Vec3 instance, double pFactor, Operation<Vec3> original)
        {
            if (!splatcraft$isSplatcraftEntity() || pFactor != 0.99)
                return original.call(instance, pFactor);
            return instance;
        }

        // if projectiles are updated in parallel with multiple threads we are screwed!!! yay
        @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ThrowableProjectile;setPos(DDD)V"))
        private void splatcraft$cancelMinimalAcceleration(ThrowableProjectile instance, double x, double y, double z, Operation<Void> original)
        {
            if (splatcraft$isSplatcraftEntity())
                original.call(instance, x * InkProjectileEntity.MixinTimeDelta, y * InkProjectileEntity.MixinTimeDelta, z * InkProjectileEntity.MixinTimeDelta);
            original.call(instance, x, y, z);
        }
    }
}
