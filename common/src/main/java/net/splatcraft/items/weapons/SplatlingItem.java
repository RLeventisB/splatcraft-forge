package net.splatcraft.items.weapons;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.PlayerCharge;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.EntityCooldown;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

public class SplatlingItem extends WeaponBaseItem<SplatlingWeaponSettings> implements IChargeableWeapon
{
	private static final int maxCharges = 2;
	public SplatlingChargingTickableSound chargingSound;
	protected SplatlingItem(String settingsId)
	{
		super(settingsId, new Item.Properties().stacksTo(1)
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
		return register.register(name, () -> new SplatlingItem(parent.value().settingsId.toString()));
	}
	@Environment(EnvType.CLIENT)
	protected static void playChargeReadySound(Player player, float pitch)
	{
		if (ClientUtils.getClientPlayer() != null && ClientUtils.getClientPlayer().getUUID().equals(player.getUUID()))
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SplatcraftSounds.splatlingReady, pitch, Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.PLAYERS)));
	}
	// its time for boilerplate code
	public static float getScaledShotSettingFloat(SplatlingWeaponSettings settings, float charge, Function<SplatlingWeaponSettings.ShotDataRecord, Float> getter)
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
	public static int getScaledShotSettingInt(SplatlingWeaponSettings settings, float charge, Function<SplatlingWeaponSettings.ShotDataRecord, Integer> getter)
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
	@Override
	public Class<SplatlingWeaponSettings> getSettingsClass()
	{
		return SplatlingWeaponSettings.class;
	}
	@Environment(EnvType.CLIENT)
	protected void playChargingSound(Player player, ItemStack stack)
	{
		LocalPlayer clientPlayer = ClientUtils.getClientPlayer();
		if (!Objects.equals(clientPlayer, player))
		{
			return;
		}
		
		SoundEvent soundEvent = PlayerCharge.getChargeValue(player, stack) > 1 ? SplatcraftSounds.splatlingChargeSecondLevel : SplatcraftSounds.splatlingCharge;
		
		if (chargingSound == null || chargingSound.isStopped() || !chargingSound.getSoundEvent().equals(soundEvent))
		{
			boolean soundExists = chargingSound != null;
			if (soundExists)
				chargingSound.fadeOut();
			chargingSound = new SplatlingChargingTickableSound(clientPlayer, soundEvent);
			if (soundExists)
				chargingSound.fadeIn();
			Minecraft.getInstance().getSoundManager().play(chargingSound);
		}
	}
	@Override
	public void weaponUseTick(Level world, LivingEntity entity, ItemStack stack, int remainingUseTicks)
	{
		if (!(entity instanceof Player player))
			return;
		
		if (EntityAction.hasEntityAction(player))
			EntityAction.setEntityAction(player, null);
		
		SplatlingWeaponSettings settings = getSettings(stack);
		
		if (world.isClientSide)
		{
			float prevCharge = PlayerCharge.getChargeValue(player, stack);
			float newCharge = prevCharge + 1f / (prevCharge >= 1 ? settings.chargeData.secondChargeTime() : settings.chargeData.firstChargeTime());
			
			if (!enoughInk(entity, this, Mth.lerp(newCharge * 0.5f, 0, settings.inkConsumption), 0, remainingUseTicks % 4 == 0))
			{
				float rechargeMult = InkTankItem.rechargeMult(player.getItemBySlot(EquipmentSlot.CHEST), true);
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
			else if (PlayerCharge.hasCharge(player) && player.equals(ClientUtils.getClientPlayer()))
			{
				PlayerCharge charge = PlayerCharge.getCharge(player);
				charge.reset();
				SplatcraftPacketHandler.sendToServer(new UpdateChargeStatePacket(false));
			}
		}
	}
	@Override
	public void onPlayerCooldownTick(Level world, Player player, ItemStack stack, EntityAction action)
	{
		if (world.isClientSide)
			return;
		
		SplatlingWeaponSettings settings = getSettings(stack);
		float charge = stack.get(SplatcraftComponents.CHARGE);
		
		boolean secondData = charge > 1;
		SplatlingWeaponSettings.ShotDataRecord firingData = secondData ? settings.secondChargeLevelShot : settings.firstChargeLevelShot;
		CommonRecords.ProjectileDataRecord projectileData = secondData ? settings.secondChargeLevelProjectile : settings.firstChargeLevelProjectile;
		
		int firingSpeed = getScaledShotSettingInt(settings, charge, SplatlingWeaponSettings.ShotDataRecord::firingSpeed);
		
		if (firingSpeed > 0 && (action.getTime() - 1) % firingSpeed == 0)
		{
			float inaccuracy = ShotDeviationHelper.updateShotDeviation(stack, world.getRandom(), firingData.accuracyData());
			for (int i = 0; i < firingData.projectileCount(); i++)
			{
				InkProjectileEntity proj = new InkProjectileEntity(world, player, stack, InkBlockUtils.getInkType(player), projectileData.size(), settings);
				proj.shootFromRotation(player, player.getXRot(), player.getYRot(), firingData.pitchCompensation(), getScaledShotSettingFloat(settings, charge, SplatlingWeaponSettings.ShotDataRecord::projectileSpeed),
					inaccuracy);
				proj.setSplatlingStats(settings, charge);
				world.addFreshEntity(proj);
			}
			
			world.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.splatlingShot, SoundSource.PLAYERS, 0.7F, CommonUtils.nextTriangular(world.getRandom(), 0.95F, 0.095F));
		}
	}
	@Override
	public void onReleaseCharge(Level world, Player player, ItemStack stack, float charge)
	{
		SplatlingWeaponSettings settings = getSettings(stack);
		
		stack.set(SplatcraftComponents.CHARGE, charge);
		
		int cooldownTime = (int) (getDecayTicks(stack) * charge);
		reduceInk(player, this, Mth.lerp(charge * 0.5f, 0, settings.inkConsumption), cooldownTime + settings.inkRecoveryCooldown, true, true);
		EntityAction.setEntityAction(player, new EntityCooldown(stack, cooldownTime, player.getInventory().selected, player.getUsedItemHand(), true, false, !settings.chargeData.canRechargeWhileFiring(), player.onGround()).setCancellable());
	}
	@Override
	public void releaseUsing(@NotNull ItemStack stack, @NotNull Level world, LivingEntity entity, int timeLeft)
	{
		super.releaseUsing(stack, world, entity, timeLeft);
		
		if (world.isClientSide && entity instanceof Player player && player.equals(ClientUtils.getClientPlayer()))
		{
			if (EntityAction.hasActionAnd(player, EntityAction::preventWeaponUse))
				return;
			
			PlayerCharge charge = PlayerCharge.getCharge(player);
			
			if (charge == null)
				return;
			if (!SplatcraftKeyHandler.isSquidKeyDown() && charge.charge > 0.05f) //checking for squid key press so it doesn't immediately release charge when squidding
			{
				SplatlingWeaponSettings settings = getSettings(stack);
				EntityAction.setEntityAction(player, new EntityCooldown(stack, (int) (settings.chargeData.firingDuration() * charge.charge), player.getInventory().selected, player.getUsedItemHand(), true, false, !settings.chargeData.canRechargeWhileFiring(), player.onGround()).setCancellable());
				SplatcraftPacketHandler.sendToServer(new ReleaseChargePacket(charge.charge, stack, false));
			}
		}
	}
	@Override
	public PlayerPosingHandler.WeaponPose getPose(Player player, ItemStack stack)
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
	public AttributeModifier getSpeedModifier(LivingEntity entity, ItemStack stack)
	{
		SplatlingWeaponSettings settings = getSettings(stack);
		
		double appliedMobility = entity.getUseItem().equals(stack) && settings.chargeData.moveSpeed().isPresent() ? settings.chargeData.moveSpeed().get() : settings.moveSpeed;
		
		return new AttributeModifier(SplatcraftItems.SPEED_MOD_IDENTIFIER, appliedMobility - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}
}