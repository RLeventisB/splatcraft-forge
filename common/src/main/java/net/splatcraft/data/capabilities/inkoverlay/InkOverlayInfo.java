package net.splatcraft.data.capabilities.inkoverlay;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;

public class InkOverlayInfo
{
	public static final Codec<InkOverlayInfo> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		InkColor.HEX_CODEC.fieldOf("color").forGetter(InkOverlayInfo::getColor),
		Codec.FLOAT.fieldOf("amount").forGetter(InkOverlayInfo::getAmount),
		Codec.BOOL.optionalFieldOf("ink_proof", false).forGetter(InkOverlayInfo::isInkproof),
		Codec.FLOAT.fieldOf("squid_pitch").forGetter(InkOverlayInfo::getSquidPitch),
		Codec.FLOAT.fieldOf("squid_pitch_0").forGetter(InkOverlayInfo::getPreviousSquidPitch)
	).apply(inst, InkOverlayInfo::new));
	private InkColor color = ColorUtils.getDefaultColor();
	private float amount = 0;
	private boolean inkproof = false;
	private float squidPitch;
	private float squidPitchO;
	public InkOverlayInfo()
	{
	}
	public InkOverlayInfo(InkColor color,
	                      float amount,
	                      boolean inkproof,
	                      float squidRot,
	                      float squidRotO)
	{
		this.color = color;
		this.amount = amount;
		this.inkproof = inkproof;
		this.squidPitch = squidRot;
		this.squidPitchO = squidRotO;
	}
	public InkColor getColor()
	{
		return color;
	}
	public void setColor(InkColor color)
	{
		this.color = color;
	}
	public float getAmount()
	{
		return amount;
	}
	public void setAmount(float v)
	{
		amount = Math.max(0, v);
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
	public CompoundTag writeNBT(CompoundTag nbt)
	{
		nbt.put("Color", getColor().getNbt());
		nbt.putFloat("Amount", getAmount());
		nbt.putBoolean("Inkproof", isInkproof());

		return nbt;
	}
	public void readNBT(CompoundTag nbt)
	{
		setColor(InkColor.getFromNbt(nbt.get("Color")));
		setAmount(nbt.getFloat("Amount"));
		setInkproof(nbt.getBoolean("Inkproof"));
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
