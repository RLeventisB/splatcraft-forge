package net.splatcraft.forge.entities;

import net.minecraft.world.damagesource.DamageSource;

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
