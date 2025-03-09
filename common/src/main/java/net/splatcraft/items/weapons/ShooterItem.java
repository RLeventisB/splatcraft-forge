package net.splatcraft.items.weapons;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.handlers.ShootingHandler;
import net.splatcraft.items.weapons.settings.ShooterWeaponSettings;
import net.splatcraft.items.weapons.settings.ShotDeviationHelper;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import org.jetbrains.annotations.NotNull;

public class ShooterItem extends WeaponBaseItem<ShooterWeaponSettings>
{
	protected ShooterItem(String settings)
	{
		super(settings);
	}
	public static RegistrySupplier<ShooterItem> create(DeferredRegister<Item> registry, String settings, String name)
	{
		return registry.register(name, () -> new ShooterItem(settings));
	}
	public static RegistrySupplier<ShooterItem> create(DeferredRegister<Item> registry, RegistrySupplier<ShooterItem> parent, String name)
	{
		return create(registry, parent, name, false);
	}
	public static RegistrySupplier<ShooterItem> create(DeferredRegister<Item> registry, RegistrySupplier<ShooterItem> parent, String name, boolean secret)
	{
		return registry.register(name, () -> new ShooterItem(parent.get().settingsId.toString()).setSecret(secret));
	}
	@Override
	public Class<ShooterWeaponSettings> getSettingsClass()
	{
		return ShooterWeaponSettings.class;
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		super.inventoryTick(stack, world, entity, itemSlot, isSelected);
	}
	@Override
	public void weaponUseTick(Level world, LivingEntity entity, ItemStack stack, int remainingUseTicks)
	{
		ShootingHandler.notifyStartShooting(entity);
	}
	@Override
	public ShootingHandler.FiringStatData getWeaponFireData(ItemStack stack, LivingEntity entity)
	{
		ShooterWeaponSettings settings = getSettings(stack);
		return ShootingHandler.FiringStatData.createFromShotData(settings.shotData,
			null,
			(data, accumulatedTime, entity1) ->
			{
				Level world = entity1.level();
				if (!world.isClientSide)
				{
					if (reduceInk(entity, this, settings.shotData.inkConsumption(), settings.shotData.inkRecoveryCooldown(), true))
					{
						float inaccuracy = ShotDeviationHelper.updateShotDeviation(stack, world.getRandom(), settings.getShotDeviationData(stack, entity));
						for (int i = 0; i < settings.shotData.projectileCount(); i++)
						{
							InkProjectileEntity proj = new InkProjectileEntity(world, entity, stack, InkBlockUtils.getInkType(entity), settings.projectileData.size(), settings);
							proj.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), settings.shotData.pitchCompensation(), settings.shotData.speed(), inaccuracy);
							proj.setShooterStats(settings);
							world.addFreshEntity(proj);
							proj.tick(accumulatedTime);
						}
						world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.shooterShot, SoundSource.PLAYERS, 0.7F, CommonUtils.nextTriangular(world.getRandom(), 0.95F, 0.095F));
					}
				}
			}, null);
	}
	@Override
	public PlayerPosingHandler.WeaponPose getPose(Player player, ItemStack stack)
	{
		return ShootingHandler.isDoingShootingAction(player) && ShootingHandler.shootingData.get(player).isDualFire() ? PlayerPosingHandler.WeaponPose.DUAL_FIRE : PlayerPosingHandler.WeaponPose.FIRE;
	}
}
