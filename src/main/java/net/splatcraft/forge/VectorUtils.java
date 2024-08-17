package net.splatcraft.forge;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class VectorUtils
{
    public static Vec3 lerp(double progress, Vec3 position, Vec3 lastPosition)
    {
        double x = Mth.lerp(progress, position.x(), lastPosition.x());
        double y = Mth.lerp(progress, position.y(), lastPosition.y());
        double z = Mth.lerp(progress, position.z(), lastPosition.z());
        return new Vec3(x, y, z);
    }
}
