package net.splatcraft.client.audio;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.util.InkBlockUtils;

public class EnemyInkTickableSound extends AbstractTickableSoundInstance
{
	private final Player player;
	public EnemyInkTickableSound(Player player, SoundEvent sound)
	{
		super(sound, SoundSource.PLAYERS, player.getRandom());
		attenuation = Attenuation.NONE;
		looping = true;
		delay = 0;
		pitch = 0;
		volume = 0f;
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
		
		boolean onEnemyInk = InkBlockUtils.onEnemyInk(player);
		if (!onEnemyInk)
			volume -= 0.03f;
		else
			volume += 0.04f;
		volume = Mth.clamp(volume, 0, 0.4f);
		if ((!onEnemyInk && (volume == 0)) || !player.isAlive())
		{
			stop();
		}
	}
}
