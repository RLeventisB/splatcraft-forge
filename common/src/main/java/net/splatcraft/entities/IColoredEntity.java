package net.splatcraft.entities;

import net.minecraft.entity.damage.DamageSource;

public interface IColoredEntity
{
    int getColor();

    void setColor(int color);

    default boolean onEntityInked(DamageSource source, float damage, int color)
    {
        return false;
    }

    default boolean handleInkOverlay()
    {
        return false;
    }
}
