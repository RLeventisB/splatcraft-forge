package net.splatcraft.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.blocks.ColoredBarrierBlock;
import net.splatcraft.blocks.StageBarrierBlock;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.client.particles.InkSplashParticleData;
import net.splatcraft.registries.SplatcraftEntities;
import net.splatcraft.util.*;
import net.splatcraft.util.structs.AttackId;
import net.splatcraft.util.structs.InkColor;
import net.splatcraft.util.structs.RangedValueCollection;
import org.jetbrains.annotations.NotNull;

public class InkDropEntity extends ThrowableProjectile implements IColoredEntity
{
	public static final float DROP_SIZE = 0.7f;
	private static final EntityDataAccessor<InkColor> DROP_COLOR = SynchedEntityData.defineId(InkDropEntity.class, CommonUtils.INKCOLOR_DATA_HANDLER);
	private static final EntityDataAccessor<Float> IMPACT_SIZE = SynchedEntityData.defineId(InkDropEntity.class, EntityDataSerializers.FLOAT);
	public float lifespan = 600;
	public InkBlockUtils.InkType inkType;
	private float timeDelta;
	private RangedValueCollection explosionData;
	private ItemStack sourceWeapon;
	private AttackId attackId;
	public InkDropEntity(EntityType<InkDropEntity> type, Level world)
	{
		super(type, world);
	}
	public InkDropEntity(Level world, Vec3 pos, Entity owner, InkColor color, InkBlockUtils.InkType inkType, float splashSize, ItemStack sourceWeapon)
	{
		super(SplatcraftEntities.INK_DROP.get(), world);
		setPos(pos);
		setOwner(owner);
		setColor(color);
		setImpactCoverage(splashSize);
		this.inkType = inkType;
		this.sourceWeapon = sourceWeapon;
	}
	public InkDropEntity(Level world, InkProjectileEntity projectile, InkColor color, InkBlockUtils.InkType inkType, float splashSize, ItemStack sourceWeapon)
	{
		this(world, projectile.position(), projectile.getOwner(), color, inkType, splashSize, sourceWeapon);
	}
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(DROP_COLOR, ColorUtils.getDefaultColor());
		builder.define(IMPACT_SIZE, 0f);
	}
	@Override
	public void tick()
	{
		tick(1f);
	}
	public void tick(float timeDelta)
	{
		this.timeDelta = timeDelta;

		Vec3 vel = getDeltaMovement();

		if (isInLiquid() || Double.isNaN(vel.x) || Double.isNaN(vel.y) || Double.isNaN(vel.z))
		{
			discard();
			return;
		}

		if (!level().isClientSide && (lifespan -= timeDelta) <= 0)
		{
			discard();
			return;
		}

		setDeltaMovement(vel.multiply(0.9, 1, 0.9));
		super.tick();

		this.timeDelta = 1;
	}
	@Override
	public @NotNull Vec3 getDeltaMovement()
	{
		return super.getDeltaMovement().scale(timeDelta);
	}
	@Override
	protected void applyGravity()
	{
		double d = getGravity() * timeDelta;
		if (d != 0.0)
		{
			setDeltaMovement(getDeltaMovement().add(0.0, -d, 0.0));
		}
	}
	@Override
	public void updateRotation()
	{
		Vec3 motion = getDeltaMovement();

		if (!Vec3.ZERO.equals(motion))
		{
			float pitch = (float) (Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG);
			float yaw = (float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG);
			if (tickCount == 1)
			{
				xRotO = pitch;
				yRotO = yaw;
			}
			setXRot(pitch);
			setYRot(yaw);
		}
	}
	@Override
	protected void onHitBlock(BlockHitResult result)
	{
		if (InkBlockUtils.canInkPassthrough(level(), result.getBlockPos()) || result.isInside())
			return;

		if (level().getBlockState(result.getBlockPos()).getBlock() instanceof ColoredBarrierBlock coloredBarrierBlock &&
			coloredBarrierBlock.canAllowThrough(result.getBlockPos(), this))
			return;

		super.onHitBlock(result);

		if (!level().isClientSide())
		{
			Vec3 pos = InkExplosion.adjustPosition(result.getLocation(), result.getDirection(), this);
			if (sourceWeapon == null || sourceWeapon.isEmpty())
			{
				InkExplosion.createInkExplosion(getOwner(), pos, getImpactCoverage(), inkType, ItemStack.EMPTY);
			}
			else if (explosionData == null)
			{
				InkExplosion.createInkExplosion(getOwner(), pos, getImpactCoverage(), inkType, sourceWeapon);
			}
			else
			{
				InkExplosion.createInkExplosion(getOwner(), pos, getImpactCoverage(), explosionData, inkType, sourceWeapon, attackId);
			}
			discard();
		}
		else
		{
			Vec3 particlePos = result.getLocation().relative(result.getDirection(), 0.4f);
			if (level().getBlockState(result.getBlockPos()).getBlock() instanceof StageBarrierBlock)
				level().addParticle(new InkExplosionParticleData(getColor(), .5f), getX(), getY(), getZ(), 0, 0, 0);
			else
				level().addParticle(new InkSplashParticleData(getColor(), getImpactCoverage()), particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
		}
	}
	@Override
	public void shoot(double x, double y, double z, float velocity, float inaccuracy)
	{
		Vec3 vec3 = (new Vec3(x, y, z)).normalize().scale(velocity).add(random.nextGaussian() * 0.0075 * inaccuracy, random.nextGaussian() * 0.0075D * inaccuracy, random.nextGaussian() * 0.0075 * inaccuracy);

		setDeltaMovement(vec3);
		setYRot((float) (Mth.atan2(vec3.x, vec3.z) * Mth.RAD_TO_DEG));
		setXRot((float) (Mth.atan2(vec3.y, vec3.horizontalDistance()) * Mth.RAD_TO_DEG));
		yRotO = getYRot();
		xRotO = getXRot();
	}
	@Override
	public void onHit(@NotNull HitResult result)
	{
		if (result instanceof EntityHitResult entityHitResult)
		{
			onHitEntity(entityHitResult);
		}
		else if (result instanceof BlockHitResult blockHitResult)
		{
			onHitBlock(blockHitResult);
		}
	}
	@Override
	protected void onHitEntity(@NotNull EntityHitResult result)
	{
		if (level().isClientSide)
		{
			result.getEntity().getBoundingBox().clip(position(), position().add(getDeltaMovement())).ifPresent(particlePos ->
				level().addParticle(new InkSplashParticleData(getColor(), getImpactCoverage()), particlePos.x, particlePos.y, particlePos.z, 0, 0, 0)
			);
		}
		else
		{
			discard();
		}
	}
	@Override
	public boolean canHitEntity(@NotNull Entity entity)
	{
		return entity instanceof SpawnShieldEntity && InkDamageUtils.canDamageColor(level(), entity.blockPosition(), ColorUtils.getEntityColor(entity), getColor());
	}
	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag nbt)
	{
		super.readAdditionalSaveData(nbt);

		Vec3.CODEC.parse(NbtOps.INSTANCE, nbt.get("DeltaMotion")).result().ifPresent(this::setDeltaMovement);

		setImpactCoverage(nbt.getFloat("ImpactCoverage"));

		setColor(InkColor.getFromNbt(nbt.get("DropColor")));

		if (nbt.contains("Lifespan"))
			lifespan = nbt.getFloat("Lifespan");

		if (nbt.contains("SourceWeapon"))
			sourceWeapon = ItemStack.parseOptional(level().registryAccess(), nbt.getCompound("SourceWeapon"));

		if (nbt.contains("CollisionData"))
			RangedValueCollection.DAMAGE_CODEC.parse(NbtOps.INSTANCE, nbt.getCompound("CollisionData")).ifSuccess(data -> explosionData = data);

		if (nbt.contains("AttackId"))
			attackId = AttackId.parseAttackId(NbtOps.INSTANCE, nbt.getCompound("AttackId"));

		inkType = InkBlockUtils.InkType.CODEC.parse(NbtOps.INSTANCE, nbt.get("InkType")).result().orElse(InkBlockUtils.InkType.NORMAL);
	}
	@Override
	public void addAdditionalSaveData(CompoundTag nbt)
	{
		Vec3.CODEC.encodeStart(NbtOps.INSTANCE, getDeltaMovement()).result().ifPresent(tag -> nbt.put("DeltaMotion", tag));

		nbt.putFloat("ImpactCoverage", getImpactCoverage());
		nbt.put("DropColor", getColor().getNbt());

		nbt.putFloat("Lifespan", lifespan);

		nbt.putBoolean("Invisible", isInvisible());

		if (inkType != null)
			nbt.putString("InkType", inkType.name());

		if (sourceWeapon != null && !sourceWeapon.isEmpty())
			nbt.put("SourceWeapon", sourceWeapon.save(level().registryAccess()));

		if (explosionData != null)
			RangedValueCollection.DAMAGE_CODEC.encodeStart(NbtOps.INSTANCE, explosionData).ifSuccess(tag -> nbt.put("CollisionData", tag));

		if (attackId != null)
			nbt.put("AttackId", AttackId.encodeAttackId(NbtOps.INSTANCE, attackId));

		super.addAdditionalSaveData(nbt);
	}
	@Override
	public @NotNull EntityDimensions getDimensions(@NotNull Pose getMatrices)
	{
		return super.getDimensions(getMatrices);
	}
	@Override
	protected double getDefaultGravity()
	{
		return 0.125;
	}
	@Override
	public InkColor getColor()
	{
		return entityData.get(DROP_COLOR);
	}
	@Override
	public void setColor(InkColor color)
	{
		entityData.set(DROP_COLOR, color);
	}
	public float getImpactCoverage()
	{
		return entityData.get(IMPACT_SIZE);
	}
	public void setImpactCoverage(float splashSize)
	{
		entityData.set(IMPACT_SIZE, splashSize);
	}
	public void setExplosionData(RangedValueCollection damageRanges, AttackId dropletAttackId)
	{
		explosionData = damageRanges;
		attackId = dropletAttackId;
	}
}
