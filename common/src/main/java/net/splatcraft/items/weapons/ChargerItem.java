package net.splatcraft.items.weapons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.client.audio.ChargerChargingTickableSound;
import net.splatcraft.entities.ExtraSaveData;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.handlers.SpecialHandler;
import net.splatcraft.items.InkTankItem;
import net.splatcraft.items.weapons.settings.ChargerWeaponSettings;
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.platform.Services;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.structs.DamageCalculator;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ChargerItem extends WeaponBaseItem<ChargerWeaponSettings> implements IChargeableWeapon
{
	public ChargerChargingTickableSound chargingSound;
	protected ChargerItem(String settingsId)
	{
		super(settingsId, properties -> properties
			.component(SplatcraftComponents.CHARGE_DATA, SplatcraftComponents.ChargeData.DEFAULT)
			.component(SplatcraftComponents.CHARGER_FIRING_DATA, SplatcraftComponents.ChargerFiringData.DEFAULT)
		);
	}
	public static RegistrySupplier<ChargerItem> create(DeferredRegister<Item> register, String settings, String name)
	{
		return register.register(name, () -> new ChargerItem(settings));
	}
	public static RegistrySupplier<ChargerItem> create(DeferredRegister<Item> register, RegistrySupplier<ChargerItem> parent, String name)
	{
		return register.register(name, () -> new ChargerItem(parent.value().components().get(SplatcraftComponents.WEAPON_SETTING_ID).toString()));
	}
	@OnlyIn(Dist.CLIENT)
	protected void playChargingSound(LivingEntity entity)
	{
		if (ClientUtils.getClientPlayer() == null || !ClientUtils.getClientPlayer().getUUID().equals(entity.getUUID()) || (chargingSound != null && !chargingSound.isStopped()))
		{
			return;
		}
		
		chargingSound = new ChargerChargingTickableSound(ClientUtils.getClientPlayer(), SplatcraftSounds.chargerCharge, 1);
		synchronized (this)
		{
			Minecraft.getInstance().getSoundManager().play(chargingSound);
		}
	}
	@OnlyIn(Dist.CLIENT)
	protected static void playChargeReadySound(LivingEntity entity)
	{
		if (ClientUtils.getClientPlayer() != null && ClientUtils.getClientPlayer().getUUID().equals(entity.getUUID()))
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SplatcraftSounds.chargerReady, Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.PLAYERS)));
	}
	@Override
	public Class<ChargerWeaponSettings> getSettingsClass()
	{
		return ChargerWeaponSettings.class;
	}
	public void shoot(Level world, LivingEntity entity, ItemStack stack, float charge, float extraTime)
	{
		ChargerWeaponSettings settings = getSettings(stack);
		CommonUtils.setSquidDelay(entity, settings.shotData.endlagTicks());
		if (world.isClientSide)
		{
			return;
		}
		
		InkProjectileEntity proj = new InkProjectileEntity(world, entity, stack, InkBlockUtils.getInkType(entity), settings.projectileData.size(), DamageCalculator.charger(settings.projectileData, charge));
		proj.setDeltaMovement(entity, entity.getXRot(), entity.getYRot(), 0.0f, settings.projectileData.speed().getValue(charge), 0f, 0f);
		proj.setChargerStats(charge, settings.projectileData);
		proj.addExtraData(new ExtraSaveData.ChargeExtraData(charge));
		proj.tick(extraTime);
		world.addFreshEntity(proj);
		world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.chargerShot, SoundSource.PLAYERS, 0.7F, CommonUtils.nextTriangular(world.getRandom(), 0.95F, 0.095F));
		
		reduceInk(entity, this, settings.shotData.inkConsumption().getValue(charge), settings.shotData.inkRecoveryCooldown(), false, true);
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		if (entity instanceof LivingEntity living)
		{
			ChargerWeaponSettings settings = getSettings(stack);
			
			float chargeMult = 0f;
			if (living.isUsingItem() && isSelected)
			{
				chargeMult = 1f;
				if (!entity.onGround())
					chargeMult *= settings.chargeData.airborneChargeRate();
				
				float consumptionForNextTick = settings.shotData.inkConsumption().getValue(
					stack.get(SplatcraftComponents.CHARGE_DATA).charge() + settings.chargeData.getChargePercentPerTick() * chargeMult);
				if (!enoughInk(living, stack.getItem(), consumptionForNextTick, 0, !world.isClientSide && living.getTicksUsingItem() % 4 == 0))
				{
					float rechargeMult = Mth.clamp((InkTankItem.getInkAmount(living.getItemBySlot(EquipmentSlot.CHEST)) - consumptionForNextTick) / consumptionForNextTick, 0, 1);
					
					chargeMult *= Mth.lerp(rechargeMult, settings.chargeData.emptyTankChargeRate(), 1f);
				}
			}
			
			float finalChargeMult = chargeMult;
			stack.update(SplatcraftComponents.CHARGER_FIRING_DATA, SplatcraftComponents.ChargerFiringData.DEFAULT, v -> v.tick(living, stack, settings, finalChargeMult, (prevCharge, newCharge) ->
			{
				if (Services.PLATFORM.isClientSide())
				{
					if (prevCharge < 1 && newCharge >= 1)
					{
						playChargeReadySound(living);
					}
					else if (newCharge < 1 && newCharge > 0)
					{
						playChargingSound(living);
					}
				}
			}, (charge, extraTime) ->
			{
				if (!CommonUtils.isSquid(living))
					shoot(world, living, stack, charge, extraTime);
			}));
		}
		super.inventoryTick(stack, world, entity, itemSlot, isSelected);
	}
	@Override
	public void weaponUseTick(Level world, LivingEntity entity, ItemStack stack, int remainingUseTicks)
	{
		ChargerWeaponSettings settings = getSettings(stack);
		if (!enoughInk(entity, this, settings.shotData.inkConsumption().minValue(), 0, remainingUseTicks == USE_DURATION))
			return;
		
		if (entity instanceof Player player && player.getCooldowns().isOnCooldown(this))
			return;
		
		stack.update(SplatcraftComponents.CHARGER_FIRING_DATA, SplatcraftComponents.ChargerFiringData.DEFAULT,
			v -> v.notifyUsage(entity, settings));
	}
	@Override
	public PlayerPosingHandler.WeaponPose getPose(Player player, ItemStack stack)
	{
		return PlayerPosingHandler.WeaponPose.BOW_CHARGE;
	}
	@Override
	public Optional<SpecialHandler.ResetAction> getResetShootingAction(ItemStack stack, LivingEntity entity)
	{
		return Optional.of(() ->
		{
			stack.set(SplatcraftComponents.CHARGER_FIRING_DATA, SplatcraftComponents.ChargerFiringData.DEFAULT);
			stack.set(SplatcraftComponents.CHARGE_DATA, SplatcraftComponents.ChargeData.DEFAULT);
		});
	}
	@Override
	public boolean preventsChanging(ItemStack stack, LivingEntity entity)
	{
		return stack.get(SplatcraftComponents.CHARGER_FIRING_DATA).preventsChanging();
	}
	@Override
	public boolean preventsChargingInkTank(ItemStack stack, LivingEntity entity)
	{
		ChargerWeaponSettings settings = getSettings(stack);
		float chargeMult = 1f;
		if (!entity.onGround())
			chargeMult *= settings.chargeData.airborneChargeRate();
		
		float consumptionForNextTick = settings.shotData.inkConsumption().getValue(
			stack.get(SplatcraftComponents.CHARGE_DATA).charge() + settings.chargeData.getChargePercentPerTick() * chargeMult);
		
		boolean chargingWeapon = stack.get(SplatcraftComponents.CHARGER_FIRING_DATA).charging();
		return preventsChanging(stack, entity) &&
			chargingWeapon &&
			enoughInk(entity, stack.getItem(), consumptionForNextTick, 0, false);
	}
	@Override
	public int getChargeLevels(ItemStack stack)
	{
		return 1;
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
		ChargerWeaponSettings settings = getSettings(stack);
		CommonUtils.setSquidDelay(entity, settings.chargeData.chargeStorageSquidLag());
		stack.update(SplatcraftComponents.CHARGER_FIRING_DATA, SplatcraftComponents.ChargerFiringData.DEFAULT, v -> v.retrieveCharge(settings));
		
		if (entity.level().isClientSide())
		{
			playChargeReadySound(entity);
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