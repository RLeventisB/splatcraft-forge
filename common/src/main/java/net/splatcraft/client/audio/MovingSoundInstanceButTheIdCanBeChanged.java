package net.splatcraft.client.audio;

import net.minecraft.client.sound.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

public abstract class MovingSoundInstanceButTheIdCanBeChanged implements TickableSoundInstance, SoundInstance
{
	protected final SoundCategory category;
	protected Identifier id;
	protected Sound sound;
	protected float volume;
	protected float pitch;
	protected double x;
	protected double y;
	protected double z;
	protected boolean repeat;
	protected int repeatDelay;
	protected SoundInstance.AttenuationType attenuationType;
	protected boolean relative;
	protected Random random;
	private boolean done;
	protected MovingSoundInstanceButTheIdCanBeChanged(Identifier id, SoundCategory category, Random random)
	{
		volume = 1.0F;
		pitch = 1.0F;
		attenuationType = AttenuationType.LINEAR;
		this.id = id;
		this.category = category;
		this.random = random;
	}
	public boolean isDone()
	{
		return done;
	}
	protected final void setDone()
	{
		done = true;
		repeat = false;
	}
	public Identifier getId()
	{
		return id;
	}
	public WeightedSoundSet getSoundSet(SoundManager soundManager)
	{
		if (id.equals(SoundManager.INTENTIONALLY_EMPTY_ID))
		{
			sound = SoundManager.INTENTIONALLY_EMPTY_SOUND;
			return SoundManager.INTENTIONALLY_EMPTY_SOUND_SET;
		}
		else
		{
			WeightedSoundSet weightedSoundSet = soundManager.get(id);
			if (weightedSoundSet == null)
			{
				sound = SoundManager.MISSING_SOUND;
			}
			else
			{
				sound = weightedSoundSet.getSound(random);
			}
			
			return weightedSoundSet;
		}
	}
	public Sound getSound()
	{
		return sound;
	}
	public SoundCategory getCategory()
	{
		return category;
	}
	public boolean isRepeatable()
	{
		return repeat;
	}
	public int getRepeatDelay()
	{
		return repeatDelay;
	}
	public float getVolume()
	{
		return volume * sound.getVolume().get(random);
	}
	public float getPitch()
	{
		return pitch * sound.getPitch().get(random);
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
	public SoundInstance.AttenuationType getAttenuationType()
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
