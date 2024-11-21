package net.splatcraft.forge.util;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.minecraft.core.Direction;

public class SodiumUtils
{
    // i didnt use this too!
    public static Direction toDirection(ModelQuadFacing quadFacing)
    {
        return switch (quadFacing)
        {
            case NEG_Y -> Direction.DOWN;
            case POS_Y -> Direction.UP;
            case NEG_Z -> Direction.NORTH;
            case POS_Z -> Direction.SOUTH;
            case NEG_X -> Direction.WEST;
            case POS_X -> Direction.EAST;
            default -> throw new IncompatibleClassChangeError();
        };
    }
}
