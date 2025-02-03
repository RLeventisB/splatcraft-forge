package net.splatcraft.entities;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.splatcraft.client.audio.StingRayTickableSound;
import net.splatcraft.client.particles.InkSplashParticleData;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.registries.SplatcraftDamageTypes;
import net.splatcraft.registries.SplatcraftEntities;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.*;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.specials.StingRayAction;
import org.jetbrains.annotations.NotNull;

public class StingRayBeamEntity extends ProjectileEntity implements IColoredEntity
{
	private static final TrackedData<InkColor> COLOR = DataTracker.registerData(StingRayBeamEntity.class, CommonUtils.INKCOLORDATAHANDLER);
	private static final TrackedData<Integer> TIME_VALUES = DataTracker.registerData(StingRayBeamEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private final float turningValue, rayWidth, rayDamage;
	private final float turningValueWithShockwave, shockwaveWidth, shockwaveDamage;
	public StingRayBeamEntity(EntityType<StingRayBeamEntity> type, World world)
	{
		this(type, world, 0, 0, 0, 0, 0, 0);
	}
	public StingRayBeamEntity(EntityType<StingRayBeamEntity> type,
	                          World world,
	                          float turningValue,
	                          float turningValueWithShockwave,
	                          float rayWidth,
	                          float shockwaveWidth,
	                          float rayDamage,
	                          float shockwaveDamage
	)
	{
		super(type, world);
		calculateDimensions();
		refreshPosition();
		this.turningValue = turningValue;
		this.turningValueWithShockwave = turningValueWithShockwave;
		this.rayWidth = rayWidth;
		this.shockwaveWidth = shockwaveWidth;
		this.rayDamage = rayDamage;
		this.shockwaveDamage = shockwaveDamage;
	}
	public StingRayBeamEntity(World world,
	                          LivingEntity owner,
	                          InkColor color,
	                          byte startup,
	                          byte shockwaveDelay,
	                          float turningValue,
	                          float turningValueWithShockwave,
	                          float rayWidth,
	                          float shockwaveWidth,
	                          float rayDamage,
	                          float shockwaveDamage
	)
	{
		this(SplatcraftEntities.STING_RAY_PROJECTILE.get(), world, turningValue, turningValueWithShockwave, rayWidth, shockwaveWidth, rayDamage, shockwaveDamage);
		setColor(color);
		setOwner(owner);
		calculateDimensions();
		refreshPosition();
		setStartup(startup);
		setPitch(owner.getPitch());
		setYaw(owner.getHeadYaw());
		setShockwaveDelay(shockwaveDelay);
	}
	// this comes from https://stackoverflow.com/questions/34952680/distance-between-a-ray-and-a-bound-box
	// yes stack overflow (and Raidho Coaxil with 41 of reputation score and 3 bronze badges who had access
	// to better search engines than now i suppose because i cant find this code anywhere else) comes to save
	// me from eternal torment
	public static double getDistance(Vec3d rayDirection, Box relativeBox)
	{
		double tx1 = relativeBox.minX / rayDirection.x;
		double tx2 = relativeBox.maxX / rayDirection.x;
		double ty1 = relativeBox.minY / rayDirection.y;
		double ty2 = relativeBox.maxY / rayDirection.y;
		double tz1 = relativeBox.minZ / rayDirection.y;
		double tz2 = relativeBox.maxZ / rayDirection.y;
		
		double p1 = Math.max(0.0, Math.max(tx1, Math.min(ty1, tz1)));
		double p2 = Math.max(0.0, Math.min(tx2, Math.max(ty2, tz2)));
		
		double x = MathHelper.clamp((rayDirection.x * p1 + rayDirection.x * p2) / 2, relativeBox.minX, relativeBox.maxX);
		double y = MathHelper.clamp((rayDirection.y * p1 + rayDirection.y * p2) / 2, relativeBox.minY, relativeBox.maxY);
		double z = MathHelper.clamp((rayDirection.z * p1 + rayDirection.z * p2) / 2, relativeBox.minZ, relativeBox.maxZ);
		
		double t = Math.max(0.0, rayDirection.dotProduct(new Vec3d(x, y, z)) / rayDirection.lengthSquared());
		x = rayDirection.x * t - x;
		y = rayDirection.y * t - y;
		z = rayDirection.z * t - z;
		return Math.sqrt(x * x + y * y + z * z);
	}
	public static Vec3d getClosestPoint(Vec3d rayDirection, Vec3d relativePoint)
	{
		double x = relativePoint.x;
		double y = relativePoint.y;
		double z = relativePoint.z;
		
		double t = Math.max(0.0, rayDirection.dotProduct(relativePoint) / rayDirection.lengthSquared());
		
		x = rayDirection.x * t - x;
		y = rayDirection.y * t - y;
		z = rayDirection.z * t - z;
		return new Vec3d(x, y, z);
	}
	@Environment(EnvType.CLIENT)
	public static void playSound(StingRayBeamEntity beam)
	{
		MinecraftClient.getInstance().getSoundManager().playNextTick(new StingRayTickableSound(beam));
	}
	@Override
	public void tick()
	{
		if (getLifespan() == 0)
		{
			playSound(this);
		}
		
		super.tick();
		
		updateRotation();
		
		if (!(getOwner() instanceof LivingEntity owner) || !owner.isAlive() || EntityInfoCapability.isSquid(owner))
		{
			discard();
			return;
		}
		
		if (!owner.isUsingItem() || !EntityAction.hasSpecificEntityAction(owner, StingRayAction.class))
		{
			markOwnerStopShooting();
		}
		
		int lifespan = getLifespan();
		if (hasOwnerStopShooting())
		{
			if (lifespan >= getStartup())
			{
				discard();
				return;
			}
			setLifespan(lifespan + 1);
			return;
		}
		
		tickRay(owner, lifespan);
	}
	public void tickRay(LivingEntity owner, int lifespan)
	{
		Vec3d forward = getRotationVector();
		setPosition(owner.getEyePos().add(forward));
		
		if (isBeamActive())
		{
			if (getWorld().isClient)
			{
				getWorld().addParticle(new InkSplashParticleData(getColor(), 1f), getX(), getY(), getZ(), forward.x, forward.y, forward.z);
			}
			else
				doCollisions(forward);
		}
		
		setLifespan(lifespan + 1);
	}
	public void doCollisions(Vec3d forward)
	{
		for (Entity entity : getWorld().getEntityLookup().iterate())
		{
			if (!canHit(entity))
				continue;
			
			Box relativeBox = entity.getBoundingBox().offset(getPos().negate());
			Vec3d[] boxPoints = new Vec3d[] {
				new Vec3d(relativeBox.minX, relativeBox.minY, relativeBox.minZ),
				new Vec3d(relativeBox.minX, relativeBox.minY, relativeBox.maxZ),
				new Vec3d(relativeBox.minX, relativeBox.maxY, relativeBox.minZ),
				new Vec3d(relativeBox.minX, relativeBox.maxY, relativeBox.maxZ),
				new Vec3d(relativeBox.maxX, relativeBox.minY, relativeBox.minZ),
				new Vec3d(relativeBox.maxX, relativeBox.minY, relativeBox.maxZ),
				new Vec3d(relativeBox.maxX, relativeBox.maxY, relativeBox.minZ),
				new Vec3d(relativeBox.maxX, relativeBox.maxY, relativeBox.maxZ),
			};
			
			boolean isOnForwardPlane = false;
			for (Vec3d point : boxPoints)
			{
				if (forward.dotProduct(point) >= 0)
				{
					isOnForwardPlane = true;
					break;
				}
			}
			
			if (isOnForwardPlane)
			{
				double distance = getDistance(forward, relativeBox);
				
				if (distance < rayWidth)
				{
					hit(entity, rayDamage);
				}
				else if (hasStartedToShowTheHellspawn() && distance < shockwaveWidth)
				{
					hit(entity, shockwaveDamage);
				}
			}
		}
	}
	private void hit(Entity target, float dmg)
	{
		if (target instanceof SpawnShieldEntity && !InkDamageUtils.canDamage(target, this))
		{
			return;
		}
		
		if (target instanceof LivingEntity livingTarget)
		{
			if (InkDamageUtils.isSplatted(livingTarget)) return;
			
			boolean didDamage = InkDamageUtils.doDamage(livingTarget, dmg, getOwner(), this, ItemStack.EMPTY, SplatcraftDamageTypes.INK_SPLAT, false, AttackId.NONE);
			if (!getWorld().isClient && didDamage)
			{
				getWorld().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterDirect, SoundCategory.PLAYERS, 0.8F, 1);
			}
		}
	}
	@Override
	public boolean canHit(Entity entity)
	{
		boolean isntOwnerOrSelf = entity != this && entity != getOwner();
		return isntOwnerOrSelf && entity.canBeHitByProjectile() && InkDamageUtils.canDamage(entity, dataTracker.get(COLOR));
	}
	@Override
	public boolean canBeHitByProjectile()
	{
		return false;
	}
	@Override
	public Vec3d getMovement()
	{
		return Vec3d.ZERO;
	}
	@Override
	public void setVelocity(double x, double y, double z)
	{
	}
	@Override
	public boolean isPushable()
	{
		return false;
	}
	@Override
	public void updateRotation()
	{
		Entity owner = getOwner();
		if (owner == null || !owner.isAlive() || hasOwnerStopShooting())
		{
			return;
		}
		
		float finalTurningValue = hasStartedToShowTheHellspawn() ? turningValueWithShockwave : turningValue;
		setPitch(MathHelper.lerpAngleDegrees(finalTurningValue, getPitch(), owner.getPitch()));
		setYaw(MathHelper.lerpAngleDegrees(finalTurningValue, getYaw(), owner.getYaw()));
	}
	@Override
	public void kill()
	{
		if (getOwner() != null && getOwner().isAlive())
			return;
		
		super.kill();
	}
	@Override
	protected void initDataTracker(DataTracker.Builder builder)
	{
		builder.add(COLOR, ColorUtils.getDefaultColor());
		builder.add(TIME_VALUES, 0);
	}
	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt)
	{
		if (nbt.contains("Color"))
			setColor(InkColor.getFromNbt(nbt.get("Color")));
		if (nbt.contains("TimeValues"))
			setTimeValues(nbt.getInt("TimeValues"));
		super.readCustomDataFromNbt(nbt);
	}
	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt)
	{
		nbt.put("Color", getColor().getNbt());
		nbt.putInt("TimeValues", getTimeValues());
		super.writeCustomDataToNbt(nbt);
	}
	public boolean hasStartedToShowTheHellspawn()
	{
		return getLifespan() >= getShockwaveDelay();
	}
	public boolean isBeamActive()
	{
		return getLifespan() >= getStartup();
	}
	@Override
	public InkColor getColor()
	{
		return dataTracker.get(COLOR);
	}
	@Override
	public void setColor(InkColor color)
	{
		dataTracker.set(COLOR, color);
	}
	public int getTimeValues()
	{
		return dataTracker.get(TIME_VALUES);
	}
	public void setTimeValues(int values)
	{
		dataTracker.set(TIME_VALUES, values);
	}
	public int getStartup()
	{
		return getTimeValues() & 0x000000FF;
	}
	public void setStartup(byte startup)
	{
		setTimeValues(getTimeValues() & 0xFFFFFF00 | startup);
	}
	public int getShockwaveDelay()
	{
		return getTimeValues() >> 8 & 0x000000FF;
	}
	public void setShockwaveDelay(byte delay)
	{
		setTimeValues(getTimeValues() & 0xFFFF00FF | delay << 8);
	}
	public int getLifespan()
	{
		return getTimeValues() >> 16 & 0x0000FFFF;
	}
	public void setLifespan(int lifespan)
	{
		setTimeValues(getTimeValues() & 0x0000FFFF | lifespan << 16);
	}
	public boolean hasOwnerStopShooting()
	{
		return getFlag(7);
	}
	public void markOwnerStopShooting()
	{
		setFlag(7, true);
	}
	@Override
	public @NotNull EntityDimensions getDimensions(@NotNull EntityPose pose)
	{
		return EntityDimensions.fixed(0, 0);
	}
	public byte getState()
	{
		return hasStartedToShowTheHellspawn() ? (byte) 2 : isBeamActive() ? (byte) 1 : 0;
	}
}
