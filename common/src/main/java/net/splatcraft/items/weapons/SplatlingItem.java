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
import net.splatcraft.handlers.SpecialHandler;
import net.splatcraft.handlers.WeaponHandler;
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
import net.splatcraft.util.structs.DamageCalculator;
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
	protected static void playChargeReadySound(LivingEntity entity, float pitch)
	{
		if (ClientUtils.getClientPlayer() != null && ClientUtils.getClientPlayer().getUUID().equals(entity.getUUID()))
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SplatcraftSounds.splatlingReady, pitch, Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.PLAYERS)));
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
			
			// this method makes concurrency very angry!!!
			synchronized (this)
			{
				Minecraft.getInstance().getSoundManager().play(chargingSound);
			}
		}
	}
	@Override
	public Class<SplatlingWeaponSettings<T>> getSettingsClass()
	{
		return (Class<SplatlingWeaponSettings<T>>) SplatlingWeaponSettings.CLASS;
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		if (entity instanceof LivingEntity living)
		{
			SplatlingWeaponSettings<T> settings = getSettings(stack);
			
			float chargeMult = 0f;
			if (WeaponHandler.canContinueShooting(living))
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
							InkProjectileEntity proj = new InkProjectileEntity(level, living, stack, InkBlockUtils.getInkType(living), projectileData.size(), DamageCalculator.basic(projectileData));
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
	public Optional<SpecialHandler.ResetAction> getResetShootingAction(ItemStack stack, LivingEntity entity)
	{
		return Optional.of(() ->
			stack.set(SplatcraftComponents.SPLATLING_FIRING_DATA, SplatcraftComponents.SplatlingFiringData.DEFAULT));
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
				stack.get(SplatcraftComponents.CHARGE_DATA).charge() > 0;
	}
	@Override
	public boolean preventsSquidForm(ItemStack stack, LivingEntity entity)
	{
		return false;
	}
}