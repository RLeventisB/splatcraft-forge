package net.splatcraft.data.capabilities.inkoverlay;

import net.minecraft.nbt.NbtCompound;
import net.splatcraft.util.ColorUtils;

public class InkOverlayInfo
{
    private int color = ColorUtils.DEFAULT;
    private float amount = 0;
    private int woolColor = -1;
    private boolean inkproof = false;
    private double squidRot;
    private double squidRotO;

    public InkOverlayInfo()
    {
    }

    public int getColor()
    {
        return color;
    }

    public void setColor(int color)
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

    public int getWoolColor()
    {
        return woolColor;
    }

    public void setWoolColor(int v)
    {
        this.woolColor = v;
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
        nbt.putInt("Color", getColor());
        nbt.putFloat("Amount", getAmount());
        nbt.putBoolean("Inkproof", isInkproof());

        if (getWoolColor() != -1)
            nbt.putInt("WoolColor", getWoolColor());

        return nbt;
    }

    public void readNBT(NbtCompound nbt)
    {
        setColor(ColorUtils.getColorFromNbt(nbt));
        setAmount(nbt.getFloat("Amount"));
        setInkproof(nbt.getBoolean("Inkproof"));

        if (nbt.contains("WoolColor"))
            setWoolColor(nbt.getInt("WoolColor"));
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
