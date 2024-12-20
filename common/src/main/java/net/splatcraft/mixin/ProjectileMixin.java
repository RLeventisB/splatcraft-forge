package net.splatcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.splatcraft.entities.InkDropEntity;
import net.splatcraft.entities.InkProjectileEntity;
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
        private static Vec3d splatcraft$hitPos = new Vec3d(0, 0, 0);

        @WrapOperation(method = "getCollision(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/entity/Entity;Ljava/util/function/Predicate;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/world/World;FLnet/minecraft/world/RaycastContext$ShapeType;)Lnet/minecraft/util/hit/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;F)Lnet/minecraft/util/hit/EntityHitResult;"))
        private static EntityHitResult splatcraft$applyDeltaTime(World world, Entity entity, Vec3d min, Vec3d max, Box box, Predicate<Entity> predicate, float margin, Operation<EntityHitResult> original)
        {
            Vec3d velocity = max.subtract(min);
            if (entity instanceof InkProjectileEntity || entity instanceof InkDropEntity)
                return original.call(world, entity, min, min.add(velocity.multiply(InkProjectileEntity.MixinTimeDelta)), box, predicate, margin);
            return original.call(world, entity, min, max, box, predicate, margin);
        }

        @Inject(method = "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;F)Lnet/minecraft/util/hit/EntityHitResult;", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE_ASSIGN", shift = At.Shift.AFTER, target = "Lnet/minecraft/util/math/Vec3d;squaredDistanceTo(Lnet/minecraft/util/math/Vec3d;)D"))
        private static void splatcraft$addHitLocation(World world, Entity entity2, Vec3d pStartVec, Vec3d pEndVec, Box pBoundingBox, Predicate<Entity> pFilter, float pInflationAmount, CallbackInfoReturnable<EntityHitResult> cir, double d0, Entity entity, Iterator var10, Entity entity1, Box aabb, Optional<Vec3d> optional, double d1)
        {
            if (d1 < d0 && (entity2 instanceof InkProjectileEntity || entity2 instanceof InkDropEntity))
            {
                splatcraft$hitPos = optional.get();
            }
        }

        @Inject(method = "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;F)Lnet/minecraft/util/hit/EntityHitResult;", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "RETURN"), cancellable = true)
        private static void splatcraft$addHitLocation(World pLevel, Entity pProjectile, Vec3d pStartVec, Vec3d pEndVec, Box pBoundingBox, Predicate<Entity> pFilter, float pInflationAmount, CallbackInfoReturnable<EntityHitResult> cir, double d0, Entity entity)
        {
            if (entity != null && (pProjectile instanceof InkProjectileEntity || pProjectile instanceof InkDropEntity))
            {
                cir.setReturnValue(new EntityHitResult(entity, splatcraft$hitPos));
                cir.cancel();
            }
        }
    }

    @Mixin(ThrownEntity.class)
    public static class InkProjectileMovementMixin
    {
        @Unique
        private boolean splatcraft$isSplatcraftEntity()
        {
            ThrownEntity projectile = (ThrownEntity) (Object) this;
            return projectile instanceof InkProjectileEntity | projectile instanceof InkDropEntity;
        }

        // ok this is only because i dont like minecraft applying 0.99 to the velocity this is easily removable
        @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;multiply(D)Lnet/minecraft/util/math/Vec3d;"))
        private Vec3d splatcraft$cancelMinimalAcceleration(Vec3d instance, double pFactor, Operation<Vec3d> original)
        {
            if (!splatcraft$isSplatcraftEntity() || pFactor != 0.99)
                return original.call(instance, pFactor);
            return instance;
        }

        // if projectiles are updated in parallel with multiple threads we are screwed!!! yay
        @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/thrown/ThrownEntity;setPosition(DDD)V"))
        private void splatcraft$cancelMinimalAcceleration(ThrownEntity instance, double x, double y, double z, Operation<Void> original)
        {
            if (splatcraft$isSplatcraftEntity())
                original.call(instance, x * InkProjectileEntity.MixinTimeDelta, y * InkProjectileEntity.MixinTimeDelta, z * InkProjectileEntity.MixinTimeDelta);
            original.call(instance, x, y, z);
        }
    }
}
