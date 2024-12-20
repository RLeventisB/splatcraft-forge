package net.splatcraft.entities;

import net.minecraft.entity.damage.DamageSource;
import net.splatcraft.util.InkColor;

public interface IColoredEntity
{
    InkColor getColor();

    void setColor(InkColor color);

    default boolean onEntityInked(DamageSource source, float damage, InkColor color)
    {
        return false;
    }

    default boolean handleInkOverlay()
    {
        return false;
    }
}
