package net.splatcraft.platform;

import java.util.function.Supplier;

public interface DeferredRegister<T>
{
	<R extends T> RegistrySupplier<R> register(String path, Supplier<R> supplier);
}
