package net.splatcraft.entities.subs;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.client.particles.InkSplashParticleData;
import net.splatcraft.entities.ObjectCollideListenerEntity;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.*;

import java.util.List;

import static net.splatcraft.items.weapons.settings.SubWeaponRecords.CurlingBombDataRecord;

public class CurlingBombEntity extends AbstractSubWeaponEntity<CurlingBombDataRecord> implements ObjectCollideListenerEntity
{
	private static final TrackedData<Integer> INIT_FUSE_TIME = DataTracker.registerData(CurlingBombEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Float> COOK_SCALE = DataTracker.registerData(CurlingBombEntity.class, TrackedDataHandlerRegistry.FLOAT);
	public int fuseTime = 0;
	public int prevFuseTime = 0;
	public float bladeRot = 0;
	public float prevBladeRot = 0;
	private boolean playedActivationSound = false;
	public CurlingBombEntity(EntityType<? extends AbstractSubWeaponEntity<CurlingBombDataRecord>> type, World world)
	{
		super(type, world);
	}
	@Override
	public float getStepHeight()
	{
		return .7f;
	}
	@Override
	protected void initDataTracker(DataTracker.Builder builder)
	{
		super.initDataTracker(builder);
		builder.add(INIT_FUSE_TIME, 0);
		builder.add(COOK_SCALE, 0f);
	}
	@Override
	protected Item getDefaultItem()
	{
		return SplatcraftItems.curlingBomb.get();
	}
	@Override
	public void tick()
	{
		super.tick();
		
		SubWeaponSettings<CurlingBombDataRecord> settings = getSettings();
		
		double spd = getVelocity().horizontalLength();
		prevBladeRot = bladeRot;
		bladeRot += (float) spd;
		
		prevFuseTime = fuseTime;
		fuseTime--;
		
		CurlingBombDataRecord curlingData = settings.subDataRecord;
		boolean slowingDown = fuseTime <= curlingData.warningFrame();
		if (slowingDown && !playedActivationSound)
		{
			getWorld().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonating, SoundCategory.PLAYERS, 0.8F, 1f);
			playedActivationSound = true;
		}
		
		if (!getWorld().isClient())
		{
			doTrail(spd > 1.0E-3, settings);
		}
		float horizontalFriction = 1f;
		if (isOnGround())
			horizontalFriction = getWorld().getBlockState(CommonUtils.createBlockPos(getX(), getY() - 1.0D, getZ())).getBlock().getSlipperiness() / 0.6f;
		if (slowingDown)
		{
			horizontalFriction *= 0.8f;
		}
		horizontalFriction = MathHelper.clamp(horizontalFriction, 0, 1);
		setVelocity(getVelocity().multiply(horizontalFriction, 1f, horizontalFriction));
		
		if (fuseTime <= 0)
		{
			Vec3d center = getBoundingBox().getCenter();
			explode(curlingData, center);
		}
		else if (spd > 0.01 && fuseTime % (int) Math.max(1, (1 - spd) * 10) == 0)
		{
			getWorld().sendEntityStatus(this, (byte) 2);
		}
		
		move(MovementType.SELF, getVelocity());
	}
	public void explode(CurlingBombDataRecord settings, Vec3d impactPos)
	{
		if (!getWorld().isClient())
		{
			InkExplosion.createInkExplosion(getOwner(), impactPos, settings.inkExplosionRange().getValue(getCookProgress()), settings.damageRanges().withShift(-getCookProgress() * settings.maxCookRadiusBonus()), inkType, sourceWeapon, AttackId.NONE);
			InkExplosion.doSplashes(getOwner(), impactPos, settings.inkSplashes(), getColor(), inkType);
			getWorld().sendEntityStatus(this, (byte) 1);
			discard();
		}
		getWorld().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonate, SoundCategory.PLAYERS, 0.8F, CommonUtils.nextTriangular(getWorld().getRandom(), 0.95F, 0.095F));
	}
	@Override
	public void handleMovement()
	{
	}
	@Override
	public float getFriction()
	{
		return 1;
	}
	private void doTrail(boolean fastEnough, SubWeaponSettings<CurlingBombDataRecord> settings)
	{
		float trailWidth = settings.subDataRecord.trailSizeRange().getValue(getCookProgress());
		float trailStep = CommonUtils.calculateStep(trailWidth, 0.7071067811865475f);
		if (fastEnough)
		{
			for (float j = -trailWidth; j <= trailWidth; j += trailStep)
			{
				Vec3d normalized = getVelocity().multiply(1, 0, 1).normalize();
				double sideX = -normalized.z;
				double sideZ = normalized.x;
				for (int i = 0; i <= 2; i++)
				{
					BlockPos side = CommonUtils.createBlockPos(Math.floor(getX() + sideX * j), getBlockY() - i, Math.floor(getZ() + sideZ * j));
					if (InkBlockUtils.canInkFromFace(getWorld(), side, Direction.UP))
					{
						InkBlockUtils.inkBlock(getOwner(), getWorld(), side, getColor(), Direction.UP, inkType, settings.subDataRecord.contactDamage());
						break;
					}
				}
			}
		}
		else
		{
			for (int i = 0; i <= 2; i++)
				if (InkBlockUtils.canInkFromFace(getWorld(), getBlockPos().down(i), Direction.UP))
				{
					InkBlockUtils.inkBlock(getOwner(), getWorld(), getBlockPos().down(i), getColor(), Direction.UP, inkType, settings.subDataRecord.contactDamage());
					break;
				}
		}
	}
	@Override
	public void handleStatus(byte id)
	{
		super.handleStatus(id);
		float maxDistance = getSettings().subDataRecord.damageRanges().getMaxDistance();
		if (id == 1)
		{
			getWorld().addImportantParticle(new InkExplosionParticleData(getColor(), (maxDistance + getCookProgress()) * 2), getX(), getY(), getZ(), 0, 0, 0);
		}
		if (id == 2)
		{
			getWorld().addParticle(new InkSplashParticleData(getColor(), 1.175f), getX(), getY() + 0.4, getZ(), 0, 0, 0);
		}
	}
	//Ripped and modified from Minestuck's BouncingProjectileEntity class (with permission)
	@Override
	protected void onEntityHit(EntityHitResult result)
	{
		if (result.getEntity() instanceof LivingEntity livingEntity)
		{
			InkDamageUtils.doRollDamage(livingEntity, getSettings().subDataRecord.contactDamage(), getOwner(), this, sourceWeapon);
		}
		
		double velocityX = getVelocity().x;
		double velocityY = getVelocity().y;
		double velocityZ = getVelocity().z;
		double absVelocityX = Math.abs(velocityX);
		double absVelocityY = Math.abs(velocityY);
		double absVelocityZ = Math.abs(velocityZ);
		
		if (absVelocityX >= absVelocityY && absVelocityX >= absVelocityZ)
			setVelocity(-velocityX, velocityY, velocityZ);
		if (absVelocityY >= .05 && absVelocityY >= absVelocityX && absVelocityY >= absVelocityZ)
			setVelocity(velocityX, -velocityY * .5, velocityZ);
		if (absVelocityZ >= absVelocityY && absVelocityZ >= absVelocityX)
			setVelocity(velocityX, velocityY, -velocityZ);
	}
	@Override
	protected void onBlockHit(BlockHitResult result)
	{
		Vec3d velocity = getVelocity().add(0, getGravity(), 0);
		if (canStepUp(velocity))
			return;
		
		double velocityX = velocity.x;
		double velocityY = velocity.y;
		double velocityZ = velocity.z;
		
		Direction blockFace = result.getSide();
		
		if (getWorld().getBlockState(result.getBlockPos()).getCollisionShape(getWorld(), result.getBlockPos()).getBoundingBox().maxY - (getBlockPos().getY() - getPos().getY()) < .7f)
			return;
		
		if (blockFace == Direction.EAST || blockFace == Direction.WEST)
			setVelocity(-velocityX, velocityY, velocityZ);
		if (Math.abs(velocityY) >= 0.05 && (blockFace == Direction.DOWN))
			setVelocity(velocityX, -velocityY * .5, velocityZ);
		if (blockFace == Direction.NORTH || blockFace == Direction.SOUTH)
			setVelocity(velocityX, velocityY, -velocityZ);
	}
	public float getFlashIntensity(float partialTicks)
	{
		SubWeaponSettings<CurlingBombDataRecord> settings = getSettings();
		if (settings.subDataRecord == null)
			return 0;
		if (fuseTime <= settings.subDataRecord.warningFrame())
		{
			return settings.subDataRecord.warningFrame() - MathHelper.lerp(partialTicks, prevFuseTime, fuseTime) * 0.85f;
		}
		return 0;
	}
	private boolean canStepUp(Vec3d p_20273_)
	{
		Box box = getBoundingBox();
		List<VoxelShape> list = getWorld().getEntityCollisions(this, box.stretch(p_20273_));
		Vec3d vec3 = p_20273_.lengthSquared() == 0.0D ? p_20273_ : adjustMovementForCollisions(this, p_20273_, box, getWorld(), list);
		boolean flag = p_20273_.x != vec3.x;
		boolean flag1 = p_20273_.y != vec3.y;
		boolean flag2 = p_20273_.z != vec3.z;
		boolean flag3 = isOnGround() || flag1 && p_20273_.y < 0.0D;
		float stepHeight = getStepHeight();
		if (stepHeight > 0.0F && flag3 && (flag || flag2))
		{
			Vec3d vec31 = adjustMovementForCollisions(this, new Vec3d(p_20273_.x, stepHeight, p_20273_.z), box, getWorld(), list);
			Vec3d vec32 = adjustMovementForCollisions(this, new Vec3d(0.0D, stepHeight, 0.0D), box.stretch(p_20273_.x, 0.0D, p_20273_.z), getWorld(), list);
			if (vec32.y < (double) stepHeight)
			{
				Vec3d vec33 = adjustMovementForCollisions(this, new Vec3d(p_20273_.x, 0.0D, p_20273_.z), box.offset(vec32), getWorld(), list).add(vec32);
				if (vec33.horizontalLengthSquared() > vec31.horizontalLengthSquared())
				{
					vec31 = vec33;
				}
			}
			
			return vec31.horizontalLengthSquared() > vec3.horizontalLengthSquared();
		}
		
		return false;
	}
	@Override
	public void setVelocity(Entity thrower, float pitch, float yaw, float pitchOffset, float speed, float divergence)
	{
		// no you're not supposed to have momentum like the other subs because i HATE fun (also it breaks paints since you can launch it pretty far lol)
		float f = -MathHelper.sin(yaw * MathHelper.RADIANS_PER_DEGREE) * MathHelper.cos(pitch * MathHelper.RADIANS_PER_DEGREE);
		float g = -MathHelper.sin((pitch + pitchOffset) * MathHelper.RADIANS_PER_DEGREE);
		float h = MathHelper.cos(yaw * MathHelper.RADIANS_PER_DEGREE) * MathHelper.cos(pitch * MathHelper.RADIANS_PER_DEGREE);
		
		Vec3d vec3d = new Vec3d(f, g, h).multiply(speed);
		setVelocity(vec3d);
		velocityDirty = true;
		
		setYaw(yaw);
		setPitch(pitch);
		prevYaw = getYaw();
		prevPitch = getPitch();
	}
	@Override
	public void readCustomDataFromNbt(NbtCompound nbt)
	{
		super.readCustomDataFromNbt(nbt);
		setInitialFuseTime(nbt.getInt("FuseTime"));
	}
	@Override
	public void writeCustomDataToNbt(NbtCompound nbt)
	{
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("FuseTime", fuseTime);
	}
	public int getInitialFuseTime()
	{
		return dataTracker.get(INIT_FUSE_TIME);
	}
	public void setInitialFuseTime(int v)
	{
		dataTracker.set(INIT_FUSE_TIME, v);
	}
	public float getCookProgress()
	{
		return dataTracker.get(COOK_SCALE);
	}
	public void setCookScale(float v)
	{
		dataTracker.set(COOK_SCALE, v);
	}
	@Override
	public void onTrackedDataSet(TrackedData<?> data)
	{
		if (INIT_FUSE_TIME.equals(data))
			fuseTime = getInitialFuseTime();
		
		super.onTrackedDataSet(data);
	}
	@Override
	public void onCollidedWithObjectEntity(Entity entity)
	{
		explode(getSettings().subDataRecord, getBoundingBox().getCenter());
	}
}
