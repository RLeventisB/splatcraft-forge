package net.splatcraft.items.weapons.subs;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.entities.subs.AbstractSubWeaponEntity;
import net.splatcraft.entities.subs.BurstBombEntity;
import net.splatcraft.items.weapons.settings.SubWeaponRecords.BurstBombDataRecord;
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

public class BurstBombSubWeaponItem extends SubWeaponItem<BurstBombDataRecord>
{
	public BurstBombSubWeaponItem(RegistrySupplier<? extends EntityType<? extends AbstractSubWeaponEntity<BurstBombDataRecord>>> entityType, String settings)
	{
		super(entityType, settings);
	}
	@Override
	public void useSub(@NotNull ItemStack stack, @NotNull Level world, @NotNull LivingEntity entity, int remainingUseTicks)
	{
		entity.swing(entity.getOffhandItem().equals(stack) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, false);

		SubWeaponSettings<BurstBombDataRecord> settings = getSettings(stack);
		DataRecord data = settings.dataRecord;
		BurstBombDataRecord burstData = settings.subDataRecord;
		if (!world.isClientSide())
		{
			BurstBombEntity proj = AbstractSubWeaponEntity.create(getEntityType(stack), world, entity, stack.copy());

			proj.setItem(stack.copy());
			proj.setDeltaMovement(entity, entity.getXRot(), entity.getYRot(), burstData.pitchOffset(), burstData.throwVelocity(), 0, burstData.throwerImpulse());
			world.addFreshEntity(proj);
		}
		world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.subThrow, SoundSource.PLAYERS, 0.7F, 1);
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
		SubWeaponSettings<BurstBombDataRecord> settings = getSettings(stack);
		BurstBombDataRecord subData = settings.subDataRecord;
		InkColor ownerColor = ColorUtils.getEntityColor(entity);

		return TrajectoryProcessor.ofFragile(entity.level(),
			subData.throwVelocity(),
			subData.pitchOffset(),
			0.09f,
			0.5f,
			subData.throwerImpulse(),
			new Vector3f(0.94f),
			v -> v.canBeHitByProjectile() && v != entity && InkDamageUtils.canDamage(v, ownerColor));
	}
}
