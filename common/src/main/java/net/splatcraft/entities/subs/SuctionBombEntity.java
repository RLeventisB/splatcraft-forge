package net.splatcraft.entities.subs;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.AttackId;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkExplosion;
import org.jetbrains.annotations.Nullable;

public class SuctionBombEntity extends AbstractSubWeaponEntity
{
	public static final int FLASH_DURATION = 20;
	private static final TrackedData<Boolean> ACTIVATED = DataTracker.registerData(SuctionBombEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	public int shakeTime;
	protected int fuseTime = 0;
	protected int prevFuseTime = 0;
	protected boolean inGround;
	protected boolean playedActivationSound = false;
	@Nullable
	private BlockState inBlockState;
	@Nullable
	private Direction stickFacing;
	public SuctionBombEntity(EntityType<? extends AbstractSubWeaponEntity> type, World world)
	{
		super(type, world);
	}
	@Override
	protected void initDataTracker(DataTracker.Builder builder)
	{
		super.initDataTracker(builder);
		builder.add(ACTIVATED, false);
	}
	@Override
	protected Item getDefaultItem()
	{
		return SplatcraftItems.suctionBomb.get();
	}
	@Override
	public void tick()
	{
		super.tick();
		BlockState state = getWorld().getBlockState(getBlockPos());
		SubWeaponSettings settings = getSettings();
		if (shakeTime > 0)
			--shakeTime;
		
		prevFuseTime = fuseTime;
		
		if (isActivated())
		{
			fuseTime++;
			if (fuseTime >= settings.subDataRecord.fuseTime())
			{
				InkExplosion.createInkExplosion(getOwner(), getBoundingBox().getCenter(), settings.subDataRecord.inkSplashRadius(), settings.subDataRecord.damageRanges(), inkType, sourceWeapon, AttackId.NONE);
				getWorld().sendEntityStatus(this, (byte) 1);
				getWorld().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonate, SoundCategory.PLAYERS, 0.8F, CommonUtils.nextTriangular(getWorld().getRandom(), 0.95F, 0.095F));
				if (!getWorld().isClient())
					discard();
				
				return;
			}
			else if (fuseTime >= settings.subDataRecord.fuseTime() - FLASH_DURATION && !playedActivationSound)
			{
				getWorld().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonating, SoundCategory.PLAYERS, 0.8F, 1f);
				playedActivationSound = true;
			}
		}
		
		if (inGround)
			if (inBlockState != state && getWorld().isSpaceEmpty((new Box(getPos(), getPos())).expand(0.06D)))
			{
				inGround = false;
				Vec3d vector3d = getVelocity();
				setVelocity(vector3d.multiply(random.nextFloat() * 0.2F, random.nextFloat() * 0.2F, random.nextFloat() * 0.2F));
			}
			else
			{
				setVelocity(0, 0, 0);
				setStickFacing();
			}
		
		checkBlockCollision();
	}
	@Override
	public void handleStatus(byte id)
	{
		super.handleStatus(id);
		if (id == 1)
			getWorld().addImportantParticle(new InkExplosionParticleData(getColor(), getSettings().subDataRecord.damageRanges().getMaxDistance() * 2), getX(), getY(), getZ(), 0, 0, 0);
	}
	public void setStickFacing()
	{
		if (stickFacing.getHorizontal() >= 0)
		{
			setYaw(180 - stickFacing.asRotation());
			prevYaw = getYaw();
		}
		else
		{
			setPitch(stickFacing.equals(Direction.UP) ? -90 : 90);
			setYaw(prevYaw);
			prevPitch = getPitch();
		}
	}
	public float getFlashIntensity(float partialTicks)
	{
		SubWeaponSettings settings = getSettings();
		return Math.max(0, MathHelper.lerp(partialTicks, prevFuseTime, fuseTime) - (settings.subDataRecord.fuseTime() - FLASH_DURATION)) * 0.85f / FLASH_DURATION;
	}
	@Override
	protected void onBlockHit(BlockHitResult result)
	{
		if (!inGround)
		{
			shakeTime = 7;
			inGround = true;
			inBlockState = getWorld().getBlockState(result.getBlockPos());
			
			setActivated(true);
			
			Vec3d vector3d = result.getPos().subtract(getX(), getY(), getZ());
			setVelocity(vector3d);
			Vec3d vector3d1 = vector3d.normalize().multiply(0.05F);
			setPos(getX() - vector3d1.x, getY() - vector3d1.y, getZ() - vector3d1.z);
			
			stickFacing = result.getSide();
			setStickFacing();
		}
	}
	public boolean isActivated()
	{
		return dataTracker.get(ACTIVATED);
	}
	public void setActivated(boolean v)
	{
		dataTracker.set(ACTIVATED, v);
	}
	@Override
	public void readCustomDataFromNbt(NbtCompound nbt)
	{
		super.readCustomDataFromNbt(nbt);
		setActivated(nbt.getBoolean("Activated"));
		if (nbt.contains("StickFacing"))
			stickFacing = Direction.byName(nbt.getString("StickFacing"));
		inGround = nbt.getBoolean("InGround");
		shakeTime = nbt.getInt("ShakeTime");
		if (nbt.contains("InBlockState", 10))
			inBlockState = BlockState.CODEC.decode(NbtOps.INSTANCE, nbt.getCompound("inBlockState")).getOrThrow().getFirst();
		
		fuseTime = nbt.getInt("FuseTime");
		prevFuseTime = fuseTime;
	}
	@Override
	public void writeCustomDataToNbt(NbtCompound nbt)
	{
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Activated", isActivated());
		if (stickFacing != null)
			nbt.putString("StickFacing", stickFacing.name());
		nbt.putBoolean("InGround", inGround);
		nbt.putInt("ShakeTime", shakeTime);
		if (inBlockState != null)
			nbt.put("InBlockState", BlockState.CODEC.encode(inBlockState, NbtOps.INSTANCE, nbt).getOrThrow());
		
		nbt.putInt("FuseTime", fuseTime);
	}
}
