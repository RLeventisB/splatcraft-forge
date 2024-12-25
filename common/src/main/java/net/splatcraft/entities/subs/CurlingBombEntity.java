package net.splatcraft.entities.subs;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.client.particles.InkSplashParticleData;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.*;

import java.util.List;

public class CurlingBombEntity extends AbstractSubWeaponEntity
{
	public static final int FLASH_DURATION = 20;
	private static final TrackedData<Integer> INIT_FUSE_TIME = DataTracker.registerData(CurlingBombEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Float> COOK_SCALE = DataTracker.registerData(CurlingBombEntity.class, TrackedDataHandlerRegistry.FLOAT);
	public int fuseTime = 0;
	public int prevFuseTime = 0;
	public float bladeRot = 0;
	public float prevBladeRot = 0;
	private boolean playedActivationSound = false;
	public CurlingBombEntity(EntityType<? extends AbstractSubWeaponEntity> type, World world)
	{
		super(type, world);
	}
	public static void onItemUseTick(World world, LivingEntity entity, ItemStack stack, int useTime)
	{
		NbtCompound data = stack.get(SplatcraftComponents.SUB_WEAPON_DATA);
		data.putInt("CookTime", stack.getItem().getMaxUseTime(stack, entity) - useTime);
		
		stack.set(SplatcraftComponents.SUB_WEAPON_DATA, data);
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
	public void readItemData(NbtCompound nbt)
	{
		if (nbt.contains("CookTime"))
		{
			if (getSettings().subDataRecord.curlingData().cookTime() > 0)
				setCookScale(Math.min(4, (nbt.getInt("CookTime") / (float) getSettings().subDataRecord.curlingData().cookTime())));
			setInitialFuseTime(nbt.getInt("CookTime"));
			prevFuseTime = getInitialFuseTime();
		}
	}
	@Override
	public void tick()
	{
		super.tick();
		
		SubWeaponSettings settings = getSettings();
		
		double spd = getVelocity().multiply(1, 0, 1).length();
		prevBladeRot = bladeRot;
		bladeRot += (float) spd;
		
		prevFuseTime = fuseTime;
		fuseTime++;
		
		boolean playAlertAnim = fuseTime == 30;
		
		if (fuseTime >= settings.subDataRecord.fuseTime() - FLASH_DURATION && !playedActivationSound)
		{
			getWorld().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonating, SoundCategory.PLAYERS, 0.8F, 1f);
			playedActivationSound = true;
		}
		
		if (!getWorld().isClient())
		{
			if (spd > 1.0E-3)
			{
				for (float j = -0.15f; j <= 0.15f; j += 0.3f)
				{
					Vec3d normalized = getVelocity().multiply(1, 0, 1).normalize();
					double sideX = -normalized.z;
					double sideZ = normalized.x;
					for (int i = 0; i <= 2; i++)
					{
						BlockPos side = CommonUtils.createBlockPos(Math.floor(getX() + sideX * j), getBlockY() - i, Math.floor(getZ() + sideZ * j));
						if (InkBlockUtils.canInkFromFace(getWorld(), side, Direction.UP))
						{
							InkBlockUtils.playerInkBlock(getOwner() instanceof PlayerEntity player ? player : null, getWorld(), side, getColor(), Direction.UP, inkType, settings.subDataRecord.curlingData().contactDamage());
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
						InkBlockUtils.playerInkBlock(getOwner() instanceof PlayerEntity player ? player : null, getWorld(), getBlockPos().down(i), getColor(), Direction.UP, inkType, settings.subDataRecord.curlingData().contactDamage());
						break;
					}
			}
		}
		if (!isOnGround() || squaredDistanceTo(getVelocity()) > 1.0E-5)
		{
			float f1 = 0.98F;
			if (isOnGround())
				f1 = getWorld().getBlockState(CommonUtils.createBlockPos(getX(), getY() - 1.0D, getZ())).getBlock().getSlipperiness();
			
			f1 = (float) Math.min(0.98, f1 * 3f) * Math.min(1, 2 * (1 - fuseTime / (float) settings.subDataRecord.fuseTime()));
			
			setVelocity(getVelocity().multiply(f1, 0.98D, f1));
		}
		if (fuseTime >= settings.subDataRecord.fuseTime())
		{
			DamageRangesRecord radiuses = settings.subDataRecord.damageRanges().cloneWithMultiplier(getCookScale(), getCookScale());
			InkExplosion.createInkExplosion(getOwner(), getBoundingBox().getCenter(), settings.subDataRecord.inkSplashRadius() * getCookScale(), radiuses, inkType, sourceWeapon, AttackId.NONE);
			getWorld().sendEntityStatus(this, (byte) 1);
			getWorld().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonate, SoundCategory.PLAYERS, 0.8F, CommonUtils.nextTriangular(getWorld().getRandom(), 0.95F, 0.095F));
			if (!getWorld().isClient())
				discard();
			return;
		}
		else if (spd > 0.01 && fuseTime % (int) Math.max(1, (1 - spd) * 10) == 0)
		{
			getWorld().sendEntityStatus(this, (byte) 2);
		}
		move(MovementType.SELF, getVelocity().multiply(0, 1, 0));
		
		Vec3d vec = getVelocity().multiply(1, 0, 1);
		vec = getPos().add(collide(vec));
		
		setPos(vec.x, vec.y, vec.z);
	}
	@Override
	public void handleStatus(byte id)
	{
		super.handleStatus(id);
		if (id == 1)
		{
			getWorld().addImportantParticle(new InkExplosionParticleData(getColor(), (getSettings().subDataRecord.damageRanges().getMaxDistance() + getCookScale()) * 2), getX(), getY(), getZ(), 0, 0, 0);
		}
		if (id == 2)
		{
			getWorld().addParticle(new InkSplashParticleData(getColor(), getSettings().subDataRecord.damageRanges().getMaxDistance() * 1.15f), getX(), getY() + 0.4, getZ(), 0, 0, 0);
		}
	}
	//Ripped and modified from Minestuck's BouncingProjectileEntity class (with permission)
	@Override
	protected void onEntityHit(EntityHitResult result)
	{
		if (result.getEntity() instanceof LivingEntity livingEntity)
		{
			InkDamageUtils.doRollDamage(livingEntity, getSettings().subDataRecord.curlingData().contactDamage(), getOwner(), this, sourceWeapon);
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
		if (canStepUp(getVelocity()))
			return;
		
		double velocityX = getVelocity().x;
		double velocityY = getVelocity().y;
		double velocityZ = getVelocity().z;
		
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
	@Override
	protected boolean handleMovement()
	{
		return false;
	}
	public float getFlashIntensity(float partialTicks)
	{
		SubWeaponSettings settings = getSettings();
		return Math.max(0, MathHelper.lerp(partialTicks, prevFuseTime, fuseTime) - (settings.subDataRecord.fuseTime() - FLASH_DURATION)) * 0.85f / FLASH_DURATION;
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
	private Vec3d collide(Vec3d p_20273_)
	{
		Box aabb = getBoundingBox();
		List<VoxelShape> list = getWorld().getEntityCollisions(this, aabb.stretch(p_20273_));
		Vec3d vec3 = p_20273_.lengthSquared() == 0.0D ? p_20273_ : adjustMovementForCollisions(this, p_20273_, aabb, getWorld(), list);
		boolean flag = p_20273_.x != vec3.x;
		boolean flag1 = p_20273_.y != vec3.y;
		boolean flag2 = p_20273_.z != vec3.z;
		boolean flag3 = isOnGround() || flag1 && p_20273_.y < 0.0D;
		float stepHeight = getStepHeight();
		if (stepHeight > 0.0F && flag3 && (flag || flag2))
		{
			Vec3d vec31 = adjustMovementForCollisions(this, new Vec3d(p_20273_.x, stepHeight, p_20273_.z), aabb, getWorld(), list);
			Vec3d vec32 = adjustMovementForCollisions(this, new Vec3d(0.0D, stepHeight, 0.0D), aabb.stretch(p_20273_.x, 0.0D, p_20273_.z), getWorld(), list);
			if (vec32.y < (double) stepHeight)
			{
				Vec3d vec33 = adjustMovementForCollisions(this, new Vec3d(p_20273_.x, 0.0D, p_20273_.z), aabb.offset(vec32), getWorld(), list).add(vec32);
				if (vec33.horizontalLengthSquared() > vec31.horizontalLengthSquared())
				{
					vec31 = vec33;
				}
			}
			
			if (vec31.horizontalLengthSquared() > vec3.horizontalLengthSquared())
			{
				return vec31.add(adjustMovementForCollisions(this, new Vec3d(0.0D, -vec31.y + p_20273_.y, 0.0D), aabb.offset(vec31), getWorld(), list));
			}
		}
		
		return vec3;
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
	public float getCookScale()
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
}
