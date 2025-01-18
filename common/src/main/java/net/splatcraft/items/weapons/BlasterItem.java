package net.splatcraft.items.weapons;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.world.World;
import net.splatcraft.entities.ExtraSaveData;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.handlers.ShootingHandler;
import net.splatcraft.items.weapons.settings.BlasterWeaponSettings;
import net.splatcraft.items.weapons.settings.ShotDeviationHelper;
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
		return registry.register(name, () -> new BlasterItem(parent.get().settingsId.toString()));
	}
	private static boolean getEndlagConsumer(ShootingHandler.WeaponShootingData data, Float accumulatedTime, LivingEntity entity, Boolean isStillUsing)
	{
		if (!isStillUsing || !data.entityData.isPlayer)
			return isStillUsing;
		
		ItemCooldownManager cooldownTracker = data.entityData.player.getItemCooldownManager();
		
		if (!data.entityData.player.getWorld().isClient)
		{
			cooldownTracker.set(data.entityData.player.getInventory().getStack(data.entityData.selected).getItem(), (int) (data.firingData.getFiringSpeed() - accumulatedTime));
		}
		return true;
	}
	@Override
	public Class<BlasterWeaponSettings> getSettingsClass()
	{
		return BlasterWeaponSettings.class;
	}
	@Override
	public void weaponUseTick(World world, LivingEntity entity, ItemStack stack, int no)
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
				World world = entity1.getWorld();
				if (!world.isClient())
				{
					BlasterItem item = (BlasterItem) data.useItem.getItem();
					if (reduceInk(entity, item, settings.shotData.inkConsumption(), settings.shotData.inkRecoveryCooldown(), true))
					{
						InkProjectileEntity proj = new InkProjectileEntity(world, entity, data.useItem, InkBlockUtils.getInkType(entity), settings.projectileData.size(), settings);
						proj.setVelocity(entity, entity.getPitch(), entity.getYaw(), 0, settings.projectileData.speed(), ShotDeviationHelper.updateShotDeviation(data.useItem, world.getRandom(), settings.getShotDeviationData(data.useItem, entity)));
						proj.setBlasterStats(settings);
						proj.setAttackId(AttackId.registerAttack().countProjectile());
						proj.addExtraData(new ExtraSaveData.ExplosionExtraData(settings.blasterData));
						world.spawnEntity(proj);
						world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.blasterShot, SoundCategory.PLAYERS, 0.7F, CommonUtils.nextTriangular(world.getRandom(), 0.95F, 0.095F));
						proj.tick(accumulatedTime);
					}
				}
			}, null);
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull World world, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		super.inventoryTick(stack, world, entity, itemSlot, isSelected);
	}
	@Override
	public PlayerPosingHandler.WeaponPose getPose(PlayerEntity player, ItemStack stack)
	{
		return ShootingHandler.isDoingShootingAction(player) && ShootingHandler.shootingData.get(player).isDualFire() ? PlayerPosingHandler.WeaponPose.DUAL_FIRE : PlayerPosingHandler.WeaponPose.FIRE;
	}
}