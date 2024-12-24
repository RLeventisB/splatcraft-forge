package net.splatcraft.registries.fabric;

import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;

public class SplatcraftEntitiesImpl
{
	public static void registerDataTracker(String name, TrackedDataHandler<?> handler)
	{
		TrackedDataHandlerRegistry.register(handler);
	}
}
