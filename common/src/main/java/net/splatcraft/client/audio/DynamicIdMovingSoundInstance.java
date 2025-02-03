package net.splatcraft.client.audio;

import net.minecraft.client.sound.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.function.Supplier;

public abstract class DynamicIdMovingSoundInstance implements TickableSoundInstance, SoundInstance
{
	protected final SoundCategory category;
	protected final Supplier<Identifier> idSupplier;
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
	protected DynamicIdMovingSoundInstance(Supplier<Identifier> soundIdSuplier, SoundCategory category, Random random)
	{
		volume = 1.0F;
		pitch = 1.0F;
		attenuationType = AttenuationType.LINEAR;
		idSupplier = soundIdSuplier;
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
		return idSupplier.get();
	}
	public WeightedSoundSet getSoundSet(SoundManager soundManager)
	{
		if (idSupplier.get().equals(SoundManager.INTENTIONALLY_EMPTY_ID))
		{
			sound = SoundManager.INTENTIONALLY_EMPTY_SOUND;
			return SoundManager.INTENTIONALLY_EMPTY_SOUND_SET;
		}
		else
		{
			WeightedSoundSet weightedSoundSet = soundManager.get(idSupplier.get());
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
		return "DynamicIdMovingSoundInstance[" + idSupplier + "]";
	}
}
