package net.splatcraft.items.weapons;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.world.World;
import net.splatcraft.client.audio.ChargerChargingTickableSound;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.entities.ExtraSaveData;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.handlers.PlayerPosingHandler;
import net.splatcraft.items.InkTankItem;
import net.splatcraft.items.weapons.settings.ChargerWeaponSettings;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.ReleaseChargePacket;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.*;
import org.jetbrains.annotations.NotNull;

public class ChargerItem extends WeaponBaseItem<ChargerWeaponSettings> implements IChargeableWeapon
{
	public ChargerChargingTickableSound chargingSound;
	protected ChargerItem(String settingsId)
	{
		super(settingsId);
	}
	public static RegistrySupplier<ChargerItem> create(DeferredRegister<Item> register, String settings, String name)
	{
		return register.register(name, () -> new ChargerItem(settings));
	}
	public static RegistrySupplier<ChargerItem> create(DeferredRegister<Item> register, RegistrySupplier<ChargerItem> parent, String name)
	{
		return register.register(name, () -> new ChargerItem(parent.get().settingsId.toString()));
	}
	@Environment(EnvType.CLIENT)
	protected static void playChargeReadySound(PlayerEntity player)
	{
		if (ClientUtils.getClientPlayer() != null && ClientUtils.getClientPlayer().getUuid().equals(player.getUuid()))
			MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SplatcraftSounds.chargerReady, MinecraftClient.getInstance().options.getSoundVolume(SoundCategory.PLAYERS)));
	}
	@Override
	public Class<ChargerWeaponSettings> getSettingsClass()
	{
		return ChargerWeaponSettings.class;
	}
	@Override
	public void onReleaseCharge(World world, PlayerEntity player, ItemStack stack, float charge)
	{
		ChargerWeaponSettings settings = getSettings(stack);
		
		InkProjectileEntity proj = new InkProjectileEntity(world, player, stack, InkBlockUtils.getInkType(player), settings.projectileData.size(), settings);
		proj.setVelocity(player, player.getPitch(), player.getYaw(), 0.0f, settings.projectileData.speed(), 0.1f);
		proj.setChargerStats(charge, settings.projectileData);
		proj.addExtraData(new ExtraSaveData.ChargeExtraData(charge));
		world.spawnEntity(proj);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.chargerShot, SoundCategory.PLAYERS, 0.7F, CommonUtils.nextTriangular(world.getRandom(), 0.95F, 0.095F));
		reduceInk(player, this, getInkConsumption(stack, charge), settings.shotData.inkRecoveryCooldown(), false, true);
		PlayerCooldown.setPlayerCooldown(player, new PlayerCooldown(stack, settings.shotData.endlagTicks(), player.getInventory().selectedSlot, player.getActiveHand(), true, false, false, player.isOnGround()));
		player.getItemCooldownManager().set(this, 7);
	}
	@Environment(EnvType.CLIENT)
	protected void playChargingSound(PlayerEntity player)
	{
		if (ClientUtils.getClientPlayer() == null || !ClientUtils.getClientPlayer().getUuid().equals(player.getUuid()) || (chargingSound != null && !chargingSound.isDone()))
		{
			return;
		}
		
		chargingSound = new ChargerChargingTickableSound(ClientUtils.getClientPlayer(), SplatcraftSounds.chargerCharge);
		MinecraftClient.getInstance().getSoundManager().play(chargingSound);
	}
	@Override
	public void weaponUseTick(World world, LivingEntity entity, ItemStack stack, int timeLeft)
	{
		if (!world.isClient)
		{
			if (timeLeft % 4 == 0 && !enoughInk(entity, this, 0.1f, 0, false))
				playNoInkSound(entity, SplatcraftSounds.noInkMain);
		}
		else if (entity instanceof PlayerEntity player && !player.getItemCooldownManager().isCoolingDown(this))
		{
			ChargerWeaponSettings settings = getSettings(stack);
			float prevCharge = PlayerCharge.getChargeValue(player, stack);
			float chargeThisFrame = settings.chargeData.getChargePercentPerTick();
			
			if (!entity.isOnGround())
				chargeThisFrame *= settings.chargeData.airborneChargeRate();
			
			if (!enoughInk(entity, this, getInkConsumption(stack, prevCharge + chargeThisFrame), 0, timeLeft % 4 == 0))
			{
				float rechargeMult = InkTankItem.rechargeMult(player.getEquippedStack(EquipmentSlot.CHEST), true);
				
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
	public void onStoppedUsing(@NotNull ItemStack stack, @NotNull World world, LivingEntity entity, int timeLeft)
	{
		super.onStoppedUsing(stack, world, entity, timeLeft);
		
		if (world.isClient && !EntityInfoCapability.isSquid(entity) && entity instanceof PlayerEntity player)
		{
			PlayerCharge charge = PlayerCharge.getCharge(player);
			if (charge != null && charge.charge > 0.05f)
			{
				ChargerWeaponSettings settings = getSettings(stack);
				PlayerCooldown.setPlayerCooldown(player, new PlayerCooldown(stack, settings.shotData.endlagTicks(), player.getInventory().selectedSlot, entity.getActiveHand(), true, false, false, entity.isOnGround()));
				SplatcraftPacketHandler.sendToServer(new ReleaseChargePacket(charge.charge, stack));
				charge.reset();
			}
		}
	}
	public float getInkConsumption(ItemStack stack, float charge)
	{
		ChargerWeaponSettings settings = getSettings(stack);
		return settings.shotData.minInkConsumption() + (settings.shotData.maxInkConsumption() - settings.shotData.minInkConsumption()) * charge;
	}
	@Override
	public PlayerPosingHandler.WeaponPose getPose(PlayerEntity player, ItemStack stack)
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