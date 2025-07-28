package net.splatcraft.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.platform.Components;
import net.splatcraft.registries.SplatcraftComponents;

public class ChargerChargingTickableSound extends AbstractTickableSoundInstance
{
	private final Player player;
	private final int totalLevels;
	public ChargerChargingTickableSound(Player player, SoundEvent sound, int totalLevels)
	{
		super(sound, SoundSource.PLAYERS, player.getRandom());
		this.totalLevels = totalLevels;
		attenuation = Attenuation.NONE;
		looping = true;
		delay = 0;
		pitch = 0;
		
		this.player = player;
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
		
		if (!player.isAlive() || !player.getUseItem().has(SplatcraftComponents.CHARGE_DATA) || !Components.ENTITY_INFO.has(player))
		{
			stop();
			return;
		}
		
		if (Components.SQUID_INFO.getOrCreate(player).isSquid())
		{
			stop();
			return;
		}
		
		SplatcraftComponents.ChargeData chargeData = player.getUseItem().get(SplatcraftComponents.CHARGE_DATA);
		float charge = chargeData.charge();
		float prevCharge = chargeData.previousCharge();
		
		if (charge == 0 || (charge >= totalLevels && !isStopped()))
		{
			stop();
			return;
		}
		
		float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
		pitch = (Mth.lerp(partialTick, prevCharge, charge) / totalLevels) * 0.5f + 0.5f;
	}
}
