package net.splatcraft.items.weapons;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.client.audio.ChargerChargingTickableSound;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.entities.ExtraSaveData;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.items.InkTankItem;
import net.splatcraft.items.weapons.settings.ChargerWeaponSettings;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.ReleaseChargePacket;
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.PlayerCharge;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.EntityCooldown;
import org.jetbrains.annotations.NotNull;

public class ChargerItem extends WeaponBaseItem<ChargerWeaponSettings> implements IChargeableWeapon
{
	public ChargerChargingTickableSound chargingSound;
	protected ChargerItem(String settingsId)
	{
		super(settingsId, new Item.Properties().stacksTo(1)
			.component(SplatcraftComponents.WEAPON_PRECISION_DATA, SplatcraftComponents.WeaponPrecisionData.DEFAULT)
			.component(SplatcraftComponents.CHARGE, 0f)
		);
	}
	public static RegistrySupplier<ChargerItem> create(DeferredRegister<Item> register, String settings, String name)
	{
		return register.register(name, () -> new ChargerItem(settings));
	}
	public static RegistrySupplier<ChargerItem> create(DeferredRegister<Item> register, RegistrySupplier<ChargerItem> parent, String name)
	{
		return register.register(name, () -> new ChargerItem(parent.value().settingsId.toString()));
	}
	@Environment(EnvType.CLIENT)
	protected static void playChargeReadySound(Player player)
	{
		if (ClientUtils.getClientPlayer() != null && ClientUtils.getClientPlayer().getUUID().equals(player.getUUID()))
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SplatcraftSounds.chargerReady, Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.PLAYERS)));
	}
	@Override
	public Class<ChargerWeaponSettings> getSettingsClass()
	{
		return ChargerWeaponSettings.class;
	}
	@Override
	public void onReleaseCharge(Level world, Player player, ItemStack stack, float charge)
	{
		ChargerWeaponSettings settings = getSettings(stack);
		
		InkProjectileEntity proj = new InkProjectileEntity(world, player, stack, InkBlockUtils.getInkType(player), settings.projectileData.size(), settings);
		proj.setDeltaMovement(player, player.getXRot(), player.getYRot(), 0.0f, settings.projectileData.speed().getValue(charge), 0f, 0f);
		proj.setChargerStats(charge, settings.projectileData);
		proj.addExtraData(new ExtraSaveData.ChargeExtraData(charge));
		world.addFreshEntity(proj);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.chargerShot, SoundSource.PLAYERS, 0.7F, CommonUtils.nextTriangular(world.getRandom(), 0.95F, 0.095F));
		
		reduceInk(player, this, settings.shotData.inkConsumption().getValue(charge), settings.shotData.inkRecoveryCooldown(), false, true);
		EntityAction.setEntityAction(player, new EntityCooldown(stack, settings.shotData.endlagTicks(), player.getInventory().selected, player.getUsedItemHand(), true, false, false, player.onGround()));
		player.getCooldowns().addCooldown(this, 7);
	}
	@Environment(EnvType.CLIENT)
	protected void playChargingSound(Player player)
	{
		if (ClientUtils.getClientPlayer() == null || !ClientUtils.getClientPlayer().getUUID().equals(player.getUUID()) || (chargingSound != null && !chargingSound.isStopped()))
		{
			return;
		}
		
		chargingSound = new ChargerChargingTickableSound(ClientUtils.getClientPlayer(), SplatcraftSounds.chargerCharge);
		Minecraft.getInstance().getSoundManager().play(chargingSound);
	}
	@Override
	public void weaponUseTick(Level world, LivingEntity entity, ItemStack stack, int remainingUseTicks)
	{
		if (!world.isClientSide)
		{
			if (remainingUseTicks % 4 == 0 && !enoughInk(entity, this, 0.1f, 0, false))
				playNoInkSound(entity, SplatcraftSounds.noInkMain);
		}
		else if (entity instanceof Player player && !player.getCooldowns().isOnCooldown(this))
		{
			ChargerWeaponSettings settings = getSettings(stack);
			float prevCharge = PlayerCharge.getChargeValue(player, stack);
			float chargeThisFrame = settings.chargeData.getChargePercentPerTick();
			
			if (!entity.onGround())
				chargeThisFrame *= settings.chargeData.airborneChargeRate();
			
			if (!enoughInk(entity, this, settings.shotData.inkConsumption().getValue(prevCharge + chargeThisFrame), 0, remainingUseTicks % 4 == 0))
			{
				float rechargeMult = InkTankItem.rechargeMult(player.getItemBySlot(EquipmentSlot.CHEST), true);
				
				if (!hasInkInTank(player, this) || rechargeMult == 0)
					return;
				chargeThisFrame *= settings.chargeData.emptyTankChargeRate();
			}
			
			float newCharge = prevCharge + chargeThisFrame;
			if (prevCharge < 1 && newCharge >= 1)
			{
				playChargeReadySound(player);
			}
			else if (newCharge < 1)
			{
				playChargingSound(player);
			}
			
			PlayerCharge.addChargeValue(player, stack, newCharge - prevCharge, false);
		}
	}
	@Override
	public void releaseUsing(@NotNull ItemStack stack, @NotNull Level world, LivingEntity entity, int timeLeft)
	{
		super.releaseUsing(stack, world, entity, timeLeft);
		
		if (world.isClientSide && !EntityInfoCapability.isSquid(entity) && entity instanceof Player player)
		{
			PlayerCharge charge = PlayerCharge.getCharge(player);
			if (charge != null && charge.charge > 0.05f)
			{
				ChargerWeaponSettings settings = getSettings(stack);
				EntityAction.setEntityAction(player, new EntityCooldown(stack, settings.shotData.endlagTicks(), player.getInventory().selected, entity.getUsedItemHand(), true, false, false, entity.onGround()));
				SplatcraftPacketHandler.sendToServer(new ReleaseChargePacket(charge.charge, stack));
				charge.reset();
			}
		}
	}
	public float getInkConsumption(ItemStack stack, float charge)
	{
		ChargerWeaponSettings settings = getSettings(stack);
		return settings.shotData.inkConsumption().getValue(charge);
	}
	@Override
	public PlayerPosingHandler.WeaponPose getPose(Player player, ItemStack stack)
	{
		return PlayerPosingHandler.WeaponPose.BOW_CHARGE;
	}
	@Override
	public int getDischargeTicks(ItemStack stack)
	{
		return getSettings(stack).chargeData.chargeStorageTime();
	}
	@Override
	public int getDecayTicks(ItemStack stack)
	{
		return 0;
	}
}