package net.splatcraft.items.weapons;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.entities.ExtraSaveData;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.handlers.ShootingHandler;
import net.splatcraft.items.weapons.settings.BlasterWeaponSettings;
import net.splatcraft.items.weapons.settings.ShotDeviationHelper;
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.AttackId;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import org.jetbrains.annotations.NotNull;

public class BlasterItem extends WeaponBaseItem<BlasterWeaponSettings>
{
	protected BlasterItem(String settings)
	{
		super(settings);
	}
	public static RegistrySupplier<BlasterItem> createBlaster(DeferredRegister<Item> registry, String settings, String name)
	{
		return registry.register(name, () -> new BlasterItem(settings));
	}
	public static RegistrySupplier<BlasterItem> createBlaster(DeferredRegister<Item> registry, RegistrySupplier<BlasterItem> parent, String name)
	{
		return registry.register(name, () -> new BlasterItem(parent.value().settingsId.toString()));
	}
	private static boolean getEndlagConsumer(ShootingHandler.WeaponShootingData data, Float accumulatedTime, LivingEntity entity, Boolean isStillUsing)
	{
		if (!isStillUsing || !data.entityData.isPlayer)
			return isStillUsing;
		
		ItemCooldowns cooldownTracker = data.entityData.player.getCooldowns();
		
		if (!data.entityData.player.level().isClientSide)
		{
			cooldownTracker.addCooldown(data.entityData.player.getInventory().getItem(data.entityData.selected).getItem(), (int) (data.firingData.getFiringSpeed() - accumulatedTime));
		}
		return true;
	}
	@Override
	public Class<BlasterWeaponSettings> getSettingsClass()
	{
		return BlasterWeaponSettings.class;
	}
	@Override
	public void weaponUseTick(Level world, LivingEntity entity, ItemStack stack, int no)
	{
		ShootingHandler.notifyStartShooting(entity);
	}
	@Override
	public ShootingHandler.FiringStatData getWeaponFireData(ItemStack stack, LivingEntity entity)
	{
		BlasterWeaponSettings settings = getSettings(stack);
		return ShootingHandler.FiringStatData.createFromShotData(settings.shotData,
			BlasterItem::getEndlagConsumer,
			(data, accumulatedTime, entity1) ->
			{
				Level world = entity1.level();
				if (!world.isClientSide())
				{
					BlasterItem item = (BlasterItem) data.useItem.getItem();
					if (reduceInk(entity, item, settings.shotData.inkConsumption(), settings.shotData.inkRecoveryCooldown(), true))
					{
						InkProjectileEntity proj = new InkProjectileEntity(world, entity, data.useItem, InkBlockUtils.getInkType(entity), settings.projectileData.size(), settings);
						proj.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), 0, settings.shotData.speed(), ShotDeviationHelper.updateShotDeviation(data.useItem, world.getRandom(), settings.getShotDeviationData(data.useItem, entity)));
						proj.setBlasterStats(settings);
						proj.setAttackId(AttackId.registerAttack().countProjectile());
						proj.addExtraData(new ExtraSaveData.ExplosionExtraData(settings.blasterData));
						world.addFreshEntity(proj);
						world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.blasterShot, SoundSource.PLAYERS, 0.7F, CommonUtils.nextTriangular(world.getRandom(), 0.95F, 0.095F));
						proj.tick(accumulatedTime);
					}
				}
			}, null);
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		super.inventoryTick(stack, world, entity, itemSlot, isSelected);
	}
	@Override
	public PlayerPosingHandler.WeaponPose getPose(Player player, ItemStack stack)
	{
		return ShootingHandler.isDoingShootingAction(player) && ShootingHandler.shootingData.get(player).isDualFire() ? PlayerPosingHandler.WeaponPose.DUAL_FIRE : PlayerPosingHandler.WeaponPose.FIRE;
	}
}