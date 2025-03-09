package net.splatcraft.platform;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class FabricDeferredRegister<T> implements DeferredRegister<T>
{
	private final String modid;
	private final Registry<T> registry;
	public FabricDeferredRegister(Registry<T> registry, String modid)
	{
		this.modid = modid;
		this.registry = registry;
	}
	@Override
	public <R extends T> RegistrySupplier<R> register(String path, Supplier<R> supplier)
	{
		R r = supplier.get();
		Holder.Reference<R> holder = (Holder.Reference<R>) Registry.registerForHolder(registry, ResourceLocation.fromNamespaceAndPath(modid, path), r);
		return new RegistrySupplier<>(holder);
	}
}
