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
import net.splatcraft.items.weapons.settings.SubWeaponRecords.CurlingBombDataRecord;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.items.weapons.settings.SubWeaponSettings.DataRecord;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.structs.trajectory.TrajectoryProcessor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class CurlingSubWeaponItem extends SubWeaponItem<CurlingBombDataRecord>
{
	public float cookProgress;
	public CurlingSubWeaponItem(RegistrySupplier<? extends EntityType<? extends AbstractSubWeaponEntity<CurlingBombDataRecord>>> entityType, String settings)
	{
		super(entityType, settings);
	}
	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand)
	{
		cookProgress = 0;
		return super.use(level, player, hand);
	}
	@Override
	public void useSub(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity, int remainingUseTicks)
	{
		SubWeaponSettings<CurlingBombDataRecord> settings = getSettings(stack);
		if (entity.getTicksUsingItem() >= settings.dataRecord.holdTime() - 1)
		{
			return;
		}
		
		shootCurlingBomb(stack, level, entity, settings);
	}
	private void shootCurlingBomb(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity, SubWeaponSettings<CurlingBombDataRecord> settings)
	{
		entity.swing(entity.getOffhandItem().equals(stack) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, false);
		
		DataRecord data = settings.dataRecord;
		CurlingBombDataRecord curlingData = settings.subDataRecord;
		cookProgress = (float) entity.getTicksUsingItem() / (data.holdTime() - 1);
		InkUsageDataRecord inkUsage = new InkUsageDataRecord(
			Mth.lerp(cookProgress, data.inkUsage().consumption(), curlingData.maxCookInkUsage().consumption()),
			Mth.lerp(cookProgress, data.inkUsage().recoveryCooldown(), curlingData.maxCookInkUsage().recoveryCooldown())
		);
		if (!level.isClientSide() && reduceInk(entity, this, inkUsage.consumption(), inkUsage.recoveryCooldown(), false))
		{
			CurlingBombEntity proj = AbstractSubWeaponEntity.create(getEntityType(stack), level, entity, stack.copy());
			
			proj.setCookScale(cookProgress);
			proj.setInitialFuseTime(curlingData.fuseTime().getValue(cookProgress));
			proj.setItem(stack);
			float throwXRot = Mth.clamp(entity.getXRot() + curlingData.throwAngle(), -89, 89);
			proj.setDeltaMovement(entity,
				throwXRot, entity.getYRot(), 0, curlingData.travelSpeedRange().getValue(cookProgress), 0, 0);
			level.addFreshEntity(proj);
		}
		level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.subThrow, SoundSource.PLAYERS, 0.7F, 1);
		if (singleUse(stack))
		{
			stack.consume(1, entity);
		}
		applyCooldown(entity);
	}
	@Override
	public void weaponUseTick(@NotNull Level level, @NotNull LivingEntity entity, @NotNull ItemStack stack, int remainingUseTicks)
	{
		SubWeaponSettings<CurlingBombDataRecord> settings = getSettings(stack);
		int holdTime = settings.dataRecord.holdTime();
		cookProgress = (float) (entity.getTicksUsingItem()) / holdTime;
		
		if (entity.getTicksUsingItem() == holdTime)
		{
			shootCurlingBomb(stack, level, entity, settings);
		}
		super.weaponUseTick(level, entity, stack, remainingUseTicks);
	}
	@Override
	public boolean useOnRelease(@NotNull ItemStack stack)
	{
		return true;
	}
	@Override
	public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity)
	{
		return USE_DURATION;
	}
	@Override
	public TrajectoryProcessor getTrajectory(ItemStack stack, LivingEntity entity, float partialTicks)
	{
		SubWeaponSettings<CurlingBombDataRecord> settings = getSettings(stack);
		CurlingBombDataRecord subData = settings.subDataRecord;
		
		return TrajectoryProcessor.ofFragileHorizontalMod(entity.level(), subData.travelSpeedRange().getValue(Math.min(cookProgress + partialTicks / settings.dataRecord.holdTime(), 1)), subData.throwAngle(), 0.09f, 0, new Vector3f(1f));
	}
}
