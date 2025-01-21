package net.splatcraft.entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.splatcraft.blocks.ColoredBarrierBlock;
import net.splatcraft.blocks.StageBarrierBlock;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.client.particles.InkSplashParticleData;
import net.splatcraft.registries.SplatcraftEntities;
import net.splatcraft.util.*;
import org.jetbrains.annotations.NotNull;

public class InkDropEntity extends ThrownEntity implements IColoredEntity
{
	public static final float DROP_SIZE = 0.7f;
	private static final TrackedData<InkColor> DROP_COLOR = DataTracker.registerData(InkDropEntity.class, CommonUtils.INKCOLORDATAHANDLER);
	private static final TrackedData<Float> IMPACT_SIZE = DataTracker.registerData(InkDropEntity.class, TrackedDataHandlerRegistry.FLOAT);
	public float lifespan = 600;
	public InkBlockUtils.InkType inkType;
	private float timeDelta;
	public InkDropEntity(EntityType<InkDropEntity> type, World world)
	{
		super(type, world);
	}
	public InkDropEntity(World world, Vec3d pos, Entity owner, InkColor color, InkBlockUtils.InkType inkType, float splashSize)
	{
		super(SplatcraftEntities.INK_DROP.get(), world);
		setPosition(pos);
		setOwner(owner);
		setColor(color);
		setImpactCoverage(splashSize);
		this.inkType = inkType;
	}
	public InkDropEntity(World world, InkProjectileEntity projectile, InkColor color, InkBlockUtils.InkType inkType, float splashSize)
	{
		this(world, projectile.getPos(), projectile.getOwner(), color, inkType, splashSize);
	}
	@Override
	protected void initDataTracker(DataTracker.Builder builder)
	{
		builder.add(DROP_COLOR, ColorUtils.getDefaultColor());
		builder.add(IMPACT_SIZE, 0f);
	}
	@Override
	public void tick()
	{
		tick(1f);
	}
	public void tick(float timeDelta)
	{
		Vec3d vel = getVelocity();
		
		if (isInFluid() || Double.isNaN(vel.x) || Double.isNaN(vel.y) || Double.isNaN(vel.z))
		{
			discard();
			return;
		}
		
		if (!getWorld().isClient && (lifespan -= timeDelta) <= 0)
		{
			discard();
			return;
		}
		
		this.timeDelta = timeDelta;
		super.tick();
		this.timeDelta = 1;
	}
	@Override
	public Vec3d getVelocity()
	{
		return super.getVelocity().multiply(timeDelta);
	}
	@Override
	protected void applyGravity()
	{
		double d = getFinalGravity() * timeDelta;
		if (d != 0.0)
		{
			setVelocity(getVelocity().add(0.0, -d, 0.0));
		}
	}
	@Override
	public void updateRotation()
	{
		Vec3d motion = getVelocity();
		
		if (!Vec3d.ZERO.equals(motion))
		{
			setYaw((float) (MathHelper.atan2(motion.y, motion.horizontalLength()) * MathHelper.DEGREES_PER_RADIAN));
			setPitch((float) (MathHelper.atan2(motion.x, motion.z) * MathHelper.DEGREES_PER_RADIAN));
		}
	}
	@Override
	protected void onBlockHit(BlockHitResult result)
	{
		if (InkBlockUtils.canInkPassthrough(getWorld(), result.getBlockPos()) || result.isInsideBlock())
			return;
		
		if (getWorld().getBlockState(result.getBlockPos()).getBlock() instanceof ColoredBarrierBlock coloredBarrierBlock &&
			coloredBarrierBlock.canAllowThrough(result.getBlockPos(), this))
			return;
		
		super.onBlockHit(result);
		
		if (!getWorld().isClient())
		{
			InkExplosion.createInkExplosion(getOwner(), InkExplosion.adjustPosition(result.getPos(), result.getSide(), this), getImpactCoverage(), inkType, ItemStack.EMPTY);
			discard();
		}
		else
		{
			// todo: clientworld doenst reach here for some reason :(
			Vec3d particlePos = result.getPos().offset(result.getSide(), 0.4f);
			if (getWorld().getBlockState(result.getBlockPos()).getBlock() instanceof StageBarrierBlock)
				getWorld().addParticle(new InkExplosionParticleData(getColor(), .5f), getX(), getY(), getZ(), 0, 0, 0);
			else
				getWorld().addParticle(new InkSplashParticleData(getColor(), getImpactCoverage()), particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
		}
	}
	@Override
	public void setVelocity(@NotNull Entity thrower, float pitch, float yaw, float pitchOffset, float velocity, float inaccuracy)
	{
		super.setVelocity(thrower, pitch, yaw, pitchOffset, velocity, inaccuracy);
		InkExplosion.createInkExplosion(getOwner(), thrower.getPos(), 0.75f, inkType, ItemStack.EMPTY);
	}
	@Override
	public void setVelocity(double x, double y, double z, float velocity, float inaccuracy)
	{
		Vec3d vec3 = (new Vec3d(x, y, z)).normalize().multiply(velocity).add(random.nextGaussian() * 0.0075 * inaccuracy, random.nextGaussian() * 0.0075D * inaccuracy, random.nextGaussian() * 0.0075 * inaccuracy);
		
		setVelocity(vec3);
		double d0 = vec3.horizontalLength();
		setYaw((float) (MathHelper.atan2(vec3.x, vec3.z) * MathHelper.DEGREES_PER_RADIAN));
		setPitch((float) (MathHelper.atan2(vec3.y, d0) * MathHelper.DEGREES_PER_RADIAN));
	}
	@Override
	public void onCollision(@NotNull HitResult result)
	{
		if (result instanceof EntityHitResult entityHitResult)
		{
			onEntityHit(entityHitResult);
		}
		else if (result instanceof BlockHitResult blockHitResult)
		{
			onBlockHit(blockHitResult);
		}
	}
	@Override
	protected void onEntityHit(EntityHitResult result)
	{
		if (getWorld().isClient)
		{
			result.getEntity().getBoundingBox().raycast(getPos(), getPos().add(getVelocity())).ifPresent(particlePos ->
				getWorld().addParticle(new InkSplashParticleData(getColor(), getImpactCoverage()), particlePos.x, particlePos.y, particlePos.z, 0, 0, 0)
			);
		}
		else
		{
			discard();
		}
	}
	@Override
	public boolean canHit(Entity entity)
	{
		return entity instanceof SpawnShieldEntity && InkDamageUtils.canDamageColor(getWorld(), entity.getBlockPos(), ColorUtils.getEntityColor(entity), getColor());
	}
	@Override
	public void readCustomDataFromNbt(@NotNull NbtCompound nbt)
	{
		super.readCustomDataFromNbt(nbt);
		
		NbtList directionTag = nbt.getList("DeltaMotion", NbtDouble.DOUBLE_TYPE);
		setVelocity(new Vec3d(directionTag.getDouble(0), directionTag.getDouble(1), directionTag.getDouble(2)));
		
		setImpactCoverage(nbt.getFloat("ImpactCoverage"));
		
		setColor(InkColor.getFromNbt(nbt.get("DropColor")));
		
		if (nbt.contains("Lifespan"))
			lifespan = nbt.getFloat("Lifespan");
		
		setInvisible(nbt.getBoolean("Invisible"));
		
		inkType = InkBlockUtils.InkType.IDENTIFIER_MAP.getOrDefault(Identifier.of(nbt.getString("InkType")), InkBlockUtils.InkType.NORMAL);
	}
	@Override
	public void writeCustomDataToNbt(NbtCompound nbt)
	{
		NbtList directionTag = new NbtList();
		Vec3d direction = getVelocity();
		directionTag.add(NbtDouble.of(direction.x));
		directionTag.add(NbtDouble.of(direction.y));
		directionTag.add(NbtDouble.of(direction.z));
		nbt.put("DeltaMotion", directionTag);
		
		nbt.putFloat("ImpactCoverage", getImpactCoverage());
		nbt.put("DropColor", getColor().getNbt());
		
		nbt.putFloat("Lifespan", lifespan);
		
		nbt.putBoolean("Invisible", isInvisible());
		
		nbt.putString("InkType", inkType.getSerializedName());
		
		super.writeCustomDataToNbt(nbt);
	}
	@Override
	public @NotNull EntityDimensions getDimensions(@NotNull EntityPose getMatrices)
	{
		return super.getDimensions(getMatrices);
	}
	@Override
	protected double getGravity()
	{
		return 0.125;
	}
	@Override
	public InkColor getColor()
	{
		return dataTracker.get(DROP_COLOR);
	}
	@Override
	public void setColor(InkColor color)
	{
		dataTracker.set(DROP_COLOR, color);
	}
	public float getImpactCoverage()
	{
		return dataTracker.get(IMPACT_SIZE);
	}
	public void setImpactCoverage(float splashSize)
	{
		dataTracker.set(IMPACT_SIZE, splashSize);
	}
}
