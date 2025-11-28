package net.splatcraft.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.client.particles.InkCloudParticleData;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.platform.IExtraDataOnAddEntity;
import net.splatcraft.registries.SplatcraftEntities;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

public class InkStormDeployerEntity extends ThrowableProjectile implements IColoredEntity, ISetVelocityExtension, IExtraDataOnAddEntity
{
	private static final EntityDataAccessor<InkColor> COLOR = SynchedEntityData.defineId(InkStormDeployerEntity.class, CommonUtils.INKCOLOR_DATA_HANDLER);
	private static final EntityDataAccessor<Boolean> IS_RISING = SynchedEntityData.defineId(InkStormDeployerEntity.class, EntityDataSerializers.BOOLEAN);
	public float riseTime;
	public float riseSpeed;
	public EntitySlot providerSlot;
	public float spawnYaw = Float.NaN;
	public ResourceLocation dataId;
	public InkBlockUtils.InkType inkType;
	public InkStormDeployerEntity(EntityType<InkStormDeployerEntity> type, Level level)
	{
		this(type, level, null, 0, 0, EntitySlot.EMPTY);
	}
	public InkStormDeployerEntity(EntityType<InkStormDeployerEntity> type,
	                              Level level,
	                              ResourceLocation dataId,
	                              float riseTime,
	                              float riseSpeed,
	                              EntitySlot providerSlot)
	{
		super(type, level);
		this.dataId = dataId;
		this.riseTime = riseTime;
		this.riseSpeed = riseSpeed;
		this.providerSlot = providerSlot;
	}
	public InkStormDeployerEntity(Level level,
	                              LivingEntity owner,
	                              InkColor color,
	                              ResourceLocation dataId,
	                              float riseTime,
	                              float riseSpeed,
	                              EntitySlot providerSlot)
	{
		this(SplatcraftEntities.INK_STORM_DEPLOYER.get(), level, dataId, riseTime, riseSpeed, providerSlot);
		setColor(color);
		setOwner(owner);
		updateRotation();
		inkType = InkBlockUtils.getInkType(owner);

		xRotO = getXRot();
		yRotO = getYRot();
	}
	@Override
	public void tick()
	{
		super.tick();

		if (isRising())
		{
			if (level().isClientSide() && tickCount % 2 == 0)
			{
				level().addParticle(new InkCloudParticleData(getColor(), 0.6f), getX(), getY(), getZ(),
					random.nextFloat() * 0.1f, random.nextFloat() * 0.1f, random.nextFloat() * 0.1f);
			}
			setNoGravity(true);
			setDeltaMovement(0, riseSpeed, 0);
			riseTime--;
			if (riseTime <= 0)
			{
				Vec3 spawnPos = position().add(0, riseTime * riseSpeed, 0);
				if (getOwner() instanceof LivingEntity livingOwner)
				{
					if (dataId != null)
					{
						InkCloudEntity cloud = InkCloudEntity.create(level(), providerSlot, spawnYaw, livingOwner, getColor(), dataId);
						if (cloud != null)
						{
							cloud.setPos(spawnPos);
							level().addFreshEntity(cloud);
						}
					}
				}
				kill();
			}
		}
		else
		{
			updateRotation();
			setDeltaMovement(getDeltaMovement().scale(0.8));
		}
	}
	@Override
	protected void onHitBlock(@NotNull BlockHitResult result)
	{
		setRising(true);
		setPos(result.getLocation());
		super.onHitBlock(result);
	}
	@Override
	public boolean canHitEntity(@NotNull Entity entity)
	{
//		boolean isntOwnerOrSelf = entity != this && entity != getOwner();
//		return isntOwnerOrSelf && InkDamageUtils.canDamage(entity, entityData.get(COLOR));
		return false;
	}
	@Override
	public boolean canBeHitByProjectile()
	{
		return false;
	}
	@Override
	public boolean isPushable()
	{
		return false;
	}
	@Override
	public void setDeltaMovement(Entity thrower, float pitch, float yaw, float pitchOffset, float speed, float inaccuracy, double throwerImpulse)
	{
		if (Float.isNaN(spawnYaw))
			spawnYaw = yaw;
		ISetVelocityExtension.super.setDeltaMovement(thrower, pitch, yaw, pitchOffset, speed, inaccuracy, throwerImpulse);
	}
	@Override
	public void onVelocityCalculated(Vec3 direction, float speed)
	{
		setDeltaMovement(direction);
	}
	@Override
	protected double getDefaultGravity()
	{
		return 0.1;
	}
	@Override
	public void updateRotation()
	{
		xRotO = getXRot();
		yRotO = getYRot();

		if (!isRising())
			setXRot(lerpRotation(getXRot(), getXRot() + 0.3f));
	}
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(COLOR, ColorUtils.getDefaultColor());
		builder.define(IS_RISING, false);
	}
	@Override
	public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet)
	{
		super.recreateFromPacket(packet);
		updateRotation();
		xRotO = getXRot();
		yRotO = getYRot();
	}
	@Override
	public void readExtraData(RegistryFriendlyByteBuf buf)
	{
		riseTime = buf.readFloat();
		riseSpeed = buf.readFloat();
		spawnYaw = buf.readFloat();
	}
	@Override
	public void writeExtraData(RegistryFriendlyByteBuf buf)
	{
		buf.writeFloat(riseTime);
		buf.writeFloat(riseSpeed);
		buf.writeFloat(spawnYaw);
	}
	@Override
	protected void readAdditionalSaveData(CompoundTag nbt)
	{
		if (nbt.contains("Color"))
			setColor(InkColor.getFromNbt(nbt.get("Color")));
		if (nbt.contains("IsRising"))
			setRising(nbt.getBoolean("IsRising"));
		if (nbt.contains("RiseTime"))
			riseTime = nbt.getFloat("RiseTime");
		if (nbt.contains("RiseSpeed"))
			riseSpeed = nbt.getFloat("RiseSpeed");
		if (nbt.contains("SpawnYaw"))
			spawnYaw = nbt.getFloat("SpawnYaw");

		providerSlot = EntitySlot.SERIALIZER_CODEC.parse(NbtOps.INSTANCE, nbt.get("ProviderSlot")).result().orElse(EntitySlot.EMPTY);
		if (nbt.contains("DataId"))
			ResourceLocation.CODEC.parse(NbtOps.INSTANCE, nbt.get("DataId")).ifSuccess(id -> dataId = id);
		if (nbt.contains("InkType"))
			inkType = InkBlockUtils.InkType.CODEC.parse(NbtOps.INSTANCE, nbt.get("InkType")).result().orElse(InkBlockUtils.InkType.NORMAL);
		super.readAdditionalSaveData(nbt);
	}
	@Override
	protected void addAdditionalSaveData(CompoundTag nbt)
	{
		nbt.put("Color", getColor().getNbt());
		nbt.putBoolean("IsRising", isRising());
		nbt.putFloat("RiseTime", riseTime);
		nbt.putFloat("RiseSpeed", riseSpeed);
		nbt.putFloat("SpawnYaw", spawnYaw);
		EntitySlot.SERIALIZER_CODEC.encodeStart(NbtOps.INSTANCE, providerSlot).ifSuccess(tag -> nbt.put("ProviderSlot", tag));
		if (dataId != null)
			ResourceLocation.CODEC.encodeStart(NbtOps.INSTANCE, dataId).ifSuccess(tag -> nbt.put("DataId", tag));
		InkBlockUtils.InkType.CODEC.encodeStart(NbtOps.INSTANCE, inkType).ifSuccess(tag -> nbt.put("InkType", tag));
		super.addAdditionalSaveData(nbt);
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
	public boolean isRising()
	{
		return entityData.get(IS_RISING);
	}
	public void setRising(boolean rising)
	{
		entityData.set(IS_RISING, rising);
	}
}
