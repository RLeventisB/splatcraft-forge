package net.splatcraft.data.capabilities.structs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.splatcraft.util.structs.InkColor;

import java.util.Optional;

public class InkOverlayData
{
	public static final Codec<InkOverlayData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		InkColor.HEX_CODEC.optionalFieldOf("color").forGetter(InkOverlayData::getColor),
		Codec.FLOAT.optionalFieldOf("amount", 0f).forGetter(InkOverlayData::getAmount),
		Codec.BOOL.optionalFieldOf("ink_proof", false).forGetter(InkOverlayData::isInkproof),
		Codec.FLOAT.optionalFieldOf("squid_pitch", 0f).forGetter(InkOverlayData::getSquidPitch),
		Codec.FLOAT.optionalFieldOf("squid_pitch_0", 0f).forGetter(InkOverlayData::getPreviousSquidPitch)
	).apply(inst, InkOverlayData::new));
	private Optional<InkColor> color = Optional.empty();
	private float amount = 0;
	private boolean inkproof = false;
	private float squidPitch;
	private float squidPitchO;
	public InkOverlayData()
	{
	}
	public InkOverlayData(Optional<InkColor> color,
	                      float amount,
	                      boolean inkproof,
	                      float squidRot,
	                      float squidRotO)
	{
		this.color = color;
		this.amount = amount;
		this.inkproof = inkproof;
		squidPitch = squidRot;
		squidPitchO = squidRotO;
	}
	public Optional<InkColor> getColor()
	{
		return color;
	}
	public void setColor(InkColor color)
	{
		this.color = Optional.ofNullable(color);
	}
	public float getAmount()
	{
		return amount;
	}
	public void setAmount(float v)
	{
		amount = Math.max(0, v);
		if (amount == 0)
			setColor(null);
	}
	public void addAmount(float v)
	{
		setAmount(amount + v);
	}
	public float getSquidPitch()
	{
		return squidPitch;
	}
	public void setSquidPitch(float v)
	{
		squidPitchO = squidPitch;
		squidPitch = v;
	}
	public float getPreviousSquidPitch()
	{
		return squidPitchO;
	}
	public String toString()
	{
		return "Color: " + color + " Amount: " + amount;
	}
	public boolean isInkproof()
	{
		return inkproof;
	}
	public void setInkproof(boolean inkproof)
	{
		this.inkproof = inkproof;
	}
}
