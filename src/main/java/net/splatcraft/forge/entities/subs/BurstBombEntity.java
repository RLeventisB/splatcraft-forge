package net.splatcraft.forge.entities.subs;

import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import net.splatcraft.forge.util.AttackId;
import net.splatcraft.forge.util.CommonUtils;
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
            InkDamageUtils.doDamage(target, settings.subDataRecord.directDamage(), getOwner(), this, sourceWeapon, SPLASH_DAMAGE_TYPE, false, AttackId.NONE);
        InkExplosion.createInkExplosion(getOwner(), result.getLocation(), settings.subDataRecord.inkSplashRadius(), settings.subDataRecord.damageRanges(), inkType, sourceWeapon, AttackId.NONE);

        level().broadcastEntityEvent(this, (byte) 1);
        level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonate, SoundSource.PLAYERS, 0.8F, CommonUtils.triangle(level().getRandom(), 0.95F, 0.095F));
        if (!level().isClientSide())
            discard();
    }

    @Override
    protected void onBlockHit(BlockHitResult result)
    {
        SubWeaponSettings settings = getSettings();
        Vec3 impactPos = InkExplosion.adjustPosition(result.getLocation(), result.getDirection().getNormal());
        InkExplosion.createInkExplosion(getOwner(), impactPos, settings.subDataRecord.inkSplashRadius(), settings.subDataRecord.damageRanges(), inkType, sourceWeapon, AttackId.NONE);
        level().broadcastEntityEvent(this, (byte) 1);
        level().playSound(null, getX(), getY(), getZ(), SplatcraftSounds.subDetonate, SoundSource.PLAYERS, 0.8F, CommonUtils.triangle(level().getRandom(), 0.95F, 0.095F));
        discard();
    }

    @Override
    public void handleEntityEvent(byte id)
    {
        super.handleEntityEvent(id);
        if (id == 1)
        {
            level().addAlwaysVisibleParticle(new InkExplosionParticleData(getColor(), getSettings().subDataRecord.damageRanges().getMaxDistance() * 2), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    public void updateRotation()
    {
        float angle = -tickCount * Mth.RAD_TO_DEG * 0.4f;
        Vec3 vec3 = this.getDeltaMovement();
        double d0 = vec3.horizontalDistance();
        this.setXRot(angle);
        this.setYRot(CommonUtils.lerpRotation(0.2f, this.yRotO, (float) (Mth.atan2(vec3.x, vec3.z) * Mth.RAD_TO_DEG)));
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
