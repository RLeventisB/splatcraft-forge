package net.splatcraft.items.weapons.subs;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.splatcraft.entities.subs.AbstractSubWeaponEntity;
import net.splatcraft.entities.subs.CurlingBombEntity;
import net.splatcraft.items.weapons.settings.CommonRecords.InkUsageDataRecord;
import net.splatcraft.items.weapons.settings.SubWeaponRecords;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.registries.SplatcraftSounds;
import org.jetbrains.annotations.NotNull;

public class CurlingSubWeaponItem extends SubWeaponItem<SubWeaponRecords.CurlingBombDataRecord>
{
	public static final float MAX_INK_RECOVERY_COOLDOWN = 70f / 3f;
	public static final float INK_RECOVERY_COOLDOWN_MULTIPLIER = 40f / 3f;
	public float cookProgress;
	public CurlingSubWeaponItem(RegistrySupplier<? extends EntityType<? extends AbstractSubWeaponEntity<SubWeaponRecords.CurlingBombDataRecord>>> entityType, String settings)
	{
		super(entityType, settings);
	}
	@Override
	public @NotNull TypedActionResult<ItemStack> use(@NotNull World world, PlayerEntity player, @NotNull Hand hand)
	{
		cookProgress = 0;
		return super.use(world, player, hand);
	}
	@Override
	public void useSub(@NotNull ItemStack stack, @NotNull World world, @NotNull LivingEntity entity, int remainingUseTicks)
	{
		entity.swingHand(entity.getOffHandStack().equals(stack) ? Hand.OFF_HAND : Hand.MAIN_HAND, false);
		
		SubWeaponSettings<SubWeaponRecords.CurlingBombDataRecord> settings = getSettings(stack);
		SubWeaponSettings.DataRecord data = settings.dataRecord;
		SubWeaponRecords.CurlingBombDataRecord curlingData = settings.subDataRecord;
		cookProgress = 1f - (float) (remainingUseTicks) / stack.getItem().getMaxUseTime(stack, entity);
		InkUsageDataRecord inkUsage = new InkUsageDataRecord(
			MathHelper.lerp(cookProgress, data.inkUsage().consumption(), curlingData.maxCookInkUsage().consumption()),
			MathHelper.lerp(cookProgress, data.inkUsage().recoveryCooldown(), curlingData.maxCookInkUsage().recoveryCooldown())
		);
		if (!world.isClient() && reduceInk(entity, this, inkUsage.consumption(), inkUsage.recoveryCooldown(), false))
		{
			CurlingBombEntity proj = (CurlingBombEntity) AbstractSubWeaponEntity.create(entityType.get(), world, entity, stack.copy());
			
			proj.setCookScale(cookProgress);
			proj.setInitialFuseTime(curlingData.fuseTime().getValue(cookProgress));
			proj.setItem(stack);
			proj.setVelocity(entity, 0, entity.getYaw(), -30, curlingData.travelSpeedRange().getValue(cookProgress), 0);
			world.spawnEntity(proj);
		}
		world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.subThrow, SoundCategory.PLAYERS, 0.7F, 1);
		if (singleUse(stack))
		{
			if (entity instanceof PlayerEntity player && !player.isCreative())
				stack.decrement(1);
		}
	}
	@Override
	public void weaponUseTick(@NotNull World world, @NotNull LivingEntity entity, @NotNull ItemStack stack, int remainingUseTicks)
	{
		cookProgress = (float) (stack.getItem().getMaxUseTime(stack, entity) - remainingUseTicks) / getSettings(stack).dataRecord.holdTime();
	}
}
