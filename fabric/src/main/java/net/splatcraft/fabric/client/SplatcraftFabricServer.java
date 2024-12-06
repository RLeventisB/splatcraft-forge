package net.splatcraft.fabric.client;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.splatcraft.Splatcraft;

public final class SplatcraftFabricServer implements DedicatedServerModInitializer
{
    @Override
    public void onInitializeServer()
    {
        Splatcraft.initServer();
        // en miencraft java se puedne plantar cualqueir tipo de de abetos
    }
}
