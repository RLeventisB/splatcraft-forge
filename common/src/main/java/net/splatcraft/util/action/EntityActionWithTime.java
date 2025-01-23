package net.splatcraft.util.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public abstract class EntityActionWithTime implements EntityAction
{
	protected float time, maxTime;
	public EntityActionWithTime(float time)
	{
		this(time, time);
	}
	public EntityActionWithTime(float time, float maxTime)
	{
		this.time = time;
		this.maxTime = maxTime;
	}
	public static <T extends EntityActionWithTime> RecordCodecBuilder<T, Float> getTimeCodec()
	{
		return Codec.FLOAT.fieldOf("time").forGetter(EntityActionWithTime::getTime);
	}
	public static <T extends EntityActionWithTime> RecordCodecBuilder<T, Float> getMaxTimeCodec()
	{
		return Codec.FLOAT.fieldOf("max_time").forGetter(EntityActionWithTime::getMaxTime);
	}
	@Override
	public float getTime()
	{
		return time;
	}
	@Override
	public EntityAction setTime(float time)
	{
		this.time = time;
		return this;
	}
	@Override
	public float getMaxTime()
	{
		return maxTime;
	}
	@Override
	public EntityAction setMaxTime(float maxTime)
	{
		this.maxTime = maxTime;
		return this;
	}
}
