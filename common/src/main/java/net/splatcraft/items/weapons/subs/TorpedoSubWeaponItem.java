package net.splatcraft.items.weapons.subs;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.entities.subs.AbstractSubWeaponEntity;
import net.splatcraft.entities.subs.TorpedoEntity;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.items.weapons.settings.SubWeaponSettings.DataRecord;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkDamageUtils;
import net.splatcraft.util.structs.InkColor;
import net.splatcraft.util.structs.trajectory.TrajectoryProcessor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import static net.splatcraft.items.weapons.settings.SubWeaponRecords.TorpedoDataRecord;

public class TorpedoSubWeaponItem extends SubWeaponItem<TorpedoDataRecord>
{
	public TorpedoSubWeaponItem(RegistrySupplier<? extends EntityType<? extends AbstractSubWeaponEntity<TorpedoDataRecord>>> entityType, String settings)
	{
		super(entityType, settings);
	}
	@Override
	public void useSub(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity, int remainingUseTicks)
	{
		entity.swing(entity.getOffhandItem().equals(stack) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, false);
		
		SubWeaponSettings<TorpedoDataRecord> settings = getSettings(stack);
		DataRecord data = settings.dataRecord;
		TorpedoDataRecord torpedoData = settings.subDataRecord;
		if (!level.isClientSide())
		{
			TorpedoEntity proj = AbstractSubWeaponEntity.create(getEntityType(stack), level, entity, stack.copy());
			
			proj.setSearchDelay(settings.subDataRecord.searchDelay());
			proj.setItem(stack.copy());
			proj.setDeltaMovement(entity, entity.getXRot(), entity.getYRot(), torpedoData.pitchOffset(), torpedoData.throwVelocity(), 0, torpedoData.throwerImpulse());
			level.addFreshEntity(proj);
		}
		level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.subThrow, SoundSource.PLAYERS, 0.7F, 1);
		if (singleUse(stack))
		{
			stack.consume(1, entity);
		}
		else
			reduceInk(entity, this, data.inkUsage().consumption(), data.inkUsage().recoveryCooldown(), false);
		applyCooldown(entity);
	}
	@Override
	public TrajectoryProcessor getTrajectory(ItemStack stack, LivingEntity entity, float partialTicks)
	{
		SubWeaponSettings<TorpedoDataRecord> settings = getSettings(stack);
		TorpedoDataRecord subData = settings.subDataRecord;
		InkColor ownerColor = ColorUtils.getEntityColor(entity);
		
		return TrajectoryProcessor.ofFragile(entity.level(),
			subData.throwVelocity(),
			subData.pitchOffset(),
			0.13f,
			0.7f,
			subData.throwerImpulse(),
			new Vector3f(0.95f),
			v -> v.canBeHitByProjectile() && v != entity && InkDamageUtils.canDamage(v, ownerColor)
		);
	}
}
