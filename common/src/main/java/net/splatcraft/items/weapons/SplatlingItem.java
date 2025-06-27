package net.splatcraft.items.weapons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.client.audio.SplatlingChargingTickableSound;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.items.InkTankItem;
import net.splatcraft.items.weapons.settings.DynamicDataRecord;
import net.splatcraft.items.weapons.settings.ShotDeviationHelper;
import net.splatcraft.items.weapons.settings.SplatlingWeaponSettings;
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.platform.Services;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public class SplatlingItem<T extends DynamicDataRecord<T>> extends WeaponBaseItem<SplatlingWeaponSettings<T>> implements IChargeableWeapon
{
	private static final int MAX_CHARGES = 2;
	public SplatlingChargingTickableSound chargingSound;
	protected SplatlingItem(String settingsId)
	{
		super(settingsId, v -> v
			.component(SplatcraftComponents.CHARGE_DATA, SplatcraftComponents.ChargeData.DEFAULT)
			.component(SplatcraftComponents.SPLATLING_FIRING_DATA, SplatcraftComponents.SplatlingFiringData.DEFAULT)
		);
	}
	public static RegistrySupplier<SplatlingItem> create(DeferredRegister<Item> register, String settings, String name)
	{
		return register.register(name, () -> new SplatlingItem(settings));
	}
	public static RegistrySupplier<SplatlingItem> create(DeferredRegister<Item> register, RegistrySupplier<SplatlingItem> parent, String name)
	{
		return register.register(name, () -> new SplatlingItem(parent.value().components().get(SplatcraftComponents.WEAPON_SETTING_ID).toString()));
	}
	@OnlyIn(Dist.CLIENT)
	protected void playChargingSound(LivingEntity entity, ItemStack stack)
	{
		LocalPlayer clientPlayer = ClientUtils.getClientPlayer();
		if (!Objects.equals(clientPlayer, entity))
		{
			return;
		}

		SoundEvent soundEvent = stack.get(SplatcraftComponents.CHARGE_DATA).charge() > 1 ? SplatcraftSounds.splatlingChargeSecondLevel : SplatcraftSounds.splatlingCharge;

		if (chargingSound == null || chargingSound.isStopped() || !chargingSound.getSoundEvent().equals(soundEvent))
		{
			boolean soundExists = chargingSound != null;
			if (soundExists)
				chargingSound.fadeOut();
			chargingSound = new SplatlingChargingTickableSound(clientPlayer, soundEvent, 2);
			if (soundExists)
				chargingSound.fadeIn();
			Minecraft.getInstance().getSoundManager().play(chargingSound);
		}
	}
	@OnlyIn(Dist.CLIENT)
	protected static void playChargeReadySound(LivingEntity entity, float pitch)
	{
		if (ClientUtils.getClientPlayer() != null && ClientUtils.getClientPlayer().getUUID().equals(entity.getUUID()))
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SplatcraftSounds.splatlingReady, pitch, Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.PLAYERS)));
	}
	/*
		// its time for boilerplate code
		public static float getScaledShotSettingFloat(SplatlingWeaponSettings settings, float charge, Function<SplatlingWeaponSettings.SplatlingShotDataRecord, Float> getter)
		{
			float min = getter.apply(settings.firstChargeLevelShot);
			float max = getter.apply(settings.secondChargeLevelShot);
			return min + (max - min) * Mth.clamp(charge, 0, 1);
		}
		public static float getScaledProjectileSettingFloat(SplatlingWeaponSettings settings, float charge, Function<CommonRecords.ProjectileDataRecord, Float> getter)
		{
			float min = getter.apply(settings.firstChargeLevelProjectile);
			float max = getter.apply(settings.secondChargeLevelProjectile);
			return min + (max - min) * Mth.clamp(charge, 0, 1);
		}
		public static int getScaledShotSettingInt(SplatlingWeaponSettings settings, float charge, Function<SplatlingWeaponSettings.SplatlingShotDataRecord, Integer> getter)
		{
			float min = getter.apply(settings.firstChargeLevelShot);
			float max = getter.apply(settings.secondChargeLevelShot);
			return Math.round(min + (max - min) * Mth.clamp(charge, 0, 1));
		}
		public static int getScaledProjectileSettingInt(SplatlingWeaponSettings settings, float charge, Function<CommonRecords.ProjectileDataRecord, Integer> getter)
		{
			float min = getter.apply(settings.firstChargeLevelProjectile);
			float max = getter.apply(settings.secondChargeLevelProjectile);
			return Math.round(min + (max - min) * Mth.clamp(charge, 0, 1));
		}
	*/
	@Override
	public Class<SplatlingWeaponSettings<T>> getSettingsClass()
	{
		return (Class<SplatlingWeaponSettings<T>>) SplatlingWeaponSettings.CLASS;
	}
	/*
		@Override
		public void weaponUseTick(Level world, LivingEntity entity, ItemStack stack, int remainingUseTicks)
		{
			if (EntityAction.hasEntityAction(entity))
				EntityAction.setEntityAction(entity, null);

			SplatlingWeaponSettings settings = getSettings(stack);

			if (world.isClientSide)
			{
				float prevCharge = stack.get(SplatcraftComponents.CHARGE_DATA).previousCharge();
				float newCharge = prevCharge + 1f / (prevCharge >= 1 ? settings.chargeData.secondChargeTime() : settings.chargeData.firstChargeTime());

				if (!enoughInk(entity, this, Mth.lerp(newCharge * 0.5f, 0, settings.inkConsumption), 0, remainingUseTicks % 4 == 0))
				{
					float rechargeMult = InkTankItem.rechargeMult(entity.getItemBySlot(EquipmentSlot.CHEST), true);
					if (!hasInkInTank(entity, this) || rechargeMult == 0)
						return;

					newCharge = prevCharge + 1f / (prevCharge >= 1 ? settings.chargeData.emptyTankSecondChargeRate() : settings.chargeData.emptyTankFirstChargeRate()) * rechargeMult;
				}

				playChargingSound(entity, stack);

				if (prevCharge < maxCharges && newCharge >= Math.ceil(prevCharge) && prevCharge > 0)
					playChargeReadySound(entity, newCharge / maxCharges);

	//			EntityStoredCharge.addChargeValue(player, stack, newCharge - prevCharge, true, maxCharges);
			}
			else if (remainingUseTicks % 4 == 0 && !enoughInk(entity, this, 0.1f, 0, false))
				playNoInkSound(entity, SplatcraftSounds.noInkMain);
		}
			@Override
			public void onPlayerCooldownEnd(Level world, Player player, ItemStack stack, EntityAction action)
			{
				if (action.getTime() > 0)
				{
					if (!world.isClientSide)
					{
						SplatlingWeaponSettings settings = getSettings(stack);

						float chargeLevel = action.getMaxTime() / (float) settings.chargeData.firingDuration(); //yeah idk about this
						float cooldownLeft = action.getTime() / action.getMaxTime();
						float inkConsumed = Mth.lerp(chargeLevel * 0.5f, 0, settings.inkConsumption);
						float inkRefunded = inkConsumed * cooldownLeft;

						refundInk(player, inkRefunded);
					}
					else if (EntityStoredCharge.hasCharge(player) && player.equals(ClientUtils.getClientPlayer()))
					{
						Optional<EntityStoredCharge> charge = EntityStoredCharge.getChargeOptional(player);
						charge.get().reset();
					}
				}
			}
			@Override
			public void onPlayerCooldownTick(Level world, Player player, ItemStack stack, EntityAction action)
			{
				if (world.isClientSide)
					return;

				SplatlingWeaponSettings settings = getSettings(stack);
				float charge = stack.get(SplatcraftComponents.CHARGE_DATA).charge();

				boolean secondData = charge > 1;
				SplatlingWeaponSettings.SplatlingShotDataRecord firingData = secondData ? settings.secondChargeLevelShot : settings.firstChargeLevelShot;
				CommonRecords.ProjectileDataRecord projectileData = secondData ? settings.secondChargeLevelProjectile : settings.firstChargeLevelProjectile;

				float firingSpeed = getScaledShotSettingFloat(settings, charge, SplatlingWeaponSettings.SplatlingShotDataRecord::repeatTicks);

				if (firingSpeed > 0 && (action.getTime() - 1) % firingSpeed == 0)
				{
					float inaccuracy = ShotDeviationHelper.updateShotDeviation(stack, world.getRandom(), firingData.accuracyData());
					for (int i = 0; i < firingData.projectileCount(); i++)
					{
						InkProjectileEntity proj = new InkProjectileEntity(world, player, stack, InkBlockUtils.getInkType(player), projectileData.size(), settings);
						proj.shootFromRotation(player, player.getXRot(), player.getYRot(), firingData.pitchCompensation(), getScaledShotSettingFloat(settings, charge, SplatlingWeaponSettings.SplatlingShotDataRecord::projectileSpeed),
							inaccuracy);
						proj.setSplatlingStats(settings, charge);
						world.addFreshEntity(proj);
					}

					world.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.splatlingShot, SoundSource.PLAYERS, 0.7F, CommonUtils.nextTriangular(world.getRandom(), 0.95F, 0.095F));
				}
			}
				public void onReleaseCharge(Level world, Player player, ItemStack stack, float charge)
				{
					SplatlingWeaponSettings settings = getSettings(stack);

					stack.set(SplatcraftComponents.CHARGE_DATA, charge);

					int cooldownTime = (int) (getDecayTicks(stack) * charge);
					reduceInk(player, this, Mth.lerp(charge * 0.5f, 0, settings.inkConsumption), cooldownTime + settings.inkRecoveryCooldown, true, true);
					EntityAction.setEntityAction(player, new EntityCooldown(stack, cooldownTime, EntitySlot.createForUsed(player), true, false, !settings.chargeData.canRechargeWhileFiring(), player.onGround()).setCancellable());
				}
			*/
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		if (entity instanceof LivingEntity living)
		{
			SplatlingWeaponSettings<T> settings = getSettings(stack);

			float chargeMult = 0f;
			if (living.isUsingItem())
			{
				float charge = stack.get(SplatcraftComponents.CHARGE_DATA).charge();
				chargeMult = 1f;
				if (!entity.onGround())
					chargeMult *= charge >= 1 ? settings.chargeData.airborneSecondChargeRate() : settings.chargeData.airborneFirstChargeRate();

				float consumptionForNextTick = settings.inkConsumption * charge * 0.5f;
				if (!enoughInk(living, stack.getItem(), consumptionForNextTick, 0, !level.isClientSide && living.getTicksUsingItem() % 4 == 0))
				{
					float rechargeMult = Mth.clamp((InkTankItem.getInkAmount(living.getItemBySlot(EquipmentSlot.CHEST)) - consumptionForNextTick) / consumptionForNextTick, 0, 1);

					chargeMult *= Mth.lerp(rechargeMult, charge >= 1 ? settings.chargeData.emptyTankSecondChargeRate() : settings.chargeData.emptyTankFirstChargeRate(), 1f);
				}
			}

			float finalChargeMult = chargeMult;
			stack.update(SplatcraftComponents.SPLATLING_FIRING_DATA, SplatcraftComponents.SplatlingFiringData.DEFAULT, v -> v.tick(living, stack, settings, finalChargeMult, (prevCharge, newCharge) ->
			{
				if (Services.PLATFORM.isClientSide())
				{
					playChargingSound(living, stack);

					if (prevCharge < MAX_CHARGES && newCharge >= Math.ceil(prevCharge) && prevCharge > 0)
						playChargeReadySound(living, newCharge / MAX_CHARGES);
				}
			}, (charge, chargeStart, shotType) ->
			{
				reduceInk(living, this, Mth.lerp((charge - chargeStart) * 0.5f, 0, settings.inkConsumption), settings.inkRecoveryCooldown, true, true);
			}, (a) ->
			{
			}, (projectileData, shotData, extraTime, dataIndex) ->
			{
				if (!EntityInfoCapability.isSquid(living))
				{
					CommonUtils.setSquidDelay(living, shotData.miscEndlagTicks());
					if (!level.isClientSide)
					{
						float divergence = ShotDeviationHelper.updateShotDeviation(stack, level.getRandom(), settings.getShotDeviationData(stack, living));
						for (int i = 0; i < shotData.projectileCount(); i++)
						{
							InkProjectileEntity proj = new InkProjectileEntity(level, living, stack, InkBlockUtils.getInkType(living), projectileData.size(), settings);
							proj.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), shotData.pitchCompensation(), shotData.projectileSpeed(), divergence);
							proj.setSplatlingStats(settings, dataIndex);
							level.addFreshEntity(proj);
							proj.tick(extraTime);
						}
						level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.splatlingShot, SoundSource.PLAYERS, 0.7F, CommonUtils.nextTriangular(level.getRandom(), 0.95F, 0.095F));
					}
				}
				// bandaid implementation to ink recovery cooldown being applied on the last shot
				InkTankItem.setRecoveryCooldown(living.getItemBySlot(EquipmentSlot.CHEST), settings.inkRecoveryCooldown);
			}));
		}
		super.inventoryTick(stack, level, entity, itemSlot, isSelected);
	}
	/*
		@Override
		public void releaseUsing(@NotNull ItemStack stack, @NotNull Level world, @NotNull LivingEntity entity, int timeLeft)
		{
			super.releaseUsing(stack, world, entity, timeLeft);

			if (world.isClientSide && entity instanceof Player player && player.equals(ClientUtils.getClientPlayer()))
			{
				if (EntityAction.hasActionAnd(player, EntityAction::preventWeaponUse))
					return;

				Optional<EntityStoredCharge> charge = EntityStoredCharge.getChargeOptional(player);

				if (charge.isEmpty())
					return;
				if (!SplatcraftKeyHandler.isSquidKeyDown() && charge.get().charge > 0.05f) //checking for squid key press so it doesn't immediately release charge when squidding
				{
					SplatlingWeaponSettings settings = getSettings(stack);
					EntityAction.setEntityAction(player, new EntityCooldown(stack, (int) (settings.chargeData.firingDuration() * charge.get().charge), EntitySlot.createForUsed(player), true, false, !settings.chargeData.canRechargeWhileFiring(), player.onGround()).setCancellable());
				}
			}
		}
	*/
	@Override
	public void weaponUseTick(Level world, LivingEntity entity, ItemStack stack, int remainingUseTicks)
	{
		SplatlingWeaponSettings<T> settings = getSettings(stack);
		if (!enoughInk(entity, this, 0.1f, 0, false))
			return;

		if (entity instanceof Player player && player.getCooldowns().isOnCooldown(this))
			return;

		stack.update(SplatcraftComponents.SPLATLING_FIRING_DATA, SplatcraftComponents.SplatlingFiringData.DEFAULT,
			v -> v.notifyUsage(entity, settings, stack));
	}
	@Override
	public boolean preventsChanging(ItemStack stack, LivingEntity entity)
	{
		return stack.get(SplatcraftComponents.SPLATLING_FIRING_DATA).preventsChanging();
	}
	@Override
	public PlayerPosingHandler.WeaponPose getPose(Player player, ItemStack stack)
	{
		return PlayerPosingHandler.WeaponPose.SPLATLING;
	}
	@Override
	public boolean hasSpeedModifier(LivingEntity entity, ItemStack stack)
	{
		return preventsChanging(stack, entity);
	}
	@Override
	public AttributeModifier getSpeedModifier(LivingEntity entity, ItemStack stack)
	{
		SplatlingWeaponSettings<T> settings = getSettings(stack);

		SplatcraftComponents.SplatlingFiringData firingData = stack.get(SplatcraftComponents.SPLATLING_FIRING_DATA);
		boolean charging = firingData.charging().equals(Optional.of(true));
		double appliedMobility = settings.getMoveSpeed(charging, settings.getShotTypeIndex(getCharge(stack), firingData.shotTypeData()));

		return new AttributeModifier(SplatcraftItems.SPEED_MOD_IDENTIFIER, appliedMobility - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}
	@Override
	public int getChargeLevels(ItemStack stack)
	{
		return MAX_CHARGES;
	}
	@Override
	public int getStorageTime(ItemStack stack)
	{
		return getSettings(stack).chargeData.chargeStorageTime();
	}
	@Override
	public void retrieveCharge(LivingEntity entity, ItemStack stack, float charge)
	{
		IChargeableWeapon.super.retrieveCharge(entity, stack, charge);
		SplatlingWeaponSettings<T> settings = getSettings(stack);
		CommonUtils.setSquidDelay(entity, settings.chargeData.chargeStorageSquidLag());
		stack.update(SplatcraftComponents.SPLATLING_FIRING_DATA, SplatcraftComponents.SplatlingFiringData.DEFAULT, v -> v.retrieveCharge(settings));

		if (entity.level().isClientSide())
		{
			playChargeReadySound(entity, Mth.floor(charge) / MAX_CHARGES);
		}
	}
	@Override
	public boolean canStore(ItemStack stack)
	{
		return
			getSettings(stack).chargeData.chargeStorageTime() > 0 &&
				stack.get(SplatcraftComponents.CHARGE_DATA).charge() >= 1;
	}
}