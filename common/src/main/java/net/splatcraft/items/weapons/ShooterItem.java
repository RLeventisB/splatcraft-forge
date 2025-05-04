package net.splatcraft.items.weapons;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.client.handlers.SplatcraftKeyHandler;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.handlers.ShootingHandler;
import net.splatcraft.items.weapons.settings.ShooterWeaponSettings;
import net.splatcraft.items.weapons.settings.ShotDeviationHelper;
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import org.jetbrains.annotations.NotNull;

public class ShooterItem extends WeaponBaseItem<ShooterWeaponSettings>
{
	protected ShooterItem(String settings)
	{
		super(settings, properties -> properties.component(SplatcraftComponents.SHOOTER_FIRING_DATA, SplatcraftComponents.ShooterFiringData.DEFAULT));
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
		return registry.register(name, () -> new ShooterItem(parent.value().components().get(SplatcraftComponents.WEAPON_SETTING_ID).toString()).setSecret(secret));
	}
	@Override
	public Class<ShooterWeaponSettings> getSettingsClass()
	{
		return ShooterWeaponSettings.class;
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		ShooterWeaponSettings settings = getSettings(stack);
		if (entity instanceof LivingEntity living)
		{
			ShootingHandler.tickShootingComponent(
				stack, SplatcraftComponents.SHOOTER_FIRING_DATA, SplatcraftComponents.ShooterFiringData.DEFAULT,
				data ->
					data.tick(
						(accumulatedTime) ->
						{
							fire(settings, world, stack, living, accumulatedTime);
							return v -> living.isUsingItem() ? v : v.withRepeatingFlag(false);
						},
						(accumulatedTime) -> v -> v)
			);
		}
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
	public void fire(ShooterWeaponSettings settings, Level level, ItemStack stack, LivingEntity entity, float accumulatedTime)
	{
		if (reduceInk(entity, this, settings.shotData.inkConsumption(), settings.shotData.inkRecoveryCooldown(), true))
		{
			if (entity == Minecraft.getInstance().player)
				SplatcraftKeyHandler.squidAndSubDelay = (int) settings.shotData.miscEndlagTicks();
			else if (!level.isClientSide)
			{
				float divergence = ShotDeviationHelper.updateShotDeviation(stack, level.getRandom(), settings.getShotDeviationData(stack, entity));
				for (int i = 0; i < settings.shotData.projectileCount(); i++)
				{
					InkProjectileEntity proj = new InkProjectileEntity(level, entity, stack, InkBlockUtils.getInkType(entity), settings.projectileData.size(), settings);
					proj.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), settings.shotData.pitchCompensation(), settings.shotData.speed(), divergence);
					proj.setShooterStats(settings);
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
		return ShootingHandler.isDoingShootingActionOnBothHands(player) ? PlayerPosingHandler.WeaponPose.DUAL_FIRE : PlayerPosingHandler.WeaponPose.FIRE;
	}
	@Override
	public boolean preventsChanging(ItemStack stack)
	{
		return SplatcraftComponents.getOptional(stack, SplatcraftComponents.SHOOTER_FIRING_DATA).map(SplatcraftComponents.ShooterFiringData::preventsChanging).orElse(false);
	}
	/*
	public static class ShooterFiringData extends FiringData implements SplatcraftComponents.FiringData<ShooterFiringData>
	{
		public static final Codec<ShooterFiringData> CODEC = RecordCodecBuilder.create(inst ->
			codecStart(inst).apply(inst, ShooterFiringData::new)
		);
		public final ShooterWeaponSettings settings;
		public ShooterFiringData(float counter, float startup, float repeat, float endlag, boolean repeatFlag, ItemStack itemStack, int slotIndex, InteractionHand hand)
		{
			super(counter, startup, repeat, endlag, repeatFlag, itemStack, slotIndex, hand);
			settings = itemStack.getItem() instanceof ShooterItem shooterItem ? shooterItem.getSettings(itemStack) : null;
		}
		public ShooterFiringData(ItemStack storedStack, ShooterWeaponSettings settings, int slotIndex, InteractionHand hand)
		{
			super(settings.shotData.startupTicks(), settings.shotData.repeatTicks(), settings.shotData.endlagTicks(), storedStack, slotIndex, hand);
			this.settings = settings;
		}
		@Override
		public void onAction(LivingEntity entity, float extraTime)
		{
			Level world = entity.level();
			if (!world.isClientSide)
			{
				if (reduceInk(entity, storedStack.getItem(), settings.shotData.inkConsumption(), settings.shotData.inkRecoveryCooldown(), true))
				{
					float inaccuracy = ShotDeviationHelper.updateShotDeviation(storedStack, world.getRandom(), settings.getShotDeviationData(storedStack, entity));
					for (int i = 0; i < settings.shotData.projectileCount(); i++)
					{
						InkProjectileEntity proj = new InkProjectileEntity(world, entity, storedStack, InkBlockUtils.getInkType(entity), settings.projectileData.size(), settings);
						proj.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), settings.shotData.pitchCompensation(), settings.shotData.speed(), inaccuracy);
						proj.setShooterStats(settings);
						world.addFreshEntity(proj);
						proj.tick(extraTime);
					}
					world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.shooterShot, SoundSource.PLAYERS, 0.7F, CommonUtils.nextTriangular(world.getRandom(), 0.95F, 0.095F));
				}
			}
		}
		@Override
		public void onActionDone(LivingEntity entity, float extraTime)
		{

		}
	}
*/
}
