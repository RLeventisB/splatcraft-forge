package net.splatcraft.forge.entities.subs;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.forge.client.particles.InkExplosionParticleData;
import net.splatcraft.forge.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.registries.SplatcraftSounds;
import net.splatcraft.forge.util.InkDamageUtils;
import net.splatcraft.forge.util.InkExplosion;

public class BurstBombEntity extends AbstractSubWeaponEntity
{
	public BurstBombEntity(EntityType<? extends AbstractSubWeaponEntity> type, Level level)
	{
		super(type, level);
	}
	@Override
	protected void onHitEntity(EntityHitResult result)
	{
		super.onHitEntity(result);
		
		SubWeaponSettings settings = getSettings();
		
		if (result.getEntity() instanceof LivingEntity target)
			InkDamageUtils.doDamage(target, settings.directDamage, getOwner(), this, sourceWeapon, SPLASH_DAMAGE_TYPE, false);
		InkExplosion.createInkExplosion(getOwner(), result.getLocation(), settings.explosionSize, settings.explosionSize, settings.indirectDamage, settings.indirectDamage, inkType, sourceWeapon);
		
		level.broadcastEntityEvent(this, (byte) 1);
		level.playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonate, SoundSource.PLAYERS, 0.8F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
		if (!level.isClientSide())
			discard();
	}
	@Override
	protected void onBlockHit(BlockHitResult result)
	{
		SubWeaponSettings settings = getSettings();
		Vec3 impactPos = InkExplosion.adjustPosition(result.getLocation(), result.getDirection().getNormal());
		InkExplosion.createInkExplosion(getOwner(), impactPos, settings.explosionSize, settings.explosionSize, settings.indirectDamage, settings.indirectDamage, inkType, sourceWeapon);
		level.broadcastEntityEvent(this, (byte) 1);
		level.playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonate, SoundSource.PLAYERS, 0.8F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
		discard();
	}
	@Override
	public void handleEntityEvent(byte id)
	{
		super.handleEntityEvent(id);
		if (id == 1)
		{
			level.addAlwaysVisibleParticle(new InkExplosionParticleData(getColor(), getSettings().explosionSize * 2), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
		}
	}
	@Override
	protected Item getDefaultItem()
	{
		return SplatcraftItems.burstBomb.get();
	}
}
