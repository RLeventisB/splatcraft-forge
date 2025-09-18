package net.splatcraft.items.weapons.subs;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.entities.subs.AbstractSubWeaponEntity;
import net.splatcraft.items.weapons.settings.SubWeaponRecords;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.structs.trajectory.TrajectoryProcessor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class ThrowableBombSubWeaponItem extends SubWeaponItem<SubWeaponRecords.ThrowableExplodingSubDataRecord>
{
	public ThrowableBombSubWeaponItem(RegistrySupplier<? extends EntityType<? extends AbstractSubWeaponEntity<SubWeaponRecords.ThrowableExplodingSubDataRecord>>> entityType, String settings)
	{
		super(entityType, settings);
	}
	@Override
	public void useSub(@NotNull ItemStack stack, @NotNull Level world, @NotNull LivingEntity entity, int remainingUseTicks)
	{
		entity.swing(entity.getOffhandItem().equals(stack) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, false);

		SubWeaponSettings<SubWeaponRecords.ThrowableExplodingSubDataRecord> settings = getSettings(stack);
		SubWeaponRecords.ThrowableExplodingSubDataRecord subData = settings.subDataRecord;
		SubWeaponSettings.DataRecord data = settings.dataRecord;
		if (!world.isClientSide())
		{
			AbstractSubWeaponEntity<SubWeaponRecords.ThrowableExplodingSubDataRecord> proj = AbstractSubWeaponEntity.create(getEntityType(stack), world, entity, stack.copy());

			proj.setItem(stack.copy());
			proj.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), subData.pitchOffset(), subData.throwVelocity(), 0, subData.throwerImpulse());
			world.addFreshEntity(proj);
		}
		world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.subThrow, SoundSource.PLAYERS, 0.7F, 1);
		if (singleUse(stack))
		{
			if (entity instanceof Player player && !player.isCreative())
				stack.shrink(1);
		}
		else
			reduceInk(entity, this, data.inkUsage().consumption(), data.inkUsage().recoveryCooldown(), false);
		applyCooldown(entity);
	}
	@Override
	public TrajectoryProcessor getTrajectory(ItemStack stack, LivingEntity entity, float partialTicks)
	{
		SubWeaponSettings<SubWeaponRecords.ThrowableExplodingSubDataRecord> settings = getSettings(stack);
		SubWeaponRecords.ThrowableExplodingSubDataRecord subData = settings.subDataRecord;

		return TrajectoryProcessor.ofFragileIgnoreEntities(entity.level(), subData.throwVelocity(), subData.pitchOffset(), 0.09f, subData.throwerImpulse(), new Vector3f(0.94f));
	}
}
