package net.splatcraft.items.weapons.subs;

import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.entities.subs.AbstractSubWeaponEntity;
import net.splatcraft.entities.subs.CurlingBombEntity;
import net.splatcraft.items.weapons.settings.CommonRecords.InkUsageDataRecord;
import net.splatcraft.items.weapons.settings.SubWeaponRecords;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.registries.SplatcraftSounds;
import org.jetbrains.annotations.NotNull;

public class CurlingSubWeaponItem extends SubWeaponItem<SubWeaponRecords.CurlingBombDataRecord>
{
	public float cookProgress;
	public CurlingSubWeaponItem(RegistrySupplier<? extends EntityType<? extends AbstractSubWeaponEntity<SubWeaponRecords.CurlingBombDataRecord>>> entityType, String settings)
	{
		super(entityType, settings);
	}
	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, Player player, @NotNull InteractionHand hand)
	{
		cookProgress = 0;
		return super.use(world, player, hand);
	}
	@Override
	public void useSub(@NotNull ItemStack stack, @NotNull Level world, @NotNull LivingEntity entity, int remainingUseTicks)
	{
		entity.swing(entity.getOffhandItem().equals(stack) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, false);
		
		SubWeaponSettings<SubWeaponRecords.CurlingBombDataRecord> settings = getSettings(stack);
		SubWeaponSettings.DataRecord data = settings.dataRecord;
		SubWeaponRecords.CurlingBombDataRecord curlingData = settings.subDataRecord;
		cookProgress = 1f - (float) (remainingUseTicks) / stack.getItem().getUseDuration(stack, entity);
		InkUsageDataRecord inkUsage = new InkUsageDataRecord(
			Mth.lerp(cookProgress, data.inkUsage().consumption(), curlingData.maxCookInkUsage().consumption()),
			Mth.lerp(cookProgress, data.inkUsage().recoveryCooldown(), curlingData.maxCookInkUsage().recoveryCooldown())
		);
		if (!world.isClientSide() && reduceInk(entity, this, inkUsage.consumption(), inkUsage.recoveryCooldown(), false))
		{
			CurlingBombEntity proj = (CurlingBombEntity) AbstractSubWeaponEntity.create(entityType.get(), world, entity, stack.copy());
			
			proj.setCookScale(cookProgress);
			proj.setInitialFuseTime(curlingData.fuseTime().getValue(cookProgress));
			proj.setItem(stack);
			proj.setDeltaMovement(entity, 0, entity.getYRot(), -30, curlingData.travelSpeedRange().getValue(cookProgress), 0, 0);
			world.addFreshEntity(proj);
		}
		world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.subThrow, SoundSource.PLAYERS, 0.7F, 1);
		if (singleUse(stack))
		{
			if (entity instanceof Player player && !player.isCreative())
				stack.shrink(1);
		}
	}
	@Override
	public void weaponUseTick(@NotNull Level world, @NotNull LivingEntity entity, @NotNull ItemStack stack, int remainingUseTicks)
	{
		cookProgress = (float) (stack.getItem().getUseDuration(stack, entity) - remainingUseTicks) / getSettings(stack).dataRecord.holdTime();
	}
}
