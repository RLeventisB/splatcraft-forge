package net.splatcraft.entities;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.client.audio.StingRayTickableSound;
import net.splatcraft.client.particles.InkSplashParticleData;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.registries.SplatcraftDamageTypes;
import net.splatcraft.registries.SplatcraftEntities;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.*;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.specials.StingRayAction;
import net.splatcraft.util.structs.AttackId;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import java.util.concurrent.atomic.AtomicBoolean;

public class StingRayBeamEntity extends Projectile implements IColoredEntity
{
	private static final EntityDataAccessor<InkColor> COLOR = SynchedEntityData.defineId(StingRayBeamEntity.class, CommonUtils.INKCOLOR_DATA_HANDLER);
	private static final EntityDataAccessor<Integer> TIME_VALUES = SynchedEntityData.defineId(StingRayBeamEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Vector2f> WIDTH_VALUES = SynchedEntityData.defineId(StingRayBeamEntity.class, CommonUtils.VEC2_DATA_HANDLER);
	private static final EntityDataAccessor<Vector2f> TURNING_VALUES = SynchedEntityData.defineId(StingRayBeamEntity.class, CommonUtils.VEC2_DATA_HANDLER);
	public float rayDamage, shockwaveDamage, paintingRadius, paintingClipSize;
	public InkBlockUtils.InkType inkType;
	public StingRayBeamEntity(EntityType<StingRayBeamEntity> type, Level world)
	{
		this(type, world, 0, 0, 0, 0, 0, 0, 0, 0);
	}
	public StingRayBeamEntity(EntityType<StingRayBeamEntity> type,
	                          Level world,
	                          float turningValue,
	                          float turningValueWithShockwave,
	                          float rayWidth,
	                          float shockwaveWidth,
	                          float rayDamage,
	                          float shockwaveDamage,
	                          float paintingRadius,
	                          float paintingClipSize
	)
	{
		super(type, world);
		refreshDimensions();
		reapplyPosition();
		setTurningValue(turningValue);
		setTurningValueWithShockwave(turningValueWithShockwave);
		setRayWidth(rayWidth);
		setShockwaveWidth(shockwaveWidth);
		this.rayDamage = rayDamage;
		this.shockwaveDamage = shockwaveDamage;
		this.paintingRadius = paintingRadius;
		this.paintingClipSize = paintingClipSize;
	}
	public StingRayBeamEntity(Level world,
	                          LivingEntity owner,
	                          InkColor color,
	                          byte startup,
	                          byte shockwaveDelay,
	                          float turningValue,
	                          float turningValueWithShockwave,
	                          float rayWidth,
	                          float shockwaveWidth,
	                          float rayDamage,
	                          float shockwaveDamage,
	                          float paintingRadius,
	                          float paintSearchRadius)
	{
		this(SplatcraftEntities.STING_RAY_PROJECTILE.get(), world, turningValue, turningValueWithShockwave, rayWidth, shockwaveWidth, rayDamage, shockwaveDamage, paintingRadius, paintSearchRadius);
		setColor(color);
		setOwner(owner);
		refreshDimensions();
		reapplyPosition();
		setStartup(startup);
		setXRot(owner.getXRot());
		setYRot(owner.getYHeadRot());
		setShockwaveDelay(shockwaveDelay);
		updatePosForward(owner);
		updateRotation();
		inkType = InkBlockUtils.getInkType(owner);
		xRotO = getXRot();
		yRotO = getYRot();
	}
	// this comes from https://stackoverflow.com/questions/34952680/distance-between-a-ray-and-a-bound-box
	// yes stack overflow (and Raidho Coaxil with 41 of reputation score and 3 bronze badges who had access
	// to better search engines than now i suppose because i cant find this code anywhere else) comes to save
	// me from eternal torment
	public static double getDistance(Vec3 rayDirection, AABB relativeBox)
	{
		double tx1 = relativeBox.minX / rayDirection.x;
		double tx2 = relativeBox.maxX / rayDirection.x;
		double ty1 = relativeBox.minY / rayDirection.y;
		double ty2 = relativeBox.maxY / rayDirection.y;
		double tz1 = relativeBox.minZ / rayDirection.y;
		double tz2 = relativeBox.maxZ / rayDirection.y;
		
		double p1 = Math.max(0.0, Math.max(tx1, Math.min(ty1, tz1)));
		double p2 = Math.max(0.0, Math.min(tx2, Math.max(ty2, tz2)));
		
		double x = Mth.clamp((rayDirection.x * p1 + rayDirection.x * p2) / 2, relativeBox.minX, relativeBox.maxX);
		double y = Mth.clamp((rayDirection.y * p1 + rayDirection.y * p2) / 2, relativeBox.minY, relativeBox.maxY);
		double z = Mth.clamp((rayDirection.z * p1 + rayDirection.z * p2) / 2, relativeBox.minZ, relativeBox.maxZ);
		
		double t = Math.max(0.0, rayDirection.dot(new Vec3(x, y, z)) / rayDirection.lengthSqr());
		x = rayDirection.x * t - x;
		y = rayDirection.y * t - y;
		z = rayDirection.z * t - z;
		return Math.sqrt(x * x + y * y + z * z);
	}
	public static Vec3 getClosestPoint(Vec3 rayDirection, Vec3 relativePoint)
	{
		double x = relativePoint.x;
		double y = relativePoint.y;
		double z = relativePoint.z;
		
		double t = Math.max(0.0, rayDirection.dot(relativePoint) / rayDirection.lengthSqr());
		
		x = rayDirection.x * t - x;
		y = rayDirection.y * t - y;
		z = rayDirection.z * t - z;
		return new Vec3(x, y, z);
	}
	@OnlyIn(Dist.CLIENT)
	public static void playSound(StingRayBeamEntity beam)
	{
		Minecraft.getInstance().getSoundManager().queueTickingSound(new StingRayTickableSound(beam));
	}
	@Override
	public void tick()
	{
		if (getLifespan() == 0 && level().isClientSide())
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
		Vec3 forward = updatePosForward(owner);
		
		if (isBeamActive())
		{
			if (level().isClientSide)
			{
				level().addParticle(new InkSplashParticleData(getColor(), 0.4f),
					getX(),
					getY(),
					getZ(),
					forward.x + random.nextFloat() / 2f - 0.25f,
					forward.y + random.nextFloat() / 2f - 0.25f,
					forward.z + random.nextFloat() / 2f - 0.25f
				);
			}
			else
			{
				float collisionDistance = paint(forward);
				doCollisions(forward);
			}
		}
		
		setLifespan(lifespan + 1);
	}
	public float paint(Vec3 forward)
	{
		ClipContext context = new ClipContext(position(), position().add(forward.scale(paintingClipSize)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this);
		BlockHitResult result = level().clip(context);
		if (result.getType() == HitResult.Type.MISS)
			return Float.POSITIVE_INFINITY;
		
		InkExplosion.createInkExplosion(this, InkExplosion.adjustPosition(result.getLocation(), result.getDirection(), null), paintingRadius, inkType, ItemStack.EMPTY);
		return (float) position().distanceToSqr(result.getLocation());
	}
	private Vec3 updatePosForward(LivingEntity owner)
	{
		Vec3 forward = getLookAngle();
		setPos(owner.getEyePosition().add(forward));
		return forward;
	}
	public void doCollisions(Vec3 forward)
	{
		AtomicBoolean canDoSound = new AtomicBoolean(getLifespan() % 4 == 0);
		for (Entity entity : level().getEntities().getAll())
		{
			if (!canHitEntity(entity))
				continue;
			
			AABB relativeBox = entity.getBoundingBox().move(position().reverse());
			Vec3[] boxPoints = new Vec3[] {
				new Vec3(relativeBox.minX, relativeBox.minY, relativeBox.minZ),
				new Vec3(relativeBox.minX, relativeBox.minY, relativeBox.maxZ),
				new Vec3(relativeBox.minX, relativeBox.maxY, relativeBox.minZ),
				new Vec3(relativeBox.minX, relativeBox.maxY, relativeBox.maxZ),
				new Vec3(relativeBox.maxX, relativeBox.minY, relativeBox.minZ),
				new Vec3(relativeBox.maxX, relativeBox.minY, relativeBox.maxZ),
				new Vec3(relativeBox.maxX, relativeBox.maxY, relativeBox.minZ),
				new Vec3(relativeBox.maxX, relativeBox.maxY, relativeBox.maxZ),
			};
			
			boolean isOnForwardPlane = false;
			for (Vec3 point : boxPoints)
			{
				if (forward.dot(point) >= 0)
				{
					isOnForwardPlane = true;
					break;
				}
			}
			
			if (isOnForwardPlane)
			{
				double distance = getDistance(forward, relativeBox);
				
				if (distance < getRayWidth())
				{
					hit(entity, rayDamage, canDoSound);
				}
				else if (hasStartedToShowTheHellspawn() && distance < getShockwaveWidth())
				{
					hit(entity, shockwaveDamage, canDoSound);
				}
			}
		}
	}
	private void hit(Entity target, float dmg, AtomicBoolean playSound)
	{
		if (target instanceof SpawnShieldEntity && !InkDamageUtils.canDamage(target, this))
		{
			return;
		}
		
		if (target instanceof LivingEntity livingTarget)
		{
			if (InkDamageUtils.isSplatted(livingTarget)) return;
			
			boolean didDamage = InkDamageUtils.doDamage(livingTarget, dmg, getOwner(), this, ItemStack.EMPTY, SplatcraftDamageTypes.INK_SPLAT, false, AttackId.NONE);
			if (!level().isClientSide && didDamage && playSound.get())
			{
				playSound.set(false);
				level().playSound(null, getOwner().getX(), getOwner().getY(), getOwner().getZ(), SplatcraftSounds.shotHit, SoundSource.PLAYERS, 0.7f, 1f);
			}
		}
	}
	@Override
	public boolean canHitEntity(@NotNull Entity entity)
	{
		boolean isntOwnerOrSelf = entity != this && entity != getOwner();
		return isntOwnerOrSelf && entity.canBeHitByProjectile() && InkDamageUtils.canDamage(entity, entityData.get(COLOR));
	}
	@Override
	public boolean canBeHitByProjectile()
	{
		return false;
	}
	@Override
	public @NotNull Vec3 getKnownMovement()
	{
		return Vec3.ZERO;
	}
	@Override
	public void setDeltaMovement(double x, double y, double z)
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
		
		xRotO = getXRot();
		yRotO = getYRot();
		
		float finalTurningValue = hasStartedToShowTheHellspawn() ? getTurningValueWithShockwave() : getTurningValue();
		setXRot(Mth.rotLerp(finalTurningValue, getXRot(), owner.getXRot()));
		setYRot(Mth.rotLerp(finalTurningValue, getYRot(), owner.getYRot()));
	}
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(COLOR, ColorUtils.getDefaultColor());
		builder.define(TIME_VALUES, 0);
		builder.define(WIDTH_VALUES, new Vector2f());
		builder.define(TURNING_VALUES, new Vector2f());
	}
	@Override
	public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet)
	{
		super.recreateFromPacket(packet);
		updateRotation();
		updatePosForward((LivingEntity) getOwner());
		updateRotation();
		xRotO = getXRot();
		yRotO = getYRot();
	}
	@Override
	protected void readAdditionalSaveData(CompoundTag nbt)
	{
		if (nbt.contains("Color"))
			setColor(InkColor.getFromNbt(nbt.get("Color")));
		if (nbt.contains("TimeValues"))
			setTimeValues(nbt.getInt("TimeValues"));
		if (nbt.contains("RayValues"))
		{
			CompoundTag valuesNbt = (CompoundTag) nbt.get("RayValues");
			setTurningValue(valuesNbt.getFloat("TurningValue"));
			setTurningValueWithShockwave(valuesNbt.getFloat("TurningValueShockwave"));
			setRayWidth(valuesNbt.getFloat("RayWidth"));
			setShockwaveWidth(valuesNbt.getFloat("ShockwaveWidth"));
			rayDamage = valuesNbt.getFloat("RayDmg");
			shockwaveDamage = valuesNbt.getFloat("ShockwaveDmg");
			paintingRadius = valuesNbt.getFloat("RayPaintSize");
			paintingClipSize = valuesNbt.getFloat("RayPaintSearch");
			inkType = InkBlockUtils.InkType.IDENTIFIER_MAP.getOrDefault(ResourceLocation.parse(valuesNbt.getString("InkType")), InkBlockUtils.InkType.NORMAL);
		}
		super.readAdditionalSaveData(nbt);
	}
	@Override
	protected void addAdditionalSaveData(CompoundTag nbt)
	{
		nbt.put("Color", getColor().getNbt());
		nbt.putInt("TimeValues", getTimeValues());
		CompoundTag valuesNbt = new CompoundTag();
		valuesNbt.putFloat("TurningValue", getTurningValue());
		valuesNbt.putFloat("TurningValueShockwave", getTurningValueWithShockwave());
		valuesNbt.putFloat("RayWidth", getRayWidth());
		valuesNbt.putFloat("ShockwaveWidth", getShockwaveWidth());
		valuesNbt.putFloat("RayDmg", rayDamage);
		valuesNbt.putFloat("ShockwaveDmg", shockwaveDamage);
		valuesNbt.putFloat("RayPaintSize", paintingRadius);
		valuesNbt.putFloat("RayPaintSearch", paintingClipSize);
		valuesNbt.putString("InkType", inkType.getIdString());
		nbt.put("RayValues", valuesNbt);
		super.addAdditionalSaveData(nbt);
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
		return entityData.get(COLOR);
	}
	@Override
	public void setColor(InkColor color)
	{
		entityData.set(COLOR, color);
	}
	public int getTimeValues()
	{
		return entityData.get(TIME_VALUES);
	}
	public void setTimeValues(int values)
	{
		entityData.set(TIME_VALUES, values);
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
		return getSharedFlag(7);
	}
	public void markOwnerStopShooting()
	{
		setSharedFlag(7, true);
	}
	@Override
	public @NotNull EntityDimensions getDimensions(@NotNull Pose pose)
	{
		return EntityDimensions.fixed(0, 0);
	}
	public byte getState()
	{
		return hasStartedToShowTheHellspawn() ? (byte) 2 : isBeamActive() ? (byte) 1 : 0;
	}
	public float getRayWidth()
	{
		return entityData.get(WIDTH_VALUES).x;
	}
	public void setRayWidth(float rayWidth)
	{
		entityData.set(WIDTH_VALUES, new Vector2f(rayWidth, getShockwaveWidth()));
	}
	public float getShockwaveWidth()
	{
		return entityData.get(WIDTH_VALUES).y;
	}
	public void setShockwaveWidth(float shockwaveWidth)
	{
		entityData.set(WIDTH_VALUES, new Vector2f(getRayWidth(), shockwaveWidth));
	}
	public float getTurningValue()
	{
		return entityData.get(TURNING_VALUES).x;
	}
	public void setTurningValue(float turningValue)
	{
		entityData.set(TURNING_VALUES, new Vector2f(turningValue, getTurningValueWithShockwave()));
	}
	public float getTurningValueWithShockwave()
	{
		return entityData.get(TURNING_VALUES).y;
	}
	public void setTurningValueWithShockwave(float turningValueWithShockwave)
	{
		entityData.set(TURNING_VALUES, new Vector2f(getTurningValue(), turningValueWithShockwave));
	}
}
