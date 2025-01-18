package net.splatcraft.items.weapons;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.splatcraft.client.audio.SplatlingChargingTickableSound;
import net.splatcraft.client.handlers.SplatcraftKeyHandler;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.items.InkTankItem;
import net.splatcraft.items.weapons.settings.CommonRecords;
import net.splatcraft.items.weapons.settings.ShotDeviationHelper;
import net.splatcraft.items.weapons.settings.SplatlingWeaponSettings;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.ReleaseChargePacket;
import net.splatcraft.network.c2s.UpdateChargeStatePacket;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.*;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

public class SplatlingItem extends WeaponBaseItem<SplatlingWeaponSettings> implements IChargeableWeapon
{
	private static final int maxCharges = 2;
	public SplatlingChargingTickableSound chargingSound;
	protected SplatlingItem(String settingsId)
	{
		super(settingsId, new Item.Settings().maxCount(1)
			.component(SplatcraftComponents.WEAPON_PRECISION_DATA, SplatcraftComponents.WeaponPrecisionData.DEFAULT)
			.component(SplatcraftComponents.CHARGE, 0f)
		);
	}
	public static RegistrySupplier<SplatlingItem> create(DeferredRegister<Item> register, String settings, String name)
	{
		return register.register(name, () -> new SplatlingItem(settings));
	}
	public static RegistrySupplier<SplatlingItem> create(DeferredRegister<Item> register, RegistrySupplier<SplatlingItem> parent, String name)
	{
		return register.register(name, () -> new SplatlingItem(parent.get().settingsId.toString()));
	}
	@Environment(EnvType.CLIENT)
	protected static void playChargeReadySound(PlayerEntity player, float pitch)
	{
		if (ClientUtils.getClientPlayer() != null && ClientUtils.getClientPlayer().getUuid().equals(player.getUuid()))
			MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SplatcraftSounds.splatlingReady, pitch, MinecraftClient.getInstance().options.getSoundVolume(SoundCategory.PLAYERS)));
	}
	// its time for boilerplate code
	public static float getScaledShotSettingFloat(SplatlingWeaponSettings settings, float charge, Function<SplatlingWeaponSettings.ShotDataRecord, Float> getter)
	{
		float min = getter.apply(settings.firstChargeLevelShot);
		float max = getter.apply(settings.secondChargeLevelShot);
		return min + (max - min) * MathHelper.clamp(charge, 0, 1);
	}
	public static float getScaledProjectileSettingFloat(SplatlingWeaponSettings settings, float charge, Function<CommonRecords.ProjectileDataRecord, Float> getter)
	{
		float min = getter.apply(settings.firstChargeLevelProjectile);
		float max = getter.apply(settings.secondChargeLevelProjectile);
		return min + (max - min) * MathHelper.clamp(charge, 0, 1);
	}
	public static int getScaledShotSettingInt(SplatlingWeaponSettings settings, float charge, Function<SplatlingWeaponSettings.ShotDataRecord, Integer> getter)
	{
		float min = getter.apply(settings.firstChargeLevelShot);
		float max = getter.apply(settings.secondChargeLevelShot);
		return Math.round(min + (max - min) * MathHelper.clamp(charge, 0, 1));
	}
	public static int getScaledProjectileSettingInt(SplatlingWeaponSettings settings, float charge, Function<CommonRecords.ProjectileDataRecord, Integer> getter)
	{
		float min = getter.apply(settings.firstChargeLevelProjectile);
		float max = getter.apply(settings.secondChargeLevelProjectile);
		return Math.round(min + (max - min) * MathHelper.clamp(charge, 0, 1));
	}
	@Override
	public Class<SplatlingWeaponSettings> getSettingsClass()
	{
		return SplatlingWeaponSettings.class;
	}
	@Environment(EnvType.CLIENT)
	protected void playChargingSound(PlayerEntity player, ItemStack stack)
	{
		ClientPlayerEntity clientPlayer = ClientUtils.getClientPlayer();
		if (!Objects.equals(clientPlayer, player))
		{
			return;
		}
		
		SoundEvent soundEvent = PlayerCharge.getChargeValue(player, stack) > 1 ? SplatcraftSounds.splatlingChargeSecondLevel : SplatcraftSounds.splatlingCharge;
		
		if (chargingSound == null || chargingSound.isDone() || !chargingSound.getSoundEvent().equals(soundEvent))
		{
			boolean soundExists = chargingSound != null;
			if (soundExists)
				chargingSound.fadeOut();
			chargingSound = new SplatlingChargingTickableSound(clientPlayer, soundEvent);
			if (soundExists)
				chargingSound.fadeIn();
			MinecraftClient.getInstance().getSoundManager().play(chargingSound);
		}
	}
	@Override
	public void weaponUseTick(World world, LivingEntity entity, ItemStack stack, int remainingUseTicks)
	{
		if (!(entity instanceof PlayerEntity player))
			return;
		
		if (PlayerCooldown.hasPlayerCooldown(player))
			PlayerCooldown.setPlayerCooldown(player, null);
		
		SplatlingWeaponSettings settings = getSettings(stack);
		
		if (world.isClient)
		{
			float prevCharge = PlayerCharge.getChargeValue(player, stack);
			float newCharge = prevCharge + 1f / (prevCharge >= 1 ? settings.chargeData.secondChargeTime() : settings.chargeData.firstChargeTime());
			
			if (!enoughInk(entity, this, MathHelper.lerp(newCharge * 0.5f, 0, settings.inkConsumption), 0, remainingUseTicks % 4 == 0))
			{
				float rechargeMult = InkTankItem.rechargeMult(player.getEquippedStack(EquipmentSlot.CHEST), true);
				if (!hasInkInTank(player, this) || rechargeMult == 0)
					return;
				newCharge = prevCharge + 1f / (prevCharge >= 1 ? settings.chargeData.emptyTankSecondChargeTime() : settings.chargeData.emptyTankFirstChargeTime()) * rechargeMult;
			}
			
			playChargingSound(player, stack);
			
			if (prevCharge < maxCharges && newCharge >= Math.ceil(prevCharge) && prevCharge > 0)
				playChargeReadySound(player, newCharge / maxCharges);
			
			PlayerCharge.addChargeValue(player, stack, newCharge - prevCharge, true, maxCharges);
		}
		else if (remainingUseTicks % 4 == 0 && !enoughInk(entity, this, 0.1f, 0, false))
			playNoInkSound(player, SplatcraftSounds.noInkMain);
	}
	@Override
	public void onPlayerCooldownEnd(World world, PlayerEntity player, ItemStack stack, PlayerCooldown cooldown)
	{
		if (cooldown.getTime() > 0)
		{
			if (!world.isClient)
			{
				SplatlingWeaponSettings settings = getSettings(stack);
				
				float chargeLevel = cooldown.getMaxTime() / (float) settings.chargeData.firingDuration(); //yeah idk about this
				float cooldownLeft = cooldown.getTime() / cooldown.getMaxTime();
				float inkConsumed = MathHelper.lerp(chargeLevel * 0.5f, 0, settings.inkConsumption);
				float inkRefunded = inkConsumed * cooldownLeft;
				
				refundInk(player, inkRefunded);
			}
			else if (PlayerCharge.hasCharge(player) && player.equals(ClientUtils.getClientPlayer()))
			{
				PlayerCharge charge = PlayerCharge.getCharge(player);
				charge.reset();
				SplatcraftPacketHandler.sendToServer(new UpdateChargeStatePacket(false));
			}
		}
	}
	@Override
	public void onPlayerCooldownTick(World world, PlayerEntity player, ItemStack stack, PlayerCooldown cooldown)
	{
		if (world.isClient)
			return;
		
		SplatlingWeaponSettings settings = getSettings(stack);
		float charge = stack.get(SplatcraftComponents.CHARGE);
		
		boolean secondData = charge > 1;
		SplatlingWeaponSettings.ShotDataRecord firingData = secondData ? settings.secondChargeLevelShot : settings.firstChargeLevelShot;
		CommonRecords.ProjectileDataRecord projectileData = secondData ? settings.secondChargeLevelProjectile : settings.firstChargeLevelProjectile;
		
		int firingSpeed = getScaledShotSettingInt(settings, charge, SplatlingWeaponSettings.ShotDataRecord::firingSpeed);
		
		if (firingSpeed > 0 && (cooldown.getTime() - 1) % firingSpeed == 0)
		{
			float inaccuracy = ShotDeviationHelper.updateShotDeviation(stack, world.getRandom(), firingData.accuracyData());
			for (int i = 0; i < firingData.projectileCount(); i++)
			{
				InkProjectileEntity proj = new InkProjectileEntity(world, player, stack, InkBlockUtils.getInkType(player), projectileData.size(), settings);
				proj.setVelocity(player, player.getPitch(), player.getYaw(), firingData.pitchCompensation(), getScaledProjectileSettingFloat(settings, charge, CommonRecords.ProjectileDataRecord::speed),
					inaccuracy);
				proj.setSplatlingStats(settings, charge);
				world.spawnEntity(proj);
			}
			
			world.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.splatlingShot, SoundCategory.PLAYERS, 0.7F, CommonUtils.nextTriangular(world.getRandom(), 0.95F, 0.095F));
		}
	}
	@Override
	public void onReleaseCharge(World world, PlayerEntity player, ItemStack stack, float charge)
	{
		SplatlingWeaponSettings settings = getSettings(stack);
		
		stack.set(SplatcraftComponents.CHARGE, charge);
		
		int cooldownTime = (int) (getDecayTicks(stack) * charge);
		reduceInk(player, this, MathHelper.lerp(charge * 0.5f, 0, settings.inkConsumption), cooldownTime + settings.inkRecoveryCooldown, true, true);
		PlayerCooldown.setPlayerCooldown(player, new PlayerCooldown(stack, cooldownTime, player.getInventory().selectedSlot, player.getActiveHand(), true, false, !settings.chargeData.canRechargeWhileFiring(), player.isOnGround()).setCancellable());
	}
	@Override
	public void onStoppedUsing(@NotNull ItemStack stack, @NotNull World world, LivingEntity entity, int timeLeft)
	{
		super.onStoppedUsing(stack, world, entity, timeLeft);
		
		if (world.isClient && entity instanceof PlayerEntity player && player.equals(ClientUtils.getClientPlayer()))
		{
			if (PlayerCooldown.hasPlayerCooldown(player) && PlayerCooldown.getPlayerCooldown(player).preventWeaponUse())
				return;
			
			PlayerCharge charge = PlayerCharge.getCharge(player);
			
			if (charge == null)
				return;
			if (!SplatcraftKeyHandler.isSquidKeyDown() && charge.charge > 0.05f) //checking for squid key press so it doesn't immediately release charge when squidding
			{
				SplatlingWeaponSettings settings = getSettings(stack);
				PlayerCooldown.setPlayerCooldown(player, new PlayerCooldown(stack, (int) (settings.chargeData.firingDuration() * charge.charge), player.getInventory().selectedSlot, player.getActiveHand(), true, false, !settings.chargeData.canRechargeWhileFiring(), player.isOnGround()).setCancellable());
				SplatcraftPacketHandler.sendToServer(new ReleaseChargePacket(charge.charge, stack, false));
			}
		}
	}
	@Override
	public PlayerPosingHandler.WeaponPose getPose(PlayerEntity player, ItemStack stack)
	{
		return PlayerPosingHandler.WeaponPose.SPLATLING;
	}
	@Override
	public int getDischargeTicks(ItemStack stack)
	{
		return getSettings(stack).chargeData.chargeStorageTime();
	}
	@Override
	public int getDecayTicks(ItemStack stack)
	{
		return getSettings(stack).chargeData.firingDuration();
	}
	@Override
	public EntityAttributeModifier getSpeedModifier(LivingEntity entity, ItemStack stack)
	{
		SplatlingWeaponSettings settings = getSettings(stack);
		
		double appliedMobility = entity.getActiveItem().equals(stack) && settings.chargeData.moveSpeed().isPresent() ? settings.chargeData.moveSpeed().get() : settings.moveSpeed;
		
		return new EntityAttributeModifier(SplatcraftItems.SPEED_MOD_IDENTIFIER, appliedMobility - 1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}
}