package net.splatcraft.forge;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class VectorUtils
{
    public static Vec3 lerp(double progress, Vec3 start, Vec3 end)
    {
        return new Vec3(
            Mth.lerp(progress, start.x(), end.x()),
            Mth.lerp(progress, start.y(), end.y()),
            Mth.lerp(progress, start.z(), end.z())
        );
    }
}
