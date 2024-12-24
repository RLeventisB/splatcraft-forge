package net.splatcraft.entities.subs;

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
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.AttackId;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkDamageUtils;
import net.splatcraft.util.InkExplosion;

public class BurstBombEntity extends AbstractSubWeaponEntity
{
	public BurstBombEntity(EntityType<? extends AbstractSubWeaponEntity> type, World world)
	{
		super(type, world);
	}
	protected void onEntityHit(EntityHitResult result)
	{
		super.onEntityHit(result);
		
		SubWeaponSettings settings = getSettings();
		
		if (result.getEntity() instanceof LivingEntity target)
			InkDamageUtils.doDamage(target, settings.subDataRecord.directDamage(), getOwner(), this, sourceWeapon, SPLASH_DAMAGE_TYPE, false, AttackId.NONE);
		InkExplosion.createInkExplosion(getOwner(), result.getPos(), settings.subDataRecord.inkSplashRadius(), settings.subDataRecord.damageRanges(), inkType, sourceWeapon, AttackId.NONE);
		
		getWorld().sendEntityStatus(this, (byte) 1);
		getWorld().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonate, SoundCategory.PLAYERS, 0.8F, CommonUtils.nextTriangular(getWorld().getRandom(), 0.95F, 0.095F));
		if (!getWorld().isClient())
			discard();
	}
	@Override
	protected void onBlockHit(BlockHitResult result)
	{
		SubWeaponSettings settings = getSettings();
		Vec3d impactPos = InkExplosion.adjustPosition(result.getPos(), result.getSide().getUnitVector());
		InkExplosion.createInkExplosion(getOwner(), impactPos, settings.subDataRecord.inkSplashRadius(), settings.subDataRecord.damageRanges(), inkType, sourceWeapon, AttackId.NONE);
		getWorld().sendEntityStatus(this, (byte) 1);
		getWorld().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonate, SoundCategory.PLAYERS, 0.8F, CommonUtils.nextTriangular(getWorld().getRandom(), 0.95F, 0.095F));
		discard();
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
		double d0 = vec3.horizontalLength();
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
}
