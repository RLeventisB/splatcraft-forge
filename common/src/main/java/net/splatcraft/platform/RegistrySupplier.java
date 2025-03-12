package net.splatcraft.platform;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class RegistrySupplier<R> implements Holder<R>, Supplier<R>
{
	Holder<R> holder;
	public RegistrySupplier(Holder<R> holder)
	{
		this.holder = holder;
	}
	@Override
	public @NotNull R value()
	{
		return holder.value();
	}
	@Override
	public boolean isBound()
	{
		return holder.isBound();
	}
	@Override
	public boolean is(@NotNull ResourceLocation resourceLocation)
	{
		return holder.is(resourceLocation);
	}
	@Override
	public boolean is(@NotNull ResourceKey<R> resourceKey)
	{
		return holder.is(resourceKey);
	}
	@Override
	public boolean is(@NotNull Predicate<ResourceKey<R>> predicate)
	{
		return holder.is(predicate);
	}
	@Override
	public boolean is(@NotNull TagKey<R> tagKey)
	{
		return holder.is(tagKey);
	}
	@Override
	public boolean is(Holder<R> holder)
	{
		return holder.is(holder);
	}
	@Override
	public @NotNull Stream<TagKey<R>> tags()
	{
		return holder.tags();
	}
	@Override
	public @NotNull Either<ResourceKey<R>, R> unwrap()
	{
		return holder.unwrap();
	}
	@Override
	public @NotNull Optional<ResourceKey<R>> unwrapKey()
	{
		return holder.unwrapKey();
	}
	@Override
	public @NotNull Kind kind()
	{
		return holder.kind();
	}
	@Override
	public boolean canSerializeIn(@NotNull HolderOwner<R> holderOwner)
	{
		return holder.canSerializeIn(holderOwner);
	}
	@Override
	public R get()
	{
		return holder.value();
	}
}
