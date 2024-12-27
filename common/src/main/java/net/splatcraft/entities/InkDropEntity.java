package net.splatcraft.entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
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
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.util.*;
import org.jetbrains.annotations.NotNull;

public class InkDropEntity extends ThrownItemEntity implements IColoredEntity
{
	private static final TrackedData<InkColor> DROP_COLOR = DataTracker.registerData(InkDropEntity.class, CommonUtils.INKCOLORDATAHANDLER);
	private static final TrackedData<Float> DROP_SIZE = DataTracker.registerData(InkDropEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> GRAVITY = DataTracker.registerData(InkDropEntity.class, TrackedDataHandlerRegistry.FLOAT);
	public int lifespan = 600;
	public ItemStack sourceWeapon = ItemStack.EMPTY;
	public float impactCoverage;
	public InkBlockUtils.InkType inkType;
	public InkDropEntity(EntityType<InkDropEntity> type, World world)
	{
		super(type, world);
	}
	public InkDropEntity(World world, InkProjectileEntity projectile, InkColor color, InkBlockUtils.InkType inkType, float splatSize, ItemStack sourceWeapon)
	{
		super(SplatcraftEntities.INK_DROP.get(), world);
		setPos(projectile.getX(), projectile.getY(), projectile.getZ());
		setOwner(projectile.getOwner());
		setColor(color);
		setProjectileSize(0.045f);
		impactCoverage = splatSize;
		this.inkType = inkType;
		this.sourceWeapon = sourceWeapon;
	}
	@Override
	protected void initDataTracker(DataTracker.Builder builder)
	{
		super.initDataTracker(builder);
		builder.add(DROP_COLOR, ColorUtils.getDefaultColor());
		builder.add(DROP_SIZE, 0.045f);
		builder.add(GRAVITY, 0.275f);
	}
	public void onTrackedDataSet(TrackedData<?> data)
	{
		if (DROP_SIZE.equals(data))
			calculateDimensions();
		
		super.onTrackedDataSet(data);
	}
	@Override
	protected @NotNull Item getDefaultItem()
	{
		return SplatcraftItems.splattershot.get();
	}
	@Override
	public void tick()
	{
		tick(SplatcraftGameRules.getIntRuleValue(getWorld(), SplatcraftGameRules.INK_PROJECTILE_FREQUENCY) / 100f);
	}
	public void tick(float timeDelta)
	{
		Vec3d vel = getVelocity();
		InkProjectileEntity.MixinTimeDelta = timeDelta;
		super.tick();
		
		if (isInFluid() || Double.isNaN(vel.x) || Double.isNaN(vel.y) || Double.isNaN(vel.z))
		{
			discard();
			return;
		}
		
		if (isRemoved())
			return;
		setVelocity(vel.subtract(0, getGravity(), 0).multiply(Math.pow(0.9, timeDelta)));
		
		if (!getWorld().isClient && lifespan-- <= 0)
		{
			discard();
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
	public void handleStatus(byte id)
	{
		super.handleStatus(id);
		
		switch (id)
		{
			case -1 ->
				getWorld().addParticle(new InkExplosionParticleData(getColor(), .5f), getX(), getY(), getZ(), 0, 0, 0);
			case 1 ->
				getWorld().addParticle(new InkSplashParticleData(getColor(), getProjectileSize() * 0.5f), getX(), getY(), getZ(), 0, 0, 0);
		}
	}
	@Override
	protected void onBlockHit(BlockHitResult result)
	{
		if (InkBlockUtils.canInkPassthrough(getWorld(), result.getBlockPos()))
			return;
		
		if (getWorld().getBlockState(result.getBlockPos()).getBlock() instanceof ColoredBarrierBlock coloredBarrierBlock &&
			coloredBarrierBlock.canAllowThrough(result.getBlockPos(), this))
			return;
		
		super.onBlockHit(result);
		
		InkExplosion.createInkExplosion(getOwner(), InkExplosion.adjustPosition(result.getPos(), result.getSide().getUnitVector()), impactCoverage, 0, 0, inkType, sourceWeapon);
		if (getWorld().getBlockState(result.getBlockPos()).getBlock() instanceof StageBarrierBlock)
			getWorld().sendEntityStatus(this, (byte) -1);
		else
			getWorld().sendEntityStatus(this, (byte) 1);
		if (!getWorld().isClient)
		{
			discard();
		}
	}
	@Override
	public void setVelocity(@NotNull Entity thrower, float pitch, float yaw, float pitchOffset, float velocity, float inaccuracy)
	{
		super.setVelocity(thrower, pitch, yaw, pitchOffset, velocity, inaccuracy);
		InkExplosion.createInkExplosion(getOwner(), thrower.getPos(), 0.75f, 0, 0, inkType, sourceWeapon);
	}
	@Override
	public void setVelocity(double x, double y, double z, float velocity, float inaccuracy)
	{
		Vec3d vec3 = (new Vec3d(x, y, z)).normalize().add(random.nextGaussian() * 0.0075 * inaccuracy, random.nextGaussian() * 0.0075D * inaccuracy, random.nextGaussian() * 0.0075 * inaccuracy);
		
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
		
		if (nbt.contains("Size"))
			setProjectileSize(nbt.getFloat("Size"));
		
		NbtList directionTag = nbt.getList("DeltaMotion", NbtDouble.DOUBLE_TYPE);
		setVelocity(new Vec3d(directionTag.getDouble(0), directionTag.getDouble(1), directionTag.getDouble(2)));
		
		impactCoverage = nbt.getFloat("ImpactCoverage");
		
		setColor(InkColor.getFromNbt(nbt.get("DropColor")));
		
		if (nbt.contains("Gravity"))
			setGravity(nbt.getFloat("Gravity"));
		if (nbt.contains("Lifespan"))
			lifespan = nbt.getInt("Lifespan");
		
		setInvisible(nbt.getBoolean("Invisible"));
		
		inkType = InkBlockUtils.InkType.IDENTIFIER_MAP.getOrDefault(Identifier.of(nbt.getString("InkType")), InkBlockUtils.InkType.NORMAL);
		
		sourceWeapon = ItemStack.fromNbtOrEmpty(getRegistryManager(), nbt.getCompound("SourceWeapon"));
	}
	@Override
	public void writeCustomDataToNbt(NbtCompound nbt)
	{
		nbt.putFloat("Size", getProjectileSize());
		NbtList directionTag = new NbtList();
		Vec3d direction = getVelocity();
		directionTag.add(NbtDouble.of(direction.x));
		directionTag.add(NbtDouble.of(direction.y));
		directionTag.add(NbtDouble.of(direction.z));
		nbt.put("DeltaMotion", directionTag);
		
		nbt.putFloat("ImpactCoverage", impactCoverage);
		nbt.put("DropColor", getColor().getNbt());
		
		nbt.putDouble("Gravity", getGravity());
		nbt.putInt("Lifespan", lifespan);
		
		nbt.putBoolean("Invisible", isInvisible());
		
		nbt.putString("InkType", inkType.getSerializedName());
		nbt.put("SourceWeapon", sourceWeapon.encode(getWorld().getRegistryManager(), new NbtCompound()));
		
		super.writeCustomDataToNbt(nbt);
		nbt.remove("Item");
	}
	@Override
	public ItemStack getStack()
	{
		return sourceWeapon;
	}
	@Override
	public @NotNull EntityDimensions getDimensions(@NotNull EntityPose getMatrices)
	{
		return super.getDimensions(getMatrices).scaled(getProjectileSize() / 2f);
	}
	public float getProjectileSize()
	{
		return dataTracker.get(DROP_SIZE);
	}
	public void setProjectileSize(float size)
	{
		dataTracker.set(DROP_SIZE, size);
		refreshPosition();
		calculateDimensions();
	}
	@Deprecated() //Modify sourceWeapon variable instead
	@Override
	public void setItem(@NotNull ItemStack itemStack)
	{
	}
	@Override
	public double getGravity()
	{
		return dataTracker.get(GRAVITY);
	}
	public void setGravity(float gravity)
	{
		dataTracker.set(GRAVITY, gravity);
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
