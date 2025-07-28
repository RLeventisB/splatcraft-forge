package net.splatcraft.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.handlers.WeaponHandler;
import net.splatcraft.items.weapons.IChargeableWeapon;
import net.splatcraft.platform.Components;
import net.splatcraft.registries.SplatcraftComponents;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class SplatlingChargingTickableSound extends AbstractTickableSoundInstance
{
	private static final int maxFadeTime = 30;
	private final Player player;
	private final SoundEvent soundEvent;
	private int fadeTime = -1;
	private boolean isFadeIn = false;
	private final int totalLevels;
	@Nullable
	private Boolean playingSecondLevel = null;
	public SplatlingChargingTickableSound(Player player, SoundEvent sound, int totalLevels)
	{
		super(sound, SoundSource.PLAYERS, player.getRandom());
		this.totalLevels = totalLevels;
		attenuation = Attenuation.NONE;
		looping = true;
		delay = 0;
		pitch = 0.5f;
		x = player.getX();
		y = player.getY();
		z = player.getZ();
		
		this.player = player;
		soundEvent = sound;
	}
	@Override
	public boolean canStartSilent()
	{
		return true;
	}
	@Override
	public void tick()
	{
		x = player.getX();
		y = player.getY();
		z = player.getZ();
		
		if (!player.isAlive() || !Components.SQUID_INFO.hasAnd(player, v -> !v.isSquid()))
		{
			stop();
			return;
		}
		
		Optional<InteractionHand> weaponHand = WeaponHandler.getWeaponHand(player, (stack, item) -> item.preventsChanging(stack, player) && item instanceof IChargeableWeapon);
		if (weaponHand.isEmpty())
		{
			stop();
			return;
		}
		ItemStack usedStack = player.getItemInHand(weaponHand.get());
		if (SplatcraftComponents.getOptional(usedStack, SplatcraftComponents.SPLATLING_FIRING_DATA).map(v -> !v.charging().equals(Optional.of(true))).orElse(false))
		{
			stop();
			return;
		}
		IChargeableWeapon chargeableWeapon = (IChargeableWeapon) usedStack.getItem();
		float charge = chargeableWeapon.getCharge(usedStack);
		float prevCharge = chargeableWeapon.getPreviousCharge(usedStack);
		
		if (playingSecondLevel == null)
			playingSecondLevel = charge > 1;
		
		if (!isFadeIn && fadeTime == 0)
		{
			stop();
			return;
		}
		else if (fadeTime > maxFadeTime)
			fadeTime = -1;
		else if (fadeTime > 0)
		{
			fadeTime += isFadeIn ? 1 : -1;
			volume = fadeTime / (float) maxFadeTime;
		}
		
		float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
		pitch = (Mth.lerp(partialTick, prevCharge, charge) / totalLevels) * 0.5f + 0.5f;
	}
	public SoundEvent getSoundEvent()
	{
		return soundEvent;
	}
	public void fadeOut()
	{
		fadeTime = maxFadeTime;
		isFadeIn = false;
	}
	public void fadeIn()
	{
		fadeTime = 0;
		isFadeIn = true;
	}
}
