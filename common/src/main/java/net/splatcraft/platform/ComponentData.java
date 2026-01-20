package net.splatcraft.platform;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.*;

public final class ComponentData<OBJ, COMPONENT>
{
	private final ResourceLocation id;
	private final Class<OBJ> holderClass;
	private final Class<COMPONENT> componentClass;
	private final Codec<COMPONENT> codec;
	private final StreamCodec<? super ByteBuf, COMPONENT> streamCodec;
	private final byte bitId;
	private ComponentExecutor<OBJ, COMPONENT> executor;
	public ComponentData(ResourceLocation id, Class<OBJ> holderClass, Class<COMPONENT> componentClass, Codec<COMPONENT> codec)
	{
		this(id, holderClass, componentClass, codec, ByteBufCodecs.fromCodec(codec));
	}
	public ComponentData(ResourceLocation id, Class<OBJ> holderClass, Class<COMPONENT> componentClass, Codec<COMPONENT> codec, StreamCodec<? super ByteBuf, COMPONENT> streamCodec)
	{
		this.id = id;
		this.holderClass = holderClass;
		this.componentClass = componentClass;
		this.codec = codec;
		this.streamCodec = streamCodec;
		bitId = Components.getNextBitId(this);
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
	public boolean doesntHaveOrNot(@NotNull OBJ holder, Predicate<COMPONENT> predicate)
	{
		return hasAnd(holder, predicate);
	}
	public COMPONENT update(@NotNull OBJ holder, UnaryOperator<COMPONENT> updator)
	{
		COMPONENT applied = updator.apply(get(holder));
		set(holder, applied);
		return applied;
	}
	public COMPONENT updateOrCreate(@NotNull OBJ holder, COMPONENT defaultComponent, UnaryOperator<COMPONENT> updator)
	{
		COMPONENT component = get(holder);
		if (component == null)
			component = defaultComponent;

		COMPONENT applied = updator.apply(component);
		set(holder, applied);
		return applied;
	}
	public COMPONENT updateOrCreate(@NotNull OBJ holder, UnaryOperator<COMPONENT> updator)
	{
		COMPONENT applied = updator.apply(getOrCreate(holder));
		set(holder, applied);
		return applied;
	}
	public boolean hasChangedAfterUpdate(@NotNull OBJ holder, UnaryOperator<COMPONENT> updator)
	{
		COMPONENT old = get(holder);
		COMPONENT newComponent = update(holder, updator);
		return old != newComponent;
	}
	public boolean hasChangedAfterUpdateOrCreate(@NotNull OBJ holder, UnaryOperator<COMPONENT> updator)
	{
		COMPONENT old = get(holder);
		COMPONENT newComponent = updateOrCreate(holder, updator);
		return old != newComponent;
	}
	public boolean hasChangedAfterUpdateOrCreate(@NotNull OBJ holder, COMPONENT defaultComponent, UnaryOperator<COMPONENT> updator)
	{
		COMPONENT old = get(holder);
		COMPONENT newComponent = updateOrCreate(holder, defaultComponent, updator);
		return old != newComponent;
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
	public byte getBitId()
	{
		return bitId;
	}
	public Codec<COMPONENT> getCodec()
	{
		return codec;
	}
	public StreamCodec<? super ByteBuf, COMPONENT> getStreamCodec()
	{
		return streamCodec;
	}
	public Class<OBJ> getHolderClass()
	{
		return holderClass;
	}
	public Class<COMPONENT> getComponentClass()
	{
		return componentClass;
	}
	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof ComponentData<?, ?> that)) return false;
		return bitId == that.bitId && Objects.equals(id, that.id) && Objects.equals(holderClass, that.holderClass) && Objects.equals(componentClass, that.componentClass) && Objects.equals(codec, that.codec) && Objects.equals(streamCodec, that.streamCodec) && Objects.equals(executor, that.executor);
	}
	@Override
	public int hashCode()
	{
		return Objects.hash(id, holderClass, componentClass, codec, streamCodec, bitId, executor);
	}
	@Override
	public String toString()
	{
		return "ComponentData{" +
		       "bitId=" + bitId +
		       ", id=" + id +
		       ", holderClass=" + holderClass +
		       ", componentClass=" + componentClass +
		       ", codec=" + codec +
		       ", streamCodec=" + streamCodec +
		       ", executor=" + executor +
		       '}';
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
