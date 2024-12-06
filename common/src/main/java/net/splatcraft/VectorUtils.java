package net.splatcraft;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class VectorUtils
{
    public static Vec3d lerp(double progress, Vec3d start, Vec3d end)
    {
        return new Vec3d(
            MathHelper.lerp(progress, start.x, end.x),
            MathHelper.lerp(progress, start.y, end.y),
            MathHelper.lerp(progress, start.z, end.z)
        );
    }
}
