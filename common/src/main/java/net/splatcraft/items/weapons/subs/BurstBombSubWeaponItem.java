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
import org.jetbrains.annotations.NotNull;

public class BurstBombSubWeaponItem extends SubWeaponItem<SubWeaponRecords.BurstBombDataRecord>
{
	public BurstBombSubWeaponItem(RegistrySupplier<? extends EntityType<? extends AbstractSubWeaponEntity<SubWeaponRecords.BurstBombDataRecord>>> entityType, String settings)
	{
		super(entityType, settings);
	}
	@Override
	public void useSub(@NotNull ItemStack stack, @NotNull Level world, @NotNull LivingEntity entity, int remainingUseTicks)
	{
		entity.swing(entity.getOffhandItem().equals(stack) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, false);
		
		SubWeaponSettings<SubWeaponRecords.BurstBombDataRecord> settings = getSettings(stack);
		SubWeaponSettings.DataRecord data = settings.dataRecord;
		SubWeaponRecords.BurstBombDataRecord burstData = settings.subDataRecord;
		if (!world.isClientSide())
		{
			AbstractSubWeaponEntity<SubWeaponRecords.BurstBombDataRecord> proj = AbstractSubWeaponEntity.create(entityType.get(), world, entity, stack.copy());
			
			proj.setItem(stack.copy());
			proj.setDeltaMovement(entity, entity.getXRot(), entity.getYRot(), burstData.throwAngle(), burstData.throwVelocity(), 0, 1f);
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
	}
}
