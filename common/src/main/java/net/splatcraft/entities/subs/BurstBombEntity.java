package net.splatcraft.entities.subs;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.entities.ObjectCollideListenerEntity;
import net.splatcraft.items.weapons.settings.SubWeaponRecords.BurstBombDataRecord;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.AttackId;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkDamageUtils;
import net.splatcraft.util.InkExplosion;

public class BurstBombEntity extends AbstractSubWeaponEntity<BurstBombDataRecord> implements ObjectCollideListenerEntity
{
	public BurstBombEntity(EntityType<? extends AbstractSubWeaponEntity<BurstBombDataRecord>> type, World world)
	{
		super(type, world);
	}
	protected void onEntityHit(EntityHitResult result)
	{
		super.onEntityHit(result);
		
		SubWeaponSettings<BurstBombDataRecord> settings = getSettings();
		
		if (result.getEntity() instanceof LivingEntity target)
			InkDamageUtils.doDamage(target, settings.subDataRecord.directDamage(), getOwner(), this, sourceWeapon, SPLASH_DAMAGE_TYPE, false, AttackId.NONE);
		explode(settings, result.getPos());
	}
	@Override
	protected void onBlockHit(BlockHitResult result)
	{
		SubWeaponSettings<BurstBombDataRecord> settings = getSettings();
		Vec3d impactPos = InkExplosion.adjustPosition(result.getPos(), result.getSide().getUnitVector());
		explode(settings, impactPos);
	}
	public void explode(SubWeaponSettings<BurstBombDataRecord> settings, Vec3d impactPos)
	{
		if (!getWorld().isClient())
		{
			InkExplosion.createInkExplosion(getOwner(), impactPos, settings.subDataRecord.inkSplashRadius(), settings.subDataRecord.damageRanges(), inkType, sourceWeapon, AttackId.NONE);
			getWorld().sendEntityStatus(this, (byte) 1);
			discard();
		}
		getWorld().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonate, SoundCategory.PLAYERS, 0.8F, CommonUtils.nextTriangular(getWorld().getRandom(), 0.95F, 0.095F));
	}
	@Override
	public void handleStatus(byte id)
	{
		super.handleStatus(id);
		if (id == 1)
		{
			getWorld().addImportantParticle(new InkExplosionParticleData(getColor(), getSettings().subDataRecord.damageRanges().getMaxDistance() * 2), getX(), getY(), getZ(), 0, 0, 0);
		}
	}
	@Override
	public void updateRotation()
	{
		float angle = -age * MathHelper.DEGREES_PER_RADIAN * 0.4f;
		Vec3d vec3 = getVelocity();
		setPitch(angle);
		setYaw(CommonUtils.lerpRotation(0.2f, prevYaw, (float) (MathHelper.atan2(vec3.x, vec3.z) * MathHelper.DEGREES_PER_RADIAN)));
	}
	@Override
	public void tick()
	{
		super.tick();
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
