package net.splatcraft.client.audio;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

public abstract class MovingSoundInstanceButTheIdCanBeChanged implements TickableSoundInstance, SoundInstance
{
	protected final SoundSource category;
	protected ResourceLocation id;
	protected Sound sound;
	protected float volume;
	protected float pitch;
	protected double x;
	protected double y;
	protected double z;
	protected boolean repeat;
	protected int repeatDelay;
	protected SoundInstance.Attenuation attenuationType;
	protected boolean relative;
	protected RandomSource random;
	private boolean done;
	protected MovingSoundInstanceButTheIdCanBeChanged(ResourceLocation id, SoundSource category, RandomSource random)
	{
		volume = 1.0F;
		pitch = 1.0F;
		attenuationType = Attenuation.LINEAR;
		this.id = id;
		this.category = category;
		this.random = random;
	}
	public boolean isStopped()
	{
		return done;
	}
	protected final void setDone()
	{
		done = true;
		repeat = false;
	}
	public @NotNull ResourceLocation getLocation()
	{
		return id;
	}
	public WeighedSoundEvents resolve(SoundManager soundManager)
	{
		if (id.equals(SoundManager.INTENTIONALLY_EMPTY_SOUND_LOCATION))
		{
			sound = SoundManager.INTENTIONALLY_EMPTY_SOUND;
			return SoundManager.INTENTIONALLY_EMPTY_SOUND_EVENT;
		}
		else
		{
			WeighedSoundEvents weightedSoundSet = soundManager.getSoundEvent(id);
			if (weightedSoundSet == null)
			{
				sound = SoundManager.EMPTY_SOUND;
			}
			else
			{
				sound = weightedSoundSet.getSound(random);
			}
			
			return weightedSoundSet;
		}
	}
	public @NotNull Sound getSound()
	{
		return sound;
	}
	public @NotNull SoundSource getSource()
	{
		return category;
	}
	public boolean isLooping()
	{
		return repeat;
	}
	public int getDelay()
	{
		return repeatDelay;
	}
	public float getVolume()
	{
		return volume * sound.getVolume().sample(random);
	}
	public float getPitch()
	{
		return pitch * sound.getPitch().sample(random);
	}
	public double getX()
	{
		return x;
	}
	public double getY()
	{
		return y;
	}
	public double getZ()
	{
		return z;
	}
	public SoundInstance.@NotNull Attenuation getAttenuation()
	{
		return attenuationType;
	}
	public boolean isRelative()
	{
		return relative;
	}
	public String toString()
	{
		return "MovingSoundInstanceButTheIdCanBeChanged[" + id + "]";
	}
}
