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
import org.jetbrains.annotations.NotNull;

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
	}
}
