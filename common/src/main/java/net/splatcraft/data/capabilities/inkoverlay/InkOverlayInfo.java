package net.splatcraft.data.capabilities.inkoverlay;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;

public class InkOverlayInfo
{
	public static final Codec<InkOverlayInfo> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		InkColor.CODEC.fieldOf("color").forGetter(InkOverlayInfo::getColor),
		Codec.FLOAT.fieldOf("amount").forGetter(InkOverlayInfo::getAmount),
		Codec.BOOL.optionalFieldOf("ink_proof", false).forGetter(InkOverlayInfo::isInkproof),
		Codec.DOUBLE.fieldOf("squid_rot").forGetter(InkOverlayInfo::getSquidRot),
		Codec.DOUBLE.fieldOf("squid_rot_0").forGetter(InkOverlayInfo::getSquidRotO)
	).apply(inst, InkOverlayInfo::new));
	private InkColor color = ColorUtils.getDefaultColor();
	private float amount = 0;
	private boolean inkproof = false;
	private double squidRot;
	private double squidRotO;
	public InkOverlayInfo()
	{
	}
	public InkOverlayInfo(InkColor color,
	                      float amount,
	                      boolean inkproof,
	                      double squidRot,
	                      double squidRotO)
	{
		this.color = color;
		this.amount = amount;
		this.inkproof = inkproof;
		this.squidRot = squidRot;
		this.squidRotO = squidRotO;
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
	public double getSquidRot()
	{
		return squidRot;
	}
	public void setSquidRot(double v)
	{
		squidRotO = squidRot;
		squidRot = v;
	}
	public double getSquidRotO()
	{
		return squidRotO;
	}
	public NbtCompound writeNBT(NbtCompound nbt)
	{
		nbt.put("Color", getColor().getNbt());
		nbt.putFloat("Amount", getAmount());
		nbt.putBoolean("Inkproof", isInkproof());
		
		return nbt;
	}
	public void readNBT(NbtCompound nbt)
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
