package net.splatcraft.items.weapons.subs;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.splatcraft.entities.subs.AbstractSubWeaponEntity;
import net.splatcraft.items.weapons.settings.SubWeaponRecords;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.registries.SplatcraftSounds;
import org.jetbrains.annotations.NotNull;

public class BurstBombSubWeaponItem extends SubWeaponItem<SubWeaponRecords.BurstBombDataRecord>
{
	public BurstBombSubWeaponItem(RegistrySupplier<? extends EntityType<? extends AbstractSubWeaponEntity<SubWeaponRecords.BurstBombDataRecord>>> entityType, String settings)
	{
		super(entityType, settings);
	}
	@Override
	public void useSub(@NotNull ItemStack stack, @NotNull World world, @NotNull LivingEntity entity, int remainingUseTicks)
	{
		entity.swingHand(entity.getOffHandStack().equals(stack) ? Hand.OFF_HAND : Hand.MAIN_HAND, false);
		
		SubWeaponSettings<SubWeaponRecords.BurstBombDataRecord> settings = getSettings(stack);
		SubWeaponSettings.DataRecord data = settings.dataRecord;
		SubWeaponRecords.BurstBombDataRecord burstData = settings.subDataRecord;
		if (!world.isClient())
		{
			AbstractSubWeaponEntity<SubWeaponRecords.BurstBombDataRecord> proj = AbstractSubWeaponEntity.create(entityType.get(), world, entity, stack.copy());
			
			proj.setItem(stack.copy());
			proj.setVelocity(entity, entity.getPitch(), entity.getYaw(), burstData.throwAngle(), burstData.throwVelocity(), 0);
			proj.setVelocity(proj.getVelocity().add(entity.getVelocity().multiply(1, 0, 1)));
			world.spawnEntity(proj);
		}
		world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.subThrow, SoundCategory.PLAYERS, 0.7F, 1);
		if (singleUse(stack))
		{
			if (entity instanceof PlayerEntity player && !player.isCreative())
				stack.decrement(1);
		}
		else
			reduceInk(entity, this, data.inkUsage().consumption(), data.inkUsage().recoveryCooldown(), false);
	}
}
