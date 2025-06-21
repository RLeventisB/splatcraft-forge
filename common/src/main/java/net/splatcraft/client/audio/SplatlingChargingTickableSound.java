package net.splatcraft.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.items.weapons.IChargeableWeapon;
import org.jetbrains.annotations.Nullable;

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

		if (player.isAlive() && player.getUseItem().getItem() instanceof IChargeableWeapon chargeableWeapon && EntityInfoCapability.hasCapability(player))
		{
			EntityInfo info = EntityInfoCapability.get(player);
			if (!info.isSquid())
			{
				float charge = chargeableWeapon.getCharge(player.getUseItem());
				float prevCharge = chargeableWeapon.getPreviousCharge(player.getUseItem());

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
				return;
			}
		}
		stop();
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
