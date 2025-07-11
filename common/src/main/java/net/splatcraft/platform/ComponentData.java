package net.splatcraft.platform;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.*;

public final class ComponentData<OBJ, COMPONENT>
{
	private final ResourceLocation id;
	private ComponentExecutor<OBJ, COMPONENT> executor;
	public ComponentData(ResourceLocation id)
	{
		this.id = id;
	}
	public static <OBJ, COMPONENT> void registerExecutor(ComponentData<OBJ, COMPONENT> componentData,
	                                                     ComponentExecutor<OBJ, COMPONENT> executor)
	{
		componentData.executor = executor;
	}
	public static <OBJ, COMPONENT> void registerExecutor(ComponentData<OBJ, COMPONENT> componentData,
	                                                     Function<OBJ, Optional<COMPONENT>> getter,
	                                                     BiConsumer<OBJ, COMPONENT> setter,
	                                                     Consumer<OBJ> eraser,
	                                                     Supplier<COMPONENT> creator)
	{
		componentData.executor = new ComponentExecutor<>(getter.andThen(v -> v.orElse(null)), setter, eraser, creator);
	}
	public @NotNull COMPONENT create()
	{
		return executor.creator().get();
	}
	public COMPONENT get(@NotNull OBJ holder)
	{
		return executor.getter().apply(holder);
	}
	public void set(@NotNull OBJ holder, COMPONENT component)
	{
		executor.setter().accept(holder, component);
	}
	public void erase(@NotNull OBJ holder)
	{
		executor.eraser().accept(holder);
	}
	public boolean has(@NotNull OBJ holder)
	{
		return get(holder) != null;
	}
	public void setOrErase(@NotNull OBJ holder, @Nullable COMPONENT component)
	{
		if (component == null)
			erase(holder);
		set(holder, component);
	}
	public boolean hasAnd(@NotNull OBJ holder, Predicate<COMPONENT> predicate)
	{
		COMPONENT component = get(holder);
		return component != null && predicate.test(component);
	}
	public void update(@NotNull OBJ holder, UnaryOperator<COMPONENT> updator)
	{
		set(holder, updator.apply(get(holder)));
	}
	public void update(@NotNull OBJ holder, COMPONENT defaultComponent, UnaryOperator<COMPONENT> updator)
	{
		COMPONENT component = get(holder);
		if (component == null)
			component = defaultComponent;
		
		set(holder, updator.apply(component));
	}
	@NotNull
	public COMPONENT getOrCreate(@NotNull OBJ holder)
	{
		return getOrCreate(holder, this::create);
	}
	@NotNull
	public COMPONENT getOrCreate(@NotNull OBJ holder, @NotNull Supplier<@NotNull COMPONENT> creator)
	{
		COMPONENT component = get(holder);
		if (component == null)
		{
			component = creator.get();
			set(holder, component);
		}
		return component;
	}
	public Optional<COMPONENT> getOptional(@NotNull OBJ holder)
	{
		return Optional.ofNullable(get(holder));
	}
	public ResourceLocation id()
	{
		return id;
	}
	@Override
	public int hashCode()
	{
		return Objects.hash(id, executor);
	}
	@Override
	public String toString()
	{
		return "Component[" +
			"id=" + id + ", " +
			"executor=" + executor + ']';
	}
	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof ComponentData<?, ?> componentData)) return false;
		return Objects.equals(id, componentData.id) && Objects.equals(executor, componentData.executor);
	}
	public record ComponentExecutor<OBJ, COMPONENT>(
		Function<@NotNull OBJ, COMPONENT> getter,
		BiConsumer<@NotNull OBJ, COMPONENT> setter,
		Consumer<@NotNull OBJ> eraser,
		Supplier<@NotNull COMPONENT> creator
	)
	{
	}
}
