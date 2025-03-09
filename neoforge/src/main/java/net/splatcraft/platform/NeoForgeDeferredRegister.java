package net.splatcraft.platform;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Registry;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.splatcraft.neoforge.SplatcraftNeoForge;

import java.util.function.Supplier;

public class NeoForgeDeferredRegister<T> implements DeferredRegister<T>
{
	private static final ObjectArrayList<net.neoforged.neoforge.registries.DeferredRegister<?>> REGISRIES_TO_REGISTER = new ObjectArrayList<>();
	private final net.neoforged.neoforge.registries.DeferredRegister<T> registry;
	protected NeoForgeDeferredRegister(Registry<T> originalRegistry, String namespace)
	{
		registry = net.neoforged.neoforge.registries.DeferredRegister.create(originalRegistry, namespace);
		REGISRIES_TO_REGISTER.add(registry);
	}
	public static void registerAllRegistries()
	{
		for (var registry : REGISRIES_TO_REGISTER)
		{
			registry.register(SplatcraftNeoForge.modBus);
		}
	}
	@Override
	public <R extends T> RegistrySupplier<R> register(String path, Supplier<R> supplier)
	{
		DeferredHolder<T, R> register = registry.register(path, supplier);
		return (RegistrySupplier<R>) new RegistrySupplier<>(register);
	}
}
