package net.splatcraft.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
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
import org.jetbrains.annotations.NotNull;

public class InkDropEntity extends ThrowableProjectile implements IColoredEntity
{
	public static final float DROP_SIZE = 0.7f;
	private static final EntityDataAccessor<InkColor> DROP_COLOR = SynchedEntityData.defineId(InkDropEntity.class, CommonUtils.INKCOLOR_DATA_HANDLER);
	private static final EntityDataAccessor<Float> IMPACT_SIZE = SynchedEntityData.defineId(InkDropEntity.class, EntityDataSerializers.FLOAT);
	public float lifespan = 600;
	public InkBlockUtils.InkType inkType;
	private float timeDelta;
	public InkDropEntity(EntityType<InkDropEntity> type, Level world)
	{
		super(type, world);
	}
	public InkDropEntity(Level world, Vec3 pos, Entity owner, InkColor color, InkBlockUtils.InkType inkType, float splashSize)
	{
		super(SplatcraftEntities.INK_DROP.get(), world);
		setPos(pos);
		setOwner(owner);
		setColor(color);
		setImpactCoverage(splashSize);
		this.inkType = inkType;
	}
	public InkDropEntity(Level world, InkProjectileEntity projectile, InkColor color, InkBlockUtils.InkType inkType, float splashSize)
	{
		this(world, projectile.position(), projectile.getOwner(), color, inkType, splashSize);
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
		
		this.timeDelta = timeDelta;
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
				setXRot(pitch);
				setYRot(yaw);
				xRotO = pitch;
				yRotO = yaw;
			}
			else
			{
				setXRot(lerpRotation(xRotO, pitch));
				setYRot(lerpRotation(yRotO, yaw));
			}
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
			InkExplosion.createInkExplosion(getOwner(), InkExplosion.adjustPosition(result.getLocation(), result.getDirection(), this), getImpactCoverage(), inkType, ItemStack.EMPTY);
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
		
		ListTag directionTag = nbt.getList("DeltaMotion", DoubleTag.TAG_DOUBLE);
		setDeltaMovement(new Vec3(directionTag.getDouble(0), directionTag.getDouble(1), directionTag.getDouble(2)));
		
		setImpactCoverage(nbt.getFloat("ImpactCoverage"));
		
		setColor(InkColor.getFromNbt(nbt.get("DropColor")));
		
		if (nbt.contains("Lifespan"))
			lifespan = nbt.getFloat("Lifespan");
		
		setInvisible(nbt.getBoolean("Invisible"));
		
		inkType = InkBlockUtils.InkType.IDENTIFIER_MAP.getOrDefault(ResourceLocation.parse(nbt.getString("InkType")), InkBlockUtils.InkType.NORMAL);
	}
	@Override
	public void addAdditionalSaveData(CompoundTag nbt)
	{
		ListTag directionTag = new ListTag();
		Vec3 direction = getDeltaMovement();
		directionTag.add(DoubleTag.valueOf(direction.x));
		directionTag.add(DoubleTag.valueOf(direction.y));
		directionTag.add(DoubleTag.valueOf(direction.z));
		nbt.put("DeltaMotion", directionTag);
		
		nbt.putFloat("ImpactCoverage", getImpactCoverage());
		nbt.put("DropColor", getColor().getNbt());
		
		nbt.putFloat("Lifespan", lifespan);
		
		nbt.putBoolean("Invisible", isInvisible());
		
		nbt.putString("InkType", inkType.getIdString());
		
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
}
