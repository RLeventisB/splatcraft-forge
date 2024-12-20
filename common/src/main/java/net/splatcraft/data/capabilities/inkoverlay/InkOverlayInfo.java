package net.splatcraft.data.capabilities.inkoverlay;

import net.minecraft.nbt.NbtCompound;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;

public class InkOverlayInfo
{
    private InkColor color = ColorUtils.getDefaultColor();
    private float amount = 0;
    private InkColor woolColor = InkColor.INVALID;
    private boolean inkproof = false;
    private double squidRot;
    private double squidRotO;

    public InkOverlayInfo()
    {
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

    public InkColor getWoolColor()
    {
        return woolColor;
    }

    public void setWoolColor(InkColor v)
    {
        woolColor = v;
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

        if (getWoolColor().isValid())
            nbt.put("WoolColor", getWoolColor().getNbt());

        return nbt;
    }

    public void readNBT(NbtCompound nbt)
    {
        setColor(InkColor.getFromNbt(nbt.get("Color")));
        setAmount(nbt.getFloat("Amount"));
        setInkproof(nbt.getBoolean("Inkproof"));

        if (nbt.contains("WoolColor"))
            setWoolColor(InkColor.getFromNbt(nbt.get("WoolColor")));
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
