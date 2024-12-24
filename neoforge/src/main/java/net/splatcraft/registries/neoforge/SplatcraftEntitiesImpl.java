package net.splatcraft.registries.neoforge;

import net.minecraft.entity.data.TrackedDataHandler;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.splatcraft.Splatcraft;

public class SplatcraftEntitiesImpl
{
	public static final DeferredRegister<TrackedDataHandler<?>> REGISTRY = DeferredRegister.create(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS, Splatcraft.MODID);
	public static void registerDataTracker(String name, TrackedDataHandler<?> handler)
	{
		REGISTRY.register(name, () -> handler);
	}
}
