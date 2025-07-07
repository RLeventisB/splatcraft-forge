package net.splatcraft.entities.subs;

import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.entities.ObjectCollideListenerEntity;
import net.splatcraft.items.weapons.settings.SubWeaponRecords.BurstBombDataRecord;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkDamageUtils;
import net.splatcraft.util.InkExplosion;
import net.splatcraft.util.structs.AttackId;
import org.jetbrains.annotations.NotNull;

public class BurstBombEntity extends AbstractSubWeaponEntity<BurstBombDataRecord> implements ObjectCollideListenerEntity
{
	public BurstBombEntity(EntityType<? extends AbstractSubWeaponEntity<BurstBombDataRecord>> type, Level world)
	{
		super(type, world);
	}
	protected void onHitEntity(@NotNull EntityHitResult result)
	{
		super.onHitEntity(result);
		
		SubWeaponSettings<BurstBombDataRecord> settings = getSettings();
		
		InkDamageUtils.doDamage(result.getEntity(), settings.subDataRecord.directDamage(), getOwner(), this, sourceWeapon, SPLASH_DAMAGE_TYPE, false, AttackId.NONE);
		explode(settings, result.getLocation());
	}
	@Override
	protected void onHitBlock(BlockHitResult result)
	{
		SubWeaponSettings<BurstBombDataRecord> settings = getSettings();
		Vec3 impactPos = InkExplosion.adjustPosition(result.getLocation(), result.getDirection(), this);
		explode(settings, impactPos);
	}
	public void explode(SubWeaponSettings<BurstBombDataRecord> settings, Vec3 impactPos)
	{
		if (!level().isClientSide())
		{
			InkExplosion.createInkExplosion(getOwner(), impactPos, settings.subDataRecord.inkSplashRadius(), settings.subDataRecord.damageRanges(), inkType, sourceWeapon, AttackId.NONE);
			level().broadcastEntityEvent(this, (byte) 1);
			discard();
		}
		level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonate, SoundSource.PLAYERS, 0.8F, CommonUtils.nextTriangular(level().getRandom(), 0.95F, 0.095F));
	}
	@Override
	public void handleEntityEvent(byte id)
	{
		super.handleEntityEvent(id);
		if (id == 1)
		{
			level().addAlwaysVisibleParticle(new InkExplosionParticleData(getColor(), getSettings().subDataRecord.damageRanges().getMaxKey() * 2), getX(), getY(), getZ(), 0, 0, 0);
		}
	}
	@Override
	public void updateRotation()
	{
		Vec3 vec3 = getDeltaMovement();
		float angle = -tickCount * Mth.RAD_TO_DEG * 0.4f;
		float yRot = (float) (Mth.atan2(vec3.x, vec3.z) * Mth.RAD_TO_DEG);
		if (tickCount == 1)
		{
			setYRot(yRot);
			xRotO = angle;
			yRotO = yRot;
		}
		else
		{
			setYRot(lerpRotation(yRotO, yRot));
		}
		setXRot(angle);
	}
	@Override
	protected Item getDefaultItem()
	{
		return SplatcraftItems.burstBomb.get();
	}
	@Override
	public void onCollidedWithObjectEntity(Entity entity)
	{
		explode(getSettings(), getBoundingBox().getCenter());
	}
}
