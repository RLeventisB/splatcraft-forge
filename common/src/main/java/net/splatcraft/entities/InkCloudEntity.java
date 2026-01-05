package net.splatcraft.entities;

import net.minecraft.client.GraphicsStatus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.handlers.DataHandler;
import net.splatcraft.handlers.WeaponHandler;
import net.splatcraft.items.weapons.settings.AbstractWeaponSettings;
import net.splatcraft.items.weapons.settings.SpecialWeaponRecords;
import net.splatcraft.items.weapons.settings.SpecialWeaponSettings;
import net.splatcraft.platform.IExtraDataOnAddEntity;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftDamageTypes;
import net.splatcraft.registries.SplatcraftEntities;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.util.*;
import net.splatcraft.util.structs.AttackId;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import oshi.util.tuples.Triplet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class InkCloudEntity extends Projectile implements IColoredEntity, ISetVelocityExtension, IExtraDataOnAddEntity
{
	private static final EntityDataAccessor<InkColor> COLOR = SynchedEntityData.defineId(InkCloudEntity.class, CommonUtils.INKCOLOR_DATA_HANDLER);
	private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(InkCloudEntity.class, EntityDataSerializers.FLOAT);
	public float moveSpeed, damage, duration, dropletRadius, dropletFrequency, dropletCounter;
	public byte formationTime;
	public static final byte DISSAPEAR_TICKS = 18;
	public InkBlockUtils.InkType inkType;
	public List<ParticleData> cloudParticleRenderData = null;
	@Nullable
	public static InkCloudEntity create(Level level,
	                                    EntitySlot providerSlot,
	                                    float movementYaw,
	                                    LivingEntity owner,
	                                    InkColor color,
	                                    ResourceLocation dataId
	)
	{
		AbstractWeaponSettings<?, ?> settings = DataHandler.WeaponStatsListener.SETTINGS.get(dataId);
		Optional<ItemStack> provider = providerSlot.tryGetItemFrom(owner).filter(v -> v.has(SplatcraftComponents.SPECIAL_PROVIDER_DATA));
		
		if (settings instanceof SpecialWeaponSettings<?> specialSettings && specialSettings.specialDataRecord instanceof SpecialWeaponRecords.InkStormDataRecord stormData)
		{
			int duration = (int) (stormData.specialCooldown() * 20);
			InkCloudEntity cloud = new InkCloudEntity(level, owner, color, stormData.cloudSpeed(), stormData.damagePerSecond() / 20, (byte) stormData.formationTime(), duration, stormData.dropletPaint(), 20f / stormData.dropletsPerSecond(), stormData.cloudRadius());
			movementYaw *= Mth.DEG_TO_RAD;
			cloud.setDeltaMovement(-Mth.sin(movementYaw) * stormData.cloudSpeed(), 0, Mth.cos(movementYaw) * stormData.cloudSpeed());
			provider.ifPresent(stack ->
			{
				stack.update(SplatcraftComponents.SPECIAL_PROVIDER_DATA, SplatcraftComponents.SpecialProviderData.DEFAULT,
					v -> v.withDelay(duration, duration).withStoredCharge(0f));
			});
			
			return cloud;
		}
		provider.ifPresent(stack ->
		{
			stack.update(SplatcraftComponents.SPECIAL_PROVIDER_DATA, SplatcraftComponents.SpecialProviderData.DEFAULT,
				v -> v.withDelay(0).withStoredCharge(0f));
		});
		
		return null;
	}
	public InkCloudEntity(EntityType<InkCloudEntity> type, Level level)
	{
		this(type, level, 0, 0, (byte) 0, 0, 0, 0, 0);
	}
	public InkCloudEntity(EntityType<InkCloudEntity> type,
	                      Level level,
	                      float moveSpeed,
	                      float damage,
	                      byte formationTime,
	                      float duration,
	                      float dropletRadius,
	                      float dropletFrequency,
	                      float cloudRadius
	)
	{
		super(type, level);
		this.moveSpeed = moveSpeed;
		this.damage = damage;
		this.formationTime = formationTime;
		this.duration = duration;
		this.dropletRadius = dropletRadius;
		this.dropletFrequency = dropletFrequency;
		
		setRadius(cloudRadius);
	}
	public InkCloudEntity(Level level,
	                      LivingEntity owner,
	                      InkColor color,
	                      float moveSpeed,
	                      float damage,
	                      byte formationTime,
	                      float duration,
	                      float dropletRadius,
	                      float dropletFrequency,
	                      float cloudRadius
	
	)
	{
		this(SplatcraftEntities.INK_CLOUD.get(), level, moveSpeed, damage, formationTime, duration, dropletRadius, dropletFrequency, cloudRadius);
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
		
		int activeTickCount = tickCount - formationTime;
		
		if (activeTickCount > 0 && tickCount < duration + DISSAPEAR_TICKS / 3)
			processDamage();
		
		if (level().isClientSide())
		{
			tickParticleData(activeTickCount);
		}
		else
		{
			if (activeTickCount <= duration && activeTickCount >= 0)
			{
				dropletCounter++;
				while (dropletCounter > 0)
				{
					float angle = random.nextFloat() * Mth.TWO_PI;
					float magnitude = random.nextFloat() * getRadius();
					Vec3 spawnPos = position().add(Mth.cos(angle) * magnitude, 0, Mth.sin(angle) * magnitude);
					InkDropEntity drop = new InkDropEntity(level(), spawnPos, getOwner(), getColor(), inkType, dropletRadius, ItemStack.EMPTY);
					drop.setNoGravity(true);
					drop.setDeltaMovement(0, -0.8, 0);
					
					level().addFreshEntity(drop);
					
					dropletCounter -= dropletFrequency;
					if (dropletFrequency == 0)
						break;
				}
			}
		}
		if (activeTickCount > duration + DISSAPEAR_TICKS)
			kill();
		
		setPos(position().add(getDeltaMovement()));
	}
	private void processDamage()
	{
		AABB cloudHitArea = AABB.ofSize(position(), getRadius() * 2, 0, getRadius() * 2);
		cloudHitArea = cloudHitArea.setMinY(Double.NEGATIVE_INFINITY).expandTowards(getDeltaMovement().scale(-300));
		Vector3f damageDirection = getDeltaMovement().reverse().add(0, -1, 0).toVector3f();
		
		for (Entity entity : level().getEntities().getAll())
		{
			if (!canHitEntity(entity))
				continue;
			
			if (!cloudHitArea.intersects(entity.getBoundingBox()))
				continue;
			
			// check if the enemy is visible from the sky
			Vec3 from = entity.position();
			Vec3 to = new Vec3(entity.getX(), getY(), entity.getZ());
			boolean collided = BlockGetter.traverseBlocks(from, to, null, (no, pos) ->
				{
					if (InkBlockUtils.canInkPassthrough(level(), pos))
						return null;
					
					BlockState state = level().getBlockState(pos);
					VoxelShape shape = state.getCollisionShape(level(), pos);
					BlockHitResult result = shape.clip(from, to, pos);
					if (result == null || result.getType() == HitResult.Type.MISS)
						return null;
					
					return true;
				}, (no) ->
					false
			);
			if (collided)
				continue;
			
			AABB relativeBox = entity.getBoundingBox().move(position().reverse());
			Triplet<Float, Vector3f, Float> impactData = CommonUtils.getRayDistance(damageDirection, relativeBox);
			
			if (impactData.getA() < getRadius())
			{
				hit(entity, damage);
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
			
			if (SplatcraftGameRules.getLocalizedRule(target.level(), target.blockPosition(), SplatcraftGameRules.ALTERNATIVE_INK_HEALTH))
			{
				if (!target.level().isClientSide())
					WeaponHandler.accumulateAltEnemyInkDamage(livingTarget, getColor(), dmg, false, SplatcraftDamageTypes.INK_SPLAT);
			}
			else
				InkDamageUtils.doDamage(livingTarget, dmg, getOwner(), this, ItemStack.EMPTY, SplatcraftDamageTypes.INK_SPLAT, false, AttackId.NONE);
		}
	}
	private void tickParticleData(int activeTickCount)
	{
		if (cloudParticleRenderData == null)
		{
			cloudParticleRenderData = new ArrayList<>();
		}
		
		if (activeTickCount <= duration - 30)
		{
			cloudParticleRenderData.add(ParticleData.create(random, getRadius()));
			if (ClientUtils.getClient().options.graphicsMode().get() != GraphicsStatus.FAST)
				cloudParticleRenderData.add(ParticleData.create(random, getRadius()));
		}
		
		for (int i = 0; i < cloudParticleRenderData.size(); i++)
		{
			ParticleData data = cloudParticleRenderData.get(i);
			if (!data.tick())
			{
				cloudParticleRenderData.remove(i);
				i--;
			}
		}
	}
	@Override
	public @NotNull AABB getBoundingBoxForCulling()
	{
		float diameter = getRadius() * 2;
		return AABB.ofSize(position(), diameter, getBbHeight(), diameter);
	}
	@Override
	public boolean canHitEntity(@NotNull Entity entity)
	{
		boolean isntOwnerOrSelf = entity != this /*&& entity != getOwner()*/;
		return isntOwnerOrSelf && entity.canBeHitByProjectile() && InkDamageUtils.canDamage(entity, getColor());
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
	public void onVelocityCalculated(Vec3 direction, float speed)
	{
		setDeltaMovement(direction);
	}
	@Override
	protected double getDefaultGravity()
	{
		return 0.08;
	}
	@Override
	public void updateRotation()
	{
		xRotO = getXRot();
		yRotO = getYRot();
	}
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(COLOR, ColorUtils.getDefaultColor());
		builder.define(RADIUS, 1f);
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
	protected void readAdditionalSaveData(@NotNull CompoundTag nbt)
	{
		super.readAdditionalSaveData(nbt);
		moveSpeed = nbt.getFloat("MoveSpeed");
		damage = nbt.getFloat("Damage");
		formationTime = nbt.getByte("FormationTime");
		duration = nbt.getFloat("Duration");
		dropletRadius = nbt.getFloat("DropletPaint");
		dropletFrequency = nbt.getFloat("DropletFrequency");
		dropletCounter = nbt.getFloat("DropletCounter");
		
		setColor(InkColor.getFromNbt(nbt.get("Color")));
		setRadius((nbt.getFloat("Radius")));
		inkType = InkBlockUtils.InkType.CODEC.parse(NbtOps.INSTANCE, nbt.get("InkType")).result().orElse(InkBlockUtils.InkType.NORMAL);
	}
	@Override
	protected void addAdditionalSaveData(@NotNull CompoundTag nbt)
	{
		super.addAdditionalSaveData(nbt);
		nbt.putFloat("MoveSpeed", moveSpeed);
		nbt.putFloat("Damage", damage);
		nbt.putByte("FormationTime", formationTime);
		nbt.putFloat("Duration", duration);
		nbt.putFloat("DropletPaint", dropletRadius);
		nbt.putFloat("DropletFrequency", dropletFrequency);
		nbt.putFloat("DropletCounter", dropletCounter);
		
		nbt.put("Color", getColor().getNbt());
		nbt.putFloat("Radius", getRadius());
		
		if (inkType != null)
			nbt.putString("InkType", inkType.name());
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
	public float getRadius()
	{
		return entityData.get(RADIUS);
	}
	public void setRadius(float radius)
	{
		entityData.set(RADIUS, radius);
	}
	@Override
	public void readExtraData(RegistryFriendlyByteBuf buf)
	{
		moveSpeed = buf.readFloat();
		damage = buf.readFloat();
		formationTime = buf.readByte();
		duration = buf.readFloat();
		dropletRadius = buf.readFloat();
		dropletFrequency = buf.readFloat();
	}
	@Override
	public void writeExtraData(RegistryFriendlyByteBuf buf)
	{
		buf.writeFloat(moveSpeed);
		buf.writeFloat(damage);
		buf.writeByte(formationTime);
		buf.writeFloat(duration);
		buf.writeFloat(dropletRadius);
		buf.writeFloat(dropletFrequency);
	}
	public static final class ParticleData
	{
		public static final byte MAX_LIFESPAN = 64;
		public static final byte LIFESPAN_BITS = (byte) 0b00111111;
		public static final byte SCALE_BITS = (byte) 0b11000000;
		public static final float MIN_SIZE = 0.8f;
		public static final float MAX_SIZE = 1.2f;
		public Vector3f position;
		public Vector3f velocity;
		public byte data;
		private ParticleData(Vector3f position, Vector3f velocity, byte data)
		{
			this.position = position;
			this.velocity = velocity;
			this.data = data;
		}
		public static ParticleData create(RandomSource random, float radius)
		{
			float angle = random.nextFloat() * Mth.TWO_PI;
			float magnitude = random.nextFloat() * radius;
			
			ParticleData data = new ParticleData(
				new Vector3f(Mth.cos(angle) * magnitude, (random.nextFloat() * 2 - 1), Mth.sin(angle) * magnitude),
				new Vector3f(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1).mul(random.nextFloat() * 0.1f),
				(byte) 0
			);
			data.setSizeBits(random.nextInt(4));
			return data;
		}
		public boolean tick()
		{
			position.add(velocity);
			int currentLifespan = getLifespan();
			if (currentLifespan == 63)
				return false;
			setLifespan(currentLifespan + 1);
			return true;
		}
		public void setSize(float size)
		{
			int sizeBits = Mth.clamp((int) (Mth.inverseLerp(size, MIN_SIZE, MAX_SIZE) * 3), 0, 3);
			setSizeBits(sizeBits);
		}
		public void setSizeBits(int bits)
		{
			data = (byte) ((data & LIFESPAN_BITS) | (bits << 6));
		}
		public void setLifespan(int lifespan)
		{
			data = (byte) ((data & SCALE_BITS) | (lifespan & LIFESPAN_BITS));
		}
		public int getLifespan()
		{
			return (data & LIFESPAN_BITS);
		}
		public float getSize()
		{
			return Mth.lerp(((data & SCALE_BITS) >> 6) / 3f, MAX_SIZE, MIN_SIZE);
		}
		@Override
		public String toString()
		{
			return "CloudParticleData[" +
				"position=" + position + ", " +
				"velocity=" + velocity + ", " +
				"data=" + data + ']';
		}
		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof ParticleData that)) return false;
			return data == that.data && Objects.equals(position, that.position) && Objects.equals(velocity, that.velocity);
		}
		@Override
		public int hashCode()
		{
			return Objects.hash(position, velocity, data);
		}
	}
}
