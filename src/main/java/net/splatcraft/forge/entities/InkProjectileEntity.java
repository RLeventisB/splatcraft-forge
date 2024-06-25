package net.splatcraft.forge.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.splatcraft.forge.blocks.ColoredBarrierBlock;
import net.splatcraft.forge.blocks.StageBarrierBlock;
import net.splatcraft.forge.client.particles.InkExplosionParticleData;
import net.splatcraft.forge.client.particles.InkSplashParticleData;
import net.splatcraft.forge.handlers.DataHandler;
import net.splatcraft.forge.items.weapons.SplatlingItem;
import net.splatcraft.forge.items.weapons.WeaponBaseItem;
import net.splatcraft.forge.items.weapons.settings.*;
import net.splatcraft.forge.registries.SplatcraftEntities;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.registries.SplatcraftSounds;
import net.splatcraft.forge.util.ColorUtils;
import net.splatcraft.forge.util.InkBlockUtils;
import net.splatcraft.forge.util.InkDamageUtils;
import net.splatcraft.forge.util.InkExplosion;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class InkProjectileEntity extends ThrowableItemProjectile implements IColoredEntity
{
	private static final EntityDataAccessor<String> PROJ_TYPE = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> PROJ_SIZE = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> GRAVITY = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> STRAIGHT_SHOT_TIME = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> HORIZONTAL_DRAG = SynchedEntityData.defineId(InkProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Vec3> SHOOT_DIRECTION = SynchedEntityData.defineId(InkProjectileEntity.class, new EntityDataSerializer<Vec3>()
	{
		@Override
		public void write(@NotNull FriendlyByteBuf buf, @NotNull Vec3 shootVelocity)
		{
			buf.writeDouble(shootVelocity.x);
			buf.writeDouble(shootVelocity.y);
			buf.writeDouble(shootVelocity.z);
		}
		@Override
		public @NotNull Vec3 read(@NotNull FriendlyByteBuf buf)
		{
			return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
		}
		@Override
		public @NotNull Vec3 copy(@NotNull Vec3 shootVelocity)
		{
			return new Vec3(shootVelocity.x, shootVelocity.y, shootVelocity.z);
		}
	});
	protected int straightShotTime = -1;
	public int lifespan = 600;
	public boolean explodes = false, bypassMobDamageMultiplier = false, canPierce = false, persistent = false;
	public ItemStack sourceWeapon = ItemStack.EMPTY;
	public float impactCoverage;
	public float trailSize = 0;
	public int trailCooldown = 0, trailOffset = 0;
	public String damageType = "splat";
	public float damageMultiplier = 1;
	public boolean causesHurtCooldown = false;
	public boolean throwerAirborne = false;
	public float charge;
	public boolean isOnRollCooldown = false;
	public AbstractWeaponSettings damage = ShooterWeaponSettings.DEFAULT;
	public InkBlockUtils.InkType inkType;
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
		this.throwerAirborne = !thrower.isOnGround();
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
	}
	public InkProjectileEntity setShooterTrail()
	{
		trailCooldown = 4;
		trailOffset = random.nextInt(0, 4);
		trailSize = getProjectileSize() * 0.75f;
		return this;
	}
	public InkProjectileEntity setChargerStats(float charge, ChargerWeaponSettings settings)
	{
		this.charge = charge;
		trailSize = settings.projectileInkTrailCoverage;
		trailCooldown = settings.projectileInkTrailCooldown;
		if (trailCooldown > 0)
			trailOffset = random.nextInt(0, trailCooldown);
		lifespan = (int) (settings.minProjectileLifeTicks + (settings.maxProjectileLifeTicks - settings.minProjectileLifeTicks) * charge);
		impactCoverage = settings.projectileInkCoverage;
		
		setGravity(0);
		this.canPierce = charge >= settings.piercesAtCharge;
		setProjectileType(Types.CHARGER);
		return this;
	}
	public InkProjectileEntity setBlasterStats(BlasterWeaponSettings settings)
	{
		this.lifespan = settings.projectileData.lifeTicks();
		setGravity(0);
		trailSize = settings.projectileData.inkTrailCoverage();
		trailCooldown = settings.projectileData.inkTrailCooldown();
		if (trailCooldown > 0)
			trailOffset = random.nextInt(0, trailCooldown);
		impactCoverage = settings.blasterData.explosionRadius();
		
		explodes = true;
		setProjectileType(Types.BLASTER);
		return this;
	}
	public InkProjectileEntity setSlosherStats(SlosherWeaponSettings settings)
	{
		trailSize = settings.projectileInkTrailCoverage;
		trailCooldown = settings.projectileInkTrailCooldown;
		if (trailCooldown > 0)
			trailOffset = random.nextInt(0, trailCooldown);
		impactCoverage = settings.projectileInkCoverage;
		
		setProjectileType(Types.SHOOTER);
		return this;
	}
	public InkProjectileEntity setShooterStats(ShooterWeaponSettings settings)
	{
		trailSize = settings.projectileData.inkTrailCoverage();
		trailCooldown = settings.projectileData.inkTrailCooldown();
		if (trailCooldown > 0)
			trailOffset = random.nextInt(0, trailCooldown);
		impactCoverage = settings.projectileData.inkCoverageImpact();
		
		setGravity(settings.projectileData.gravity());
		setStraightShotTime(settings.projectileData.straightShotTicks());
		
		lifespan = settings.projectileData.lifeTicks();
		entityData.set(SPEED, settings.projectileData.speed());
		entityData.set(HORIZONTAL_DRAG, settings.projectileData.horizontalDrag());
		setProjectileType(Types.SHOOTER);
		return this;
	}
	public InkProjectileEntity setSplatlingStats(SplatlingWeaponSettings settings, float charge)
	{
		CommonRecords.ProjectileDataRecord projectileData = charge > 1 ? settings.secondChargeLevelProjectile : settings.firstChargeLevelProjectile;
		
		trailSize = projectileData.inkTrailCoverage();
		trailCooldown = projectileData.inkTrailCooldown();
		if (trailCooldown > 0)
			trailOffset = random.nextInt(0, trailCooldown);
		impactCoverage = projectileData.inkCoverageImpact();
		
		setGravity(SplatlingItem.getScaledProjectileSettingFloat(settings, charge, CommonRecords.ProjectileDataRecord::gravity));
		setStraightShotTime(SplatlingItem.getScaledProjectileSettingInt(settings, charge, CommonRecords.ProjectileDataRecord::straightShotTicks));
		
		lifespan = SplatlingItem.getScaledProjectileSettingInt(settings, charge, CommonRecords.ProjectileDataRecord::lifeTicks);
		entityData.set(SPEED, SplatlingItem.getScaledProjectileSettingFloat(settings, charge, CommonRecords.ProjectileDataRecord::speed));
		entityData.set(HORIZONTAL_DRAG, 0.9f);
		setProjectileType(Types.SHOOTER);
		return this;
	}
	public InkProjectileEntity setDualieStats(CommonRecords.ProjectileDataRecord firingData)
	{
		trailSize = firingData.inkTrailCoverage();
		trailCooldown = firingData.inkTrailCooldown();
		if (trailCooldown > 0)
			trailOffset = random.nextInt(0, trailCooldown);
		impactCoverage = firingData.inkCoverageImpact();
		
		setGravity(firingData.gravity());
		setStraightShotTime(firingData.straightShotTicks());
		
		lifespan = firingData.lifeTicks();
		entityData.set(SPEED, firingData.speed());
		entityData.set(HORIZONTAL_DRAG, firingData.horizontalDrag());
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
	@Override
	protected void defineSynchedData()
	{
		entityData.define(PROJ_TYPE, Types.SHOOTER);
		entityData.define(COLOR, ColorUtils.DEFAULT);
		entityData.define(PROJ_SIZE, 1.0f);
		entityData.define(GRAVITY, 0.075f);
		entityData.define(STRAIGHT_SHOT_TIME, 0);
		entityData.define(SPEED, 0f);
		entityData.define(HORIZONTAL_DRAG, 1f);
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
		setDeltaMovement(getShootVelocity());
		super.tick();
//		setDeltaMovement(getDeltaMovement().subtract(getShootVelocity().scale(0.99)));
		
		straightShotTime--;
		
		if (isInWater())
		{
			discard();
			return;
		}
		
		if (isRemoved())
			return;
		
		if (!level.isClientSide && !persistent && lifespan-- <= 0)
		{
			float dmg = damage.calculateDamage(this.tickCount - Math.max(0, straightShotTime), throwerAirborne, charge, isOnRollCooldown) * damageMultiplier;
			InkExplosion.createInkExplosion(getOwner(), blockPosition(), impactCoverage, dmg, explodes ? damage.getMinDamage() : dmg, true, inkType, sourceWeapon);
			if (explodes)
			{
				level.broadcastEntityEvent(this, (byte) 3);
				level.playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterExplosion, SoundSource.PLAYERS, 0.8F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
			}
			discard();
		}
		else if (trailSize > 0 && (trailCooldown == 0 || (tickCount + trailOffset) % trailCooldown == 0))
		{
			if (!isInvisible())
				level.broadcastEntityEvent(this, (byte) 1);
			if (!level.isClientSide)
			{
				InkDropEntity proj = new InkDropEntity(level, this, getColor(), inkType, trailSize, sourceWeapon);
				proj.shoot(getDeltaMovement().x, Math.min(getDeltaMovement().y, 0) - 2.5, getDeltaMovement().z, (float) proj.getDeltaMovement().length(), 0.2f);
				level.addFreshEntity(proj);
			}
			InkExplosion.createInkExplosion(getOwner(), blockPosition(), trailSize, 0, 0, true, inkType, sourceWeapon);
		}
	}
	private Vec3 getShootVelocity()
	{
		if (straightShotTime >= 0)
			return entityData.get(SHOOT_DIRECTION);
		else
		{
			double acumulatedHorizontalDrag = Math.pow(entityData.get(HORIZONTAL_DRAG), -straightShotTime);
			return entityData.get(SHOOT_DIRECTION).scale(entityData.get(SPEED)).multiply(acumulatedHorizontalDrag, acumulatedHorizontalDrag, acumulatedHorizontalDrag).subtract(0, getGravity() * -straightShotTime * ((1 + acumulatedHorizontalDrag) / 2), 0);
		}
	}
	@Override
	protected void updateRotation()
	{
		Vec3 motion = getShootVelocity().add(getDeltaMovement());
		
		if (!Vec3.ZERO.equals(motion))
		{
			this.setXRot(lerpRotation(this.xRotO, (float) (Mth.atan2(motion.y, motion.horizontalDistance()) * (double) (180F / (float) Math.PI))));
			this.setYRot(lerpRotation(this.yRotO, (float) (Mth.atan2(motion.x, motion.z) * (double) (180F / (float) Math.PI))));
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
					level.addParticle(new InkSplashParticleData(getColor(), getProjectileSize()), this.getX() - this.getDeltaMovement().x() * 0.25D, this.getY() + getBbHeight() * 0.5f - this.getDeltaMovement().y() * 0.25D, this.getZ() - this.getDeltaMovement().z() * 0.25D, 0, -0.1, 0);
				else
					level.addParticle(new InkSplashParticleData(getColor(), getProjectileSize()), this.getX() - this.getDeltaMovement().x() * 0.25D, this.getY() + getBbHeight() * 0.5f - this.getDeltaMovement().y() * 0.25D, this.getZ() - this.getDeltaMovement().z() * 0.25D, this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z());
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
		float dmg = damage.calculateDamage(this.tickCount - Math.max(0, straightShotTime), throwerAirborne, charge, isOnRollCooldown) * damageMultiplier;
		
		if (!level.isClientSide() && target instanceof SpawnShieldEntity && !InkDamageUtils.canDamage(target, this))
		{
			discard();
			level.broadcastEntityEvent(this, (byte) -1);
		}
		
		if (target instanceof LivingEntity livingTarget)
		{
			if (InkDamageUtils.isSplatted(livingTarget)) return;
			
			if (InkDamageUtils.doDamage(livingTarget, dmg, getOwner(), this, sourceWeapon, damageType, causesHurtCooldown) &&
				InkDamageUtils.isSplatted(livingTarget) && charge >= 1.0f && getOwner() instanceof ServerPlayer serverPlayer)
				serverPlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.ARROW_HIT_PLAYER, 0.0F));
		}
		
		if (!canPierce)
		{
			if (explodes)
			{
				InkExplosion.createInkExplosion(getOwner(), blockPosition(), impactCoverage, damage.getMinDamage(), dmg, true, inkType, sourceWeapon);
				level.broadcastEntityEvent(this, (byte) 3);
				level.playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterDirect, SoundSource.PLAYERS, 0.8F, 1);
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
		
		float dmg = damage.calculateDamage(this.tickCount - Math.max(0, straightShotTime), throwerAirborne, charge, isOnRollCooldown) * damageMultiplier;
		InkExplosion.createInkExplosion(getOwner(), blockPosition(), impactCoverage, dmg, explodes ? damage.getMinDamage() : 0, true, inkType, sourceWeapon);
		if (explodes)
		{
			level.broadcastEntityEvent(this, (byte) 3);
			level.playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterExplosion, SoundSource.PLAYERS, 0.8F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
		}
		else if (level.getBlockState(result.getBlockPos()).getBlock() instanceof StageBarrierBlock)
			level.broadcastEntityEvent(this, (byte) -1);
		else level.broadcastEntityEvent(this, (byte) 2);
		if (!level.isClientSide)
			this.discard();
	}
	@Override
	public void shootFromRotation(@NotNull Entity thrower, float pitch, float yaw, float pitchOffset, float velocity, float inaccuracy)
	{
		float f = -Mth.sin(yaw * ((float) Math.PI / 180F)) * Mth.cos(pitch * ((float) Math.PI / 180F));
		float f1 = -Mth.sin((pitch + pitchOffset) * ((float) Math.PI / 180F));
		float f2 = Mth.cos(yaw * ((float) Math.PI / 180F)) * Mth.cos(pitch * ((float) Math.PI / 180F));
		this.shoot(f, f1, f2, velocity, inaccuracy);
		
		InkExplosion.createInkExplosion(getOwner(), thrower.blockPosition(), 0.75f, 0, 0, true, inkType, sourceWeapon);
	}
	@Override
	public void shoot(double x, double y, double z, float velocity, float inaccuracy)
	{
		double usedInaccuracy = inaccuracy * 0.0075;
		Vec3 vec3 = (new Vec3(x, y, z)).normalize().add(this.random.nextGaussian() * usedInaccuracy, this.random.nextGaussian() * usedInaccuracy, this.random.nextGaussian() * usedInaccuracy);
		
		entityData.set(SHOOT_DIRECTION, vec3.normalize());
		
		double d0 = vec3.horizontalDistance();
		this.setYRot((float) (Mth.atan2(vec3.x, vec3.z) * (180.0 / Math.PI)));
		this.setXRot((float) (Mth.atan2(vec3.y, d0) * (180.0 / Math.PI)));
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
			setStraightShotTime(nbt.getInt("StraightShotTime"));
		if (nbt.contains("MaxStraightShotTime"))
			straightShotTime = (nbt.getInt("StraightShotTime"));
		
		ListTag directionTag = nbt.getList("Direction", DoubleTag.TAG_DOUBLE);
		entityData.set(SHOOT_DIRECTION, new Vec3(directionTag.getDouble(0), directionTag.getDouble(1), directionTag.getDouble(2)));
		
		trailCooldown = nbt.getInt("TrailCooldown");
		trailSize = nbt.getFloat("TrailSize");
		bypassMobDamageMultiplier = nbt.getBoolean("BypassMobDamageMultiplier");
		canPierce = nbt.getBoolean("CanPierce");
		explodes = nbt.getBoolean("Explodes");
		persistent = nbt.getBoolean("Persistent");
		causesHurtCooldown = nbt.getBoolean("CausesHurtCooldown");
		damageType = nbt.getString("DamageType");
		
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
		nbt.putInt("Color", getColor());
		
		nbt.putFloat("Speed", entityData.get(SPEED));
		nbt.putFloat("HorizontalDrag", entityData.get(HORIZONTAL_DRAG));
		nbt.putInt("MaxStraightShotTime", getMaxStraightShotTime());
		nbt.putInt("StraightShotTime", straightShotTime);
		
		ListTag directionTag = new ListTag();
		Vec3 direction = getShotDirection();
		directionTag.add(DoubleTag.valueOf(direction.x));
		directionTag.add(DoubleTag.valueOf(direction.y));
		directionTag.add(DoubleTag.valueOf(direction.z));
		nbt.put("Direction", directionTag);
		
		nbt.putFloat("Gravity", getGravity());
		nbt.putInt("Lifespan", lifespan);
		nbt.putFloat("TrailSize", trailSize);
		nbt.putInt("TrailCooldown", trailCooldown);
		nbt.putBoolean("BypassMobDamageMultiplier", bypassMobDamageMultiplier);
		nbt.putBoolean("CanPierce", canPierce);
		nbt.putBoolean("Explodes", explodes);
		nbt.putBoolean("Persistent", persistent);
		nbt.putBoolean("CausesHurtCooldown", causesHurtCooldown);
		
		nbt.putBoolean("Invisible", isInvisible());
		
		nbt.putString("DamageType", damageType);
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
	public @NotNull Packet<?> getAddEntityPacket()
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
	public int getStraightShotTime()
	{
		return straightShotTime;
	}
	public int getMaxStraightShotTime()
	{
		return entityData.get(STRAIGHT_SHOT_TIME);
	}
	public void setStraightShotTime(int time)
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
		return entityData.get(PROJ_SIZE);
	}
	public void setProjectileSize(float size)
	{
		entityData.set(PROJ_SIZE, size);
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
	public Random getRandom()
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
