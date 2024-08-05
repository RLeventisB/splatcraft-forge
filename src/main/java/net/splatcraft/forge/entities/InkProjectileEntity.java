package net.splatcraft.forge.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import net.splatcraft.forge.VectorUtils;
import net.splatcraft.forge.blocks.ColoredBarrierBlock;
import net.splatcraft.forge.blocks.StageBarrierBlock;
import net.splatcraft.forge.client.particles.InkExplosionParticleData;
import net.splatcraft.forge.client.particles.InkSplashParticleData;
import net.splatcraft.forge.handlers.DataHandler;
import net.splatcraft.forge.items.weapons.WeaponBaseItem;
import net.splatcraft.forge.items.weapons.settings.*;
import net.splatcraft.forge.registries.SplatcraftEntities;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.registries.SplatcraftSounds;
import net.splatcraft.forge.util.*;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class InkProjectileEntity extends ThrowableItemProjectile implements IColoredEntity
{
	private static final EntityDataAccessor<String> PROJ_TYPE = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Vec2> PROJ_SIZE = SynchedEntityData.defineId(InkProjectileEntity.class, new EntityDataSerializer<Vec2>()
	{
		@Override
		public void write(@NotNull FriendlyByteBuf buf, @NotNull Vec2 vec2)
		{
			buf.writeFloat(vec2.x);
			buf.writeFloat(vec2.y);
		}
		@Override
		public @NotNull Vec2 read(@NotNull FriendlyByteBuf buf)
		{
			return new Vec2(buf.readFloat(), buf.readFloat());
		}
		@Override
		public @NotNull Vec2 copy(@NotNull Vec2 vec2)
		{
			return new Vec2(vec2.x, vec2.y);
		}
	});
	private static final EntityDataAccessor<Float> GRAVITY = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> STRAIGHT_SHOT_TIME = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> HORIZONTAL_DRAG = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Vec3> SHOOT_DIRECTION = SynchedEntityData.defineId(InkProjectileEntity.class, new EntityDataSerializer<Vec3>()
	{
		@Override
		public void write(@NotNull FriendlyByteBuf buf, @NotNull Vec3 vec3)
		{
			buf.writeDouble(vec3.x);
			buf.writeDouble(vec3.y);
			buf.writeDouble(vec3.z);
		}
		@Override
		public @NotNull Vec3 read(@NotNull FriendlyByteBuf buf)
		{
			return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
		}
		@Override
		public @NotNull Vec3 copy(@NotNull Vec3 vec3)
		{
			return new Vec3(vec3.x, vec3.y, vec3.z);
		}
	});
	protected float straightShotTime = -1;
	public int lifespan = 600;
	public boolean explodes = false, bypassMobDamageMultiplier = false, canPierce = false, persistent = false;
	public ItemStack sourceWeapon = ItemStack.EMPTY;
	public float impactCoverage, dropImpactSize, distanceBetweenDrops;
	public float dropSkipDistance;
	public float damageMultiplier = 1;
	public boolean causesHurtCooldown, throwerAirborne;
	public Object[] data = new Object[0];
	public AbstractWeaponSettings damage = ShooterWeaponSettings.DEFAULT;
	public InkBlockUtils.InkType inkType;
	private float accumulatedDrops;
	public InkProjectileEntity(EntityType<InkProjectileEntity> type, Level level)
	{
		super(type, level);
	}
	public InkProjectileEntity(Level level, LivingEntity thrower, int color, InkBlockUtils.InkType inkType, float projectileSize, AbstractWeaponSettings damage, ItemStack sourceWeapon)
	{
		super(SplatcraftEntities.INK_PROJECTILE.get(), thrower, level);
		setColor(color);
		setProjectileSize(projectileSize);
		this.impactCoverage = projectileSize * 0.85f;
		this.throwerAirborne = !thrower.onGround();
		this.damage = damage;
		this.inkType = inkType;
		this.sourceWeapon = sourceWeapon;
	}
	public InkProjectileEntity(Level level, LivingEntity thrower, int color, InkBlockUtils.InkType inkType, float projectileSize, AbstractWeaponSettings damage)
	{
		this(level, thrower, color, inkType, projectileSize, damage, ItemStack.EMPTY);
	}
	public InkProjectileEntity(Level level, LivingEntity thrower, ItemStack sourceWeapon, InkBlockUtils.InkType inkType, float projectileSize, AbstractWeaponSettings damage)
	{
		this(level, thrower, ColorUtils.getInkColor(sourceWeapon), inkType, projectileSize, damage, sourceWeapon);
	}
	public static void registerDataAccessors()
	{
		EntityDataSerializers.registerSerializer(SHOOT_DIRECTION.getSerializer());
		EntityDataSerializers.registerSerializer(PROJ_SIZE.getSerializer());
	}
	public InkProjectileEntity setShooterTrail()
	{
		distanceBetweenDrops = 3;
//		dropSkipDistance = CommonUtils.nextFloat(random, 0, distanceBetweenDrops);
		dropImpactSize = getProjectileSize() * 0.75f;
		return this;
	}
	public InkProjectileEntity setChargerStats(float charge, ChargerWeaponSettings settings)
	{
		data = new Object[] {charge};
		dropImpactSize = settings.projectileInkTrailCoverage;
		distanceBetweenDrops = settings.projectileInkTrailCooldown;
		if (distanceBetweenDrops > 0)
			dropSkipDistance = CommonUtils.nextFloat(random, 0, distanceBetweenDrops);
		lifespan = (int) (settings.minProjectileLifeTicks + (settings.maxProjectileLifeTicks - settings.minProjectileLifeTicks) * charge);
		impactCoverage = settings.projectileInkCoverage;
		
		setGravity(0);
		this.canPierce = charge >= settings.piercesAtCharge;
		setProjectileType(Types.CHARGER);
		return this;
	}
	public InkProjectileEntity setBlasterStats(BlasterWeaponSettings settings)
	{
		setCommonProjectileStats(settings.projectileData);
		data = new Object[]
			{
				settings.blasterData.explosionRadius(),
				settings.blasterData.maxIndirectDamage(),
				settings.blasterData.sparkDamagePenalty(),
				settings.blasterData.explosionPaint(),
			};
		explodes = true;
		setProjectileType(Types.BLASTER);
		return this;
	}
	public InkProjectileEntity setSlosherStats(SlosherWeaponSettings settings)
	{
		dropImpactSize = settings.projectileInkTrailCoverage;
		distanceBetweenDrops = settings.projectileInkTrailCooldown;
		if (distanceBetweenDrops > 0)
			dropSkipDistance = CommonUtils.nextFloat(random, 0, distanceBetweenDrops);
		impactCoverage = settings.projectileInkCoverage;
		
		setProjectileType(Types.SHOOTER);
		return this;
	}
	public InkProjectileEntity setShooterStats(ShooterWeaponSettings settings)
	{
		setCommonProjectileStats(settings.projectileData);
		setProjectileType(Types.SHOOTER);
		return this;
	}
	public InkProjectileEntity setSplatlingStats(SplatlingWeaponSettings settings, float charge)
	{
		CommonRecords.ProjectileDataRecord projectileData = charge > 1 ? settings.secondChargeLevelProjectile : settings.firstChargeLevelProjectile;
		
		setCommonProjectileStats(projectileData);
		setProjectileType(Types.SHOOTER);
		return this;
	}
	public InkProjectileEntity setDualieStats(CommonRecords.ProjectileDataRecord settings)
	{
		
		setCommonProjectileStats(settings);
		setProjectileType(Types.SHOOTER);
		return this;
	}
	public InkProjectileEntity setRollerSwingStats(RollerWeaponSettings settings, float progress)
	{
		setProjectileType(Types.ROLLER);
		
		setGravity(0.15f);
		if (throwerAirborne)
		{
			setStraightShotTime(settings.flingStraightTicks);
			entityData.set(HORIZONTAL_DRAG, settings.flingHorizontalDrag);
		}
		else
		{
			setStraightShotTime(settings.swingStraightTicks);
			entityData.set(HORIZONTAL_DRAG, settings.swingHorizontalDrag);
		}
		
		return this;
	}
	public InkProjectileEntity setCommonProjectileStats(CommonRecords.ProjectileDataRecord settings)
	{
		dropImpactSize = settings.inkDropCoverage();
		distanceBetweenDrops = settings.distanceBetweenInkDrops();
		if (distanceBetweenDrops > 0)
			accumulatedDrops = CommonUtils.nextFloat(random, 0, 1);
		impactCoverage = settings.inkCoverageImpact();
		
		setGravity(settings.gravity());
		setStraightShotTime(settings.straightShotTicks());
		
		lifespan = settings.lifeTicks();
		entityData.set(SPEED, settings.speed());
		entityData.set(HORIZONTAL_DRAG, settings.horizontalDrag());
		
		return this;
	}
	@Override
	protected void defineSynchedData()
	{
		entityData.define(PROJ_TYPE, Types.SHOOTER);
		entityData.define(COLOR, ColorUtils.DEFAULT);
		entityData.define(PROJ_SIZE, new Vec2(0.2F, 0.6F));
		entityData.define(GRAVITY, 0.075F);
		entityData.define(STRAIGHT_SHOT_TIME, 0F);
		entityData.define(SPEED, 0f);
		entityData.define(HORIZONTAL_DRAG, 1F);
		entityData.define(SHOOT_DIRECTION, new Vec3(0, 0, 0));
	}
	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> dataParameter)
	{
		if (dataParameter.equals(PROJ_SIZE))
			refreshDimensions();
		else if (dataParameter.equals(STRAIGHT_SHOT_TIME))
			straightShotTime = entityData.get(STRAIGHT_SHOT_TIME);
		
		super.onSyncedDataUpdated(dataParameter);
	}
	@Override
	protected @NotNull Item getDefaultItem()
	{
		return SplatcraftItems.splattershot.get();
	}
	@Override
	public void tick()
	{
		Vec3 lastPosition = position();
		Vec3 velocity = getShootVelocity();
		setDeltaMovement(velocity);
		super.tick();
		setDeltaMovement(velocity);
		
		if (isInWater())
		{
			discard();
			return;
		}
		
		if (isRemoved())
			return;
		
		if (!level.isClientSide && !persistent && lifespan-- <= 0)
		{
			if (Objects.equals(getProjectileType(), Types.BLASTER) && data.length >= 4)
			{
				InkExplosion.createInkExplosion(getOwner(), position(), (float) data[3], (float) data[0], damage.getMinDamage() * damageMultiplier, (float) data[1] * damageMultiplier, inkType, sourceWeapon);
				createDrop(getX(), getY(), getZ(), 0, (float) data[3]);
				level.broadcastEntityEvent(this, (byte) 3);
				level.playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterExplosion, SoundSource.PLAYERS, 0.8F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
			}
			else
			{
				InkExplosion.createInkExplosion(getOwner(), position(), impactCoverage, 0, 0, inkType, sourceWeapon);
			}
			if (distanceBetweenDrops == 0)
			{
				createDrop(getX(), getY(), getZ(), 0);
			}
			else
			{
				calculateDrops(lastPosition);
			}
			discard();
		}
		else if (dropImpactSize > 0)
		{
			if (!isInvisible())
				level.broadcastEntityEvent(this, (byte) 1);
			if (distanceBetweenDrops == 0)
			{
				createDrop(getX(), getY(), getZ(), 0);
			}
			else
			{
				calculateDrops(lastPosition);
			}
		}
		straightShotTime--;
	}
	private void calculateDrops(Vec3 lastPosition)
	{
		Vec3 deltaMovement = getDeltaMovement();
		float dropsTravelled = (float) deltaMovement.length() / distanceBetweenDrops;
		if (dropSkipDistance > 0)
		{
			if ((dropSkipDistance -= dropsTravelled) > 0)
			{
				dropsTravelled = 0;
			}
			else
			{
				dropsTravelled = -dropSkipDistance;
			}
		}
		if (dropsTravelled > 0)
		{
//			CommonUtils.spawnTestBlockParticle(position(), Blocks.GREEN_WOOL.defaultBlockState());
//			CommonUtils.spawnTestBlockParticle(getPosition(0), Blocks.RED_WOOL.defaultBlockState());
			accumulatedDrops += dropsTravelled;
			while (accumulatedDrops >= 1)
			{
				accumulatedDrops -= 1;
				
				double progress = accumulatedDrops / (dropsTravelled);
				Vec3 dropPos = VectorUtils.lerp(progress, position(), lastPosition);
				
				createDrop(dropPos.x(), dropPos.y(), dropPos.z(), progress);
			}
		}
	}
	public void createDrop(double dropX, double dropY, double dropZ, double extraFrame)
	{
		createDrop(dropX, dropY, dropZ, extraFrame, dropImpactSize);
	}
	public void createDrop(double dropX, double dropY, double dropZ, double extraFrame, float dropImpactSize)
	{
		InkDropEntity proj = new InkDropEntity(level, this, getColor(), inkType, dropImpactSize, sourceWeapon);
		Vec3 velocity = new Vec3(getDeltaMovement().x * 0.7, getDeltaMovement().y - 3.5, getDeltaMovement().z * 0.7);
		velocity = velocity.scale(Math.pow(0.99, extraFrame)).subtract(0, 0.275 * extraFrame, 0).multiply(Math.pow(0.9, extraFrame), 1, Math.pow(0.9, extraFrame));
		Vec3 nextAproximatePos = new Vec3(dropX, dropY, dropZ).add(velocity);
		HitResult hitresult = this.level.clip(new ClipContext(new Vec3(dropX, dropY, dropZ), nextAproximatePos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
		if (hitresult.getType() != HitResult.Type.MISS)
		{
			proj.onHit(hitresult);
			proj.discard();
			return;
		}
		
		if (proj.isRemoved())
			return;
		proj.moveTo(nextAproximatePos);
		proj.shoot(velocity.x, velocity.y, velocity.z, (float) proj.getDeltaMovement().length(), 0.02f);
		level.addFreshEntity(proj);
	}
	private Vec3 getShootVelocity()
	{
		Vec3 speed = entityData.get(SHOOT_DIRECTION).scale(entityData.get(SPEED));
		if (straightShotTime > 0)
			return speed;
		else
		{
			double acumulatedHorizontalDrag = Math.pow(entityData.get(HORIZONTAL_DRAG), -straightShotTime);
			return speed.multiply(acumulatedHorizontalDrag, acumulatedHorizontalDrag, acumulatedHorizontalDrag).subtract(0, getGravity() * -straightShotTime * ((1 + acumulatedHorizontalDrag) / 2), 0);
		}
	}
	@Override
	protected void updateRotation()
	{
		Vec3 motion = getShootVelocity().add(getDeltaMovement());
		
		if (!Vec3.ZERO.equals(motion))
		{
			this.setXRot(lerpRotation(this.xRotO, (float) (Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG)));
			this.setYRot(lerpRotation(this.yRotO, (float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG)));
		}
	}
	@Override
	public void handleEntityEvent(byte id)
	{
		super.handleEntityEvent(id);
		
		switch (id)
		{
			case -1 ->
				level.addParticle(new InkExplosionParticleData(getColor(), .5f), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
			case 1 ->
			{
				if (getProjectileType().equals(Types.CHARGER))
					level.addParticle(new InkSplashParticleData(getColor(), getProjectileSize() * 0.4f), this.getX() - this.getDeltaMovement().x() * 0.25D, this.getY() + getBbHeight() * 0.5f - this.getDeltaMovement().y() * 0.25D, this.getZ() - this.getDeltaMovement().z() * 0.25D, 0, -0.1, 0);
				else
					level.addParticle(new InkSplashParticleData(getColor(), getProjectileSize() * 0.4f), this.getX() - this.getDeltaMovement().x() * 0.25D, this.getY() + getBbHeight() * 0.5f - this.getDeltaMovement().y() * 0.25D, this.getZ() - this.getDeltaMovement().z() * 0.25D, this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z());
			}
			case 2 ->
				level.addParticle(new InkSplashParticleData(getColor(), getProjectileSize() * 2), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
			case 3 ->
				level.addParticle(new InkExplosionParticleData(getColor(), getProjectileSize() * 2), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
		}
	}
	@Override
	protected void onHitEntity(@NotNull EntityHitResult result)
	{
		super.onHitEntity(result);
		
		Entity target = result.getEntity();
		float dmg = damage.calculateDamage(this.tickCount - Math.max(0, straightShotTime), throwerAirborne, data) * damageMultiplier;
		
		if (!level.isClientSide() && target instanceof SpawnShieldEntity && !InkDamageUtils.canDamage(target, this))
		{
			discard();
			level.broadcastEntityEvent(this, (byte) -1);
		}
		
		if (target instanceof LivingEntity livingTarget)
		{
			if (InkDamageUtils.isSplatted(livingTarget)) return;
			
			boolean didDamage = InkDamageUtils.doDamage(livingTarget, dmg, getOwner(), this, sourceWeapon, InkDamageUtils.SPLAT, causesHurtCooldown);
			if (!level.isClientSide && didDamage)
			{
				if ((Objects.equals(getProjectileType(), Types.CHARGER) && (float) data[0] >= 1.0f && InkDamageUtils.isSplatted(livingTarget)) ||
					Objects.equals(getProjectileType(), Types.BLASTER))
				{
					level.playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterDirect, SoundSource.PLAYERS, 0.8F, 1);
				}
			}
		}
		
		if (!canPierce)
		{
			if (explodes && Objects.equals(getProjectileType(), Types.BLASTER) && data.length >= 4)
			{
				InkExplosion.createInkExplosion(getOwner(), result.getLocation(), (float) data[3], (float) data[0], damage.getMinDamage() * damageMultiplier, (float) data[1] * damageMultiplier, inkType, sourceWeapon);
				level.broadcastEntityEvent(this, (byte) 3);
				level.playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterExplosion, SoundSource.PLAYERS, 0.8F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
			}
			else
				level.broadcastEntityEvent(this, (byte) 2);
			
			if (!level.isClientSide)
				discard();
		}
	}
	@Override
	protected void onHitBlock(BlockHitResult result)
	{
		if (InkBlockUtils.canInkPassthrough(level, result.getBlockPos()))
			return;
		
		if (level.getBlockState(result.getBlockPos()).getBlock() instanceof ColoredBarrierBlock coloredBarrierBlock &&
			coloredBarrierBlock.canAllowThrough(result.getBlockPos(), this))
			return;
		
		super.onHitBlock(result);
		
		if (level.getBlockState(result.getBlockPos()).getBlock() instanceof StageBarrierBlock)
			level.broadcastEntityEvent(this, (byte) -1);
		else
		{
			level.broadcastEntityEvent(this, (byte) 2);
			Vec3 impactPos = InkExplosion.adjustPosition(result.getLocation(), result.getDirection().getNormal());
			if (Objects.equals(getProjectileType(), Types.BLASTER) && data.length >= 4)
			{
				damageMultiplier *= (float) data[2];
				InkExplosion.createInkExplosion(getOwner(), impactPos, (float) data[3], (float) data[0], damage.getMinDamage() * damageMultiplier, (float) data[1] * damageMultiplier, inkType, sourceWeapon);
				level.broadcastEntityEvent(this, (byte) 3);
//				level.playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterExplosion, SoundSource.PLAYERS, 0.8F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
			}
			else
			{
				InkExplosion.createInkExplosion(getOwner(), impactPos, impactCoverage, 0, 0, inkType, sourceWeapon);
			}
		}
		if (!level.isClientSide)
			this.discard();
	}
	@Override
	public void shootFromRotation(@NotNull Entity thrower, float pitch, float yaw, float pitchOffset, float velocity, float inaccuracy)
	{
		float f = -Mth.sin(yaw * (Mth.DEG_TO_RAD)) * Mth.cos(pitch * (Mth.DEG_TO_RAD));
		float f1 = -Mth.sin((pitch + pitchOffset) * (Mth.DEG_TO_RAD));
		float f2 = Mth.cos(yaw * (Mth.DEG_TO_RAD)) * Mth.cos(pitch * (Mth.DEG_TO_RAD));
		this.shoot(f, f1, f2, velocity, inaccuracy);
		
		createDrop(getX(), getY(), getZ(), 0, 1.5f); // ik that in splatoon weapons don't always paint their feet but that's such a bs mechanic so why not
	}
	@Override
	public void shoot(double x, double y, double z, float velocity, float inaccuracy)
	{
		double usedInaccuracy = inaccuracy * 0.0075;
		Vec3 vec3 = (new Vec3(x, y, z)).normalize().add(this.random.nextGaussian() * usedInaccuracy, this.random.nextGaussian() * usedInaccuracy, this.random.nextGaussian() * usedInaccuracy).normalize();
		
		entityData.set(SHOOT_DIRECTION, vec3);
		vec3 = vec3.scale(velocity);
		
		double d0 = vec3.horizontalDistance();
		this.setYRot((float) (Mth.atan2(vec3.x, vec3.z) * Mth.RAD_TO_DEG));
		this.setXRot((float) (Mth.atan2(vec3.y, d0) * Mth.RAD_TO_DEG));
		this.yRotO = this.getYRot();
		this.xRotO = this.getXRot();
		
		entityData.set(SPEED, velocity);
	}
	@Override
	protected void onHit(HitResult result)
	{
		HitResult.Type rayType = result.getType();
		if (rayType == HitResult.Type.ENTITY)
		{
			this.onHitEntity((EntityHitResult) result);
		}
		else if (rayType == HitResult.Type.BLOCK)
		{
			onHitBlock((BlockHitResult) result);
		}
	}
	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag nbt)
	{
		super.readAdditionalSaveData(nbt);
		
		if (nbt.contains("Size"))
			setProjectileSize(nbt.getFloat("Size"));
		if (nbt.contains("VisualSize"))
			setProjectileVisualSize(nbt.getFloat("VisualSize"));
		
		impactCoverage = nbt.contains("ImpactCoverage") ? nbt.getFloat("ImpactCoverage") : getProjectileSize() * 0.85f;
		
		if (nbt.contains("Color"))
			setColor(ColorUtils.getColorFromNbt(nbt));
		
		entityData.set(SPEED, nbt.getFloat("Speed"));
		entityData.set(HORIZONTAL_DRAG, nbt.getFloat("HorizontalDrag"));
		
		if (nbt.contains("Gravity"))
			setGravity(nbt.getFloat("Gravity"));
		if (nbt.contains("Lifespan"))
			lifespan = nbt.getInt("Lifespan");
		if (nbt.contains("StraightShotTime"))
			setStraightShotTime(nbt.getFloat("StraightShotTime"));
		if (nbt.contains("MaxStraightShotTime"))
			straightShotTime = (nbt.getFloat("StraightShotTime"));
		
		ListTag directionTag = nbt.getList("Direction", DoubleTag.TAG_DOUBLE);
		entityData.set(SHOOT_DIRECTION, new Vec3(directionTag.getDouble(0), directionTag.getDouble(1), directionTag.getDouble(2)));
		
		distanceBetweenDrops = nbt.getFloat("TrailFrequency");
		dropImpactSize = nbt.getFloat("TrailSize");
		bypassMobDamageMultiplier = nbt.getBoolean("BypassMobDamageMultiplier");
		canPierce = nbt.getBoolean("CanPierce");
		explodes = nbt.getBoolean("Explodes");
		persistent = nbt.getBoolean("Persistent");
		causesHurtCooldown = nbt.getBoolean("CausesHurtCooldown");
		
		setInvisible(nbt.getBoolean("Invisible"));
		
		String type = nbt.getString("ProjectileType");
		setProjectileType(type.isEmpty() ? Types.DEFAULT : type);
		inkType = InkBlockUtils.InkType.values.getOrDefault(new ResourceLocation(nbt.getString("InkType")), InkBlockUtils.InkType.NORMAL);
		
		sourceWeapon = ItemStack.of(nbt.getCompound("SourceWeapon"));
		
		AbstractWeaponSettings<?, ?> settings = DataHandler.WeaponStatsListener.SETTINGS.get(new ResourceLocation(nbt.getString(nbt.getString("Settings"))));
		if (settings != null)
			damage = settings;
		else if (sourceWeapon.getItem() instanceof WeaponBaseItem<?> weapon)
			damage = weapon.getSettings(sourceWeapon);
	}
	@Override
	public void addAdditionalSaveData(CompoundTag nbt)
	{
		nbt.putFloat("Size", getProjectileSize());
		nbt.putFloat("VisualSize", getProjectileVisualSize());
		nbt.putInt("Color", getColor());
		
		nbt.putFloat("Speed", entityData.get(SPEED));
		nbt.putFloat("HorizontalDrag", entityData.get(HORIZONTAL_DRAG));
		nbt.putFloat("MaxStraightShotTime", getMaxStraightShotTime());
		nbt.putFloat("StraightShotTime", straightShotTime);
		
		ListTag directionTag = new ListTag();
		Vec3 direction = getShotDirection();
		directionTag.add(DoubleTag.valueOf(direction.x));
		directionTag.add(DoubleTag.valueOf(direction.y));
		directionTag.add(DoubleTag.valueOf(direction.z));
		nbt.put("Direction", directionTag);
		
		nbt.putFloat("Gravity", getGravity());
		nbt.putInt("Lifespan", lifespan);
		nbt.putFloat("TrailSize", dropImpactSize);
		nbt.putFloat("TrailFrequency", distanceBetweenDrops);
		nbt.putBoolean("BypassMobDamageMultiplier", bypassMobDamageMultiplier);
		nbt.putBoolean("CanPierce", canPierce);
		nbt.putBoolean("Explodes", explodes);
		nbt.putBoolean("Persistent", persistent);
		nbt.putBoolean("CausesHurtCooldown", causesHurtCooldown);
		
		nbt.putBoolean("Invisible", isInvisible());
		
		nbt.putString("ProjectileType", getProjectileType());
		nbt.putString("InkType", inkType.getSerializedName());
		nbt.put("SourceWeapon", sourceWeapon.save(new CompoundTag()));
		
		super.addAdditionalSaveData(nbt);
		nbt.remove("Item");
	}
	@Deprecated //Modify sourceWeapon variable instead
	@Override
	public void setItem(@NotNull ItemStack itemStack)
	{
	}
	@Override
	protected @NotNull ItemStack getItemRaw()
	{
		return sourceWeapon;
	}
	@Override
	public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket()
	{
		return NetworkHooks.getEntitySpawningPacket(this);
	}
	@Override
	public @NotNull EntityDimensions getDimensions(@NotNull Pose pose)
	{
		return super.getDimensions(pose).scale(getProjectileSize() / 2f);
	}
	@Override
	public float getGravity()
	{
		return entityData.get(GRAVITY);
	}
	public void setGravity(float gravity)
	{
		entityData.set(GRAVITY, gravity);
	}
	public float getStraightShotTime()
	{
		return straightShotTime;
	}
	public float getMaxStraightShotTime()
	{
		return entityData.get(STRAIGHT_SHOT_TIME);
	}
	public void setStraightShotTime(float time)
	{
		entityData.set(STRAIGHT_SHOT_TIME, time);
	}
	public Vec3 getShotDirection()
	{
		return entityData.get(SHOOT_DIRECTION);
	}
	@Override
	public boolean isNoGravity()
	{
		return straightShotTime > 0 || getGravity() == 0 || super.isNoGravity();
	}
	public float getProjectileSize()
	{
		return entityData.get(PROJ_SIZE).x;
	}
	public float getProjectileVisualSize()
	{
		return entityData.get(PROJ_SIZE).y;
	}
	public void setProjectileVisualSize(float visualSize)
	{
		entityData.set(PROJ_SIZE, new Vec2(getProjectileSize(), visualSize));
	}
	public void setProjectileSize(float size)
	{
		entityData.set(PROJ_SIZE, new Vec2(size, size * 3));
		reapplyPosition();
		refreshDimensions();
	}
	@Override
	public @NotNull ItemStack getItem()
	{
		return ItemStack.EMPTY;
	}
	@Override
	public int getColor()
	{
		return entityData.get(COLOR);
	}
	@Override
	public void setColor(int color)
	{
		entityData.set(COLOR, color);
	}
	public String getProjectileType()
	{
		return entityData.get(PROJ_TYPE);
	}
	public void setProjectileType(String v)
	{
		entityData.set(PROJ_TYPE, v);
	}
	public RandomSource getRandom()
	{
		return random;
	}
	public static class Types
	{
		public static final String DEFAULT = "default";
		public static final String SHOOTER = "shooter";
		public static final String CHARGER = "charger";
		public static final String ROLLER = "roller";
		public static final String BLASTER = "blaster";
	}
}
