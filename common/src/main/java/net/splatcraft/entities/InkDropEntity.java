package net.splatcraft.entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
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
	public static final float DROP_SIZE = 1f;
	private static final TrackedData<InkColor> DROP_COLOR = DataTracker.registerData(InkDropEntity.class, CommonUtils.INKCOLORDATAHANDLER);
	private static final byte SHIELD_DENY = -1;
	private static final byte BLOCK_COLLIDE = 1;
	public int lifespan = 600;
	public float impactCoverage;
	public InkBlockUtils.InkType inkType;
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
		impactCoverage = splashSize;
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
		
		if (!getWorld().isClient && lifespan-- <= 0)
		{
			discard();
			return;
		}
		
		setVelocity(vel.multiply(timeDelta));
		super.tick();
		setVelocity(vel);
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
	public void handleStatus(byte id)
	{
		super.handleStatus(id);
		
		switch (id)
		{
			case SHIELD_DENY ->
				getWorld().addParticle(new InkExplosionParticleData(getColor(), .5f), getX(), getY(), getZ(), 0, 0, 0);
			case BLOCK_COLLIDE ->
				getWorld().addParticle(new InkSplashParticleData(getColor(), 0.0225f), getX(), getY(), getZ(), 0, 0, 0);
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
		
		InkExplosion.createInkExplosion(getOwner(), InkExplosion.adjustPosition(result.getPos(), result.getSide().getUnitVector()), impactCoverage, 0, 0, inkType, ItemStack.EMPTY);
		if (getWorld().getBlockState(result.getBlockPos()).getBlock() instanceof StageBarrierBlock)
			getWorld().sendEntityStatus(this, SHIELD_DENY);
		else
			getWorld().sendEntityStatus(this, BLOCK_COLLIDE);
		if (!getWorld().isClient)
		{
			discard();
		}
	}
	@Override
	public void setVelocity(@NotNull Entity thrower, float pitch, float yaw, float pitchOffset, float velocity, float inaccuracy)
	{
		super.setVelocity(thrower, pitch, yaw, pitchOffset, velocity, inaccuracy);
		InkExplosion.createInkExplosion(getOwner(), thrower.getPos(), 0.75f, 0, 0, inkType, ItemStack.EMPTY);
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
	public void readCustomDataFromNbt(@NotNull NbtCompound nbt)
	{
		super.readCustomDataFromNbt(nbt);
		
		NbtList directionTag = nbt.getList("DeltaMotion", NbtDouble.DOUBLE_TYPE);
		setVelocity(new Vec3d(directionTag.getDouble(0), directionTag.getDouble(1), directionTag.getDouble(2)));
		
		impactCoverage = nbt.getFloat("ImpactCoverage");
		
		setColor(InkColor.getFromNbt(nbt.get("DropColor")));
		
		if (nbt.contains("Lifespan"))
			lifespan = nbt.getInt("Lifespan");
		
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
		
		nbt.putFloat("ImpactCoverage", impactCoverage);
		nbt.put("DropColor", getColor().getNbt());
		
		nbt.putInt("Lifespan", lifespan);
		
		nbt.putBoolean("Invisible", isInvisible());
		
		nbt.putString("InkType", inkType.getSerializedName());
		
		super.writeCustomDataToNbt(nbt);
	}
	@Override
	public @NotNull EntityDimensions getDimensions(@NotNull EntityPose getMatrices)
	{
		return super.getDimensions(getMatrices);
	}
	public float getProjectileSize()
	{
		return DROP_SIZE;
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
}
