package net.splatcraft.items.weapons;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.client.handlers.SplatcraftKeyHandler;
import net.splatcraft.entities.ExtraSaveData;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.items.weapons.settings.BlasterWeaponSettings;
import net.splatcraft.items.weapons.settings.ShotDeviationHelper;
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.AttackId;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import org.jetbrains.annotations.NotNull;

public class BlasterItem extends WeaponBaseItem<BlasterWeaponSettings>
{
	protected BlasterItem(String settings)
	{
		super(settings, properties -> properties.component(SplatcraftComponents.SHOOTER_FIRING_DATA, SplatcraftComponents.ShooterFiringData.DEFAULT));
	}
	public static RegistrySupplier<BlasterItem> createBlaster(DeferredRegister<Item> registry, String settings, String name)
	{
		return registry.register(name, () -> new BlasterItem(settings));
	}
	public static RegistrySupplier<BlasterItem> createBlaster(DeferredRegister<Item> registry, RegistrySupplier<BlasterItem> parent, String name)
	{
		return registry.register(name, () -> new BlasterItem(parent.value().components().get(SplatcraftComponents.WEAPON_SETTING_ID).toString()));
	}
	@Override
	public Class<BlasterWeaponSettings> getSettingsClass()
	{
		return BlasterWeaponSettings.class;
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		BlasterWeaponSettings settings = getSettings(stack);
		if (entity instanceof LivingEntity living)
			stack.update(
				SplatcraftComponents.SHOOTER_FIRING_DATA,
				SplatcraftComponents.ShooterFiringData.DEFAULT,
				data ->
				{
					SplatcraftComponents.ShooterFiringData updatedData = data.tick(
						(accumulatedTime) ->
						{
							fire(settings, world, stack, living, accumulatedTime);
							return v -> living.isUsingItem() ? v : v.withRepeatingFlag(false);
						},
						(accumulatedTime) -> v -> v);
					return updatedData;
				}
			);
		super.inventoryTick(stack, world, entity, itemSlot, isSelected);
	}
	@Override
	public void weaponUseTick(Level world, LivingEntity entity, ItemStack stack, int remainingUseTicks)
	{
		stack.update(
			SplatcraftComponents.SHOOTER_FIRING_DATA,
			SplatcraftComponents.ShooterFiringData.DEFAULT,
			data -> data.notifyUsing(entity, getSettings(stack).shotData)
		);
	}
	public void fire(BlasterWeaponSettings settings, Level level, ItemStack stack, LivingEntity entity, float accumulatedTime)
	{
		if (reduceInk(entity, this, settings.shotData.inkConsumption(), settings.shotData.inkRecoveryCooldown(), true))
		{
			SplatcraftKeyHandler.setSquidDelay(entity, settings.shotData.miscEndlagTicks());
			if (!level.isClientSide)
			{
				float divergence = ShotDeviationHelper.updateShotDeviation(stack, level.getRandom(), settings.getShotDeviationData(stack, entity));
				for (int i = 0; i < settings.shotData.projectileCount(); i++)
				{
					InkProjectileEntity proj = new InkProjectileEntity(level, entity, stack, InkBlockUtils.getInkType(entity), settings.projectileData.size(), settings);
					proj.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), settings.shotData.pitchCompensation(), settings.shotData.speed(), divergence);
					proj.setBlasterStats(settings);
					proj.setAttackId(AttackId.registerAttack().countProjectile());
					proj.addExtraData(new ExtraSaveData.ExplosionExtraData(settings.blasterData));
					level.addFreshEntity(proj);
					proj.tick(accumulatedTime);
				}
				level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.shooterShot, SoundSource.PLAYERS, 0.7F, CommonUtils.nextTriangular(level.getRandom(), 0.95F, 0.095F));
			}
		}
	}
	@Override
	public PlayerPosingHandler.WeaponPose getPose(Player player, ItemStack stack)
	{
		return PlayerPosingHandler.WeaponPose.FIRE;
	}
	@Override
	public boolean preventsChanging(ItemStack stack, LivingEntity entity)
	{
		return SplatcraftComponents.getOptional(stack, SplatcraftComponents.SHOOTER_FIRING_DATA).map(SplatcraftComponents.ShooterFiringData::preventsChanging).orElse(false);
	}
}