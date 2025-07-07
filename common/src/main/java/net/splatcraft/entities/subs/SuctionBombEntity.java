package net.splatcraft.entities.subs;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.entities.ObjectCollideListenerEntity;
import net.splatcraft.items.weapons.settings.SubWeaponRecords.ThrowableExplodingSubDataRecord;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.structs.AttackId;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkExplosion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SuctionBombEntity extends AbstractSubWeaponEntity<ThrowableExplodingSubDataRecord> implements ObjectCollideListenerEntity
{
	public static final int FLASH_DURATION = 20;
	private static final EntityDataAccessor<Boolean> ACTIVATED = SynchedEntityData.defineId(SuctionBombEntity.class, EntityDataSerializers.BOOLEAN);
	public int shakeTime;
	protected int fuseTime = 0;
	protected int prevFuseTime = 0;
	protected boolean playedActivationSound = false;
	@Nullable
	private BlockState inBlockState;
	@Nullable
	private Direction stickFacing;
	public SuctionBombEntity(EntityType<? extends AbstractSubWeaponEntity<ThrowableExplodingSubDataRecord>> type, Level world)
	{
		super(type, world);
	}
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		super.defineSynchedData(builder);
		builder.define(ACTIVATED, false);
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
		SubWeaponSettings<ThrowableExplodingSubDataRecord> settings = getSettings();
		if (shakeTime > 0)
			--shakeTime;

		prevFuseTime = fuseTime;

		if (isActivated())
		{
			fuseTime++;
			if (fuseTime >= settings.subDataRecord.fuseTime() && stickFacing != null)
			{
				explode(settings);

				return;
			}
			else if (fuseTime >= settings.subDataRecord.fuseTime() - FLASH_DURATION && !playedActivationSound)
			{
				level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonating, SoundSource.PLAYERS, 0.8F, 1f);
				playedActivationSound = true;
			}
		}

		if (isSticked())
		{
			if (level().noCollision(getBoundingBox().expandTowards(Vec3.ZERO.relative(stickFacing, -0.05f))))
			{
				stickFacing = null;
				setActivated(false);
				Vec3 vector3d = getDeltaMovement();
				setDeltaMovement(vector3d.multiply(random.nextFloat() * 0.2F, random.nextFloat() * 0.2F, random.nextFloat() * 0.2F));
			}
			else
			{
				setDeltaMovement(0, 0, 0);
				setStickFacing();
			}
		}

		checkInsideBlocks();
	}
	@Override
	public @NotNull Vec3 getLightProbePosition(float tickDelta)
	{
		if (stickFacing != null)
		{
			return getBoundingBox().getCenter().relative(stickFacing, 0.3f);
		}
		return super.getLightProbePosition(tickDelta);
	}
	private void explode(SubWeaponSettings<ThrowableExplodingSubDataRecord> settings)
	{
		Vec3 impactPos = stickFacing != null ? getBoundingBox().getCenter().relative(stickFacing, 0.3f) : getPosition(0);
		InkExplosion.createInkExplosion(getOwner(), impactPos, settings.subDataRecord.inkSplashRadius(), settings.subDataRecord.damageRanges(), inkType, sourceWeapon, AttackId.NONE);
		level().broadcastEntityEvent(this, (byte) 1);
		level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonate, SoundSource.PLAYERS, 0.8F, CommonUtils.nextTriangular(level().getRandom(), 0.95F, 0.095F));
		if (!level().isClientSide())
			discard();
	}
	@Override
	public void handleEntityEvent(byte id)
	{
		super.handleEntityEvent(id);
		if (id == 1)
			level().addAlwaysVisibleParticle(new InkExplosionParticleData(getColor(), getSettings().subDataRecord.damageRanges().getMaxDistance() * 2), getX(), getY(), getZ(), 0, 0, 0);
	}
	public void setStickFacing()
	{
		if (stickFacing.get2DDataValue() >= 0)
		{
			setYRot(180 - stickFacing.toYRot());
			yRotO = getYRot();
		}
		else
		{
			setXRot(stickFacing.equals(Direction.UP) ? -90 : 90);
			setYRot(yRotO);
			xRotO = getXRot();
		}
	}
	public float getFlashIntensity(float partialTicks)
	{
		SubWeaponSettings<ThrowableExplodingSubDataRecord> settings = getSettings();
		if (settings.subDataRecord == null)
			return 0;
		return Math.max(0, Mth.lerpInt(partialTicks, prevFuseTime, fuseTime) - (settings.subDataRecord.fuseTime() - FLASH_DURATION)) * 0.85f / FLASH_DURATION;
	}
	@Override
	protected void onHitBlock(@NotNull BlockHitResult result)
	{
		if (!isSticked())
		{
			shakeTime = 7;
			inBlockState = level().getBlockState(result.getBlockPos());

			setActivated(true);

			setPos(result.getLocation());
			setDeltaMovement(Vec3.ZERO);

			stickFacing = result.getDirection();
			if (stickFacing.getAxis() == Direction.Axis.Y)
			{
				setPos(result.getLocation().add(0, -getBbHeight() / 2, 0));
			}
			else
			{
				setPos(result.getLocation());
			}
			setStickFacing();
		}
	}
	public boolean isActivated()
	{
		return entityData.get(ACTIVATED);
	}
	public void setActivated(boolean v)
	{
		entityData.set(ACTIVATED, v);
	}
	public boolean isSticked()
	{
		return stickFacing != null;
	}
	@Override
	public void readAdditionalSaveData(CompoundTag nbt)
	{
		super.readAdditionalSaveData(nbt);
		setActivated(nbt.getBoolean("Activated"));
		if (nbt.contains("StickFacing"))
			stickFacing = Direction.byName(nbt.getString("StickFacing"));
		shakeTime = nbt.getInt("ShakeTime");
		if (nbt.contains("InBlockState", Tag.TAG_COMPOUND))
		{
			BlockState.CODEC.parse(NbtOps.INSTANCE, nbt.getCompound("inBlockState")).ifSuccess(v -> inBlockState = v);
		}

		fuseTime = nbt.getInt("FuseTime");
		prevFuseTime = fuseTime;
	}
	@Override
	public void addAdditionalSaveData(CompoundTag nbt)
	{
		super.addAdditionalSaveData(nbt);
		nbt.putBoolean("Activated", isActivated());
		if (stickFacing != null)
			nbt.putString("StickFacing", stickFacing.name());
		nbt.putInt("ShakeTime", shakeTime);
		if (inBlockState != null)
			nbt.put("InBlockState", BlockState.CODEC.encode(inBlockState, NbtOps.INSTANCE, nbt).getOrThrow());

		nbt.putInt("FuseTime", fuseTime);
	}
	@Override
	public void onCollidedWithObjectEntity(Entity entity)
	{
		explode(getSettings());
	}
}
