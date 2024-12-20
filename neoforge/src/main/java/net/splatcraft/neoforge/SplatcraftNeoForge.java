package net.splatcraft.neoforge;

import net.neoforged.fml.common.Mod;
import net.splatcraft.Splatcraft;

@Mod(Splatcraft.MODID)
public final class SplatcraftNeoForge
{
    public SplatcraftNeoForge()
    {
        // Run our common setup.
        Splatcraft.init();
    }
}
