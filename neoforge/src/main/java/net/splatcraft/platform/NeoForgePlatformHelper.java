package net.splatcraft.platform;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.neoforge.SplatcraftNeoForgeDataAttachments;
import net.splatcraft.platform.event.CommandRegistrationEvent;
import net.splatcraft.platform.event.TickEvents;
import net.splatcraft.platform.services.IPlatformHelper;

public class NeoForgePlatformHelper implements IPlatformHelper
{
	private static final NeoForgeDeferredRegister<EntityDataSerializer<?>> DATA_SERIALIZER_REGISTRY = new NeoForgeDeferredRegister<>(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS, Splatcraft.MODID);
	public static NeoForgePlatformHelper INSTANCE;
	public static NeoForgeDeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_REGISTRY = new NeoForgeDeferredRegister<>(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, Splatcraft.MODID);
	public void init()
	{
		INSTANCE = this;
		EventHelper.registerEvent(RegisterCommandsEvent.class, (evt) ->
			invokeConsumerEvent(CommandRegistrationEvent.class, evt.getDispatcher(), evt.getBuildContext(), evt.getCommandSelection())
		);
		EventHelper.registerEvent(PlayerTickEvent.Pre.class, (evt) ->
			invokeConsumerEvent(TickEvents.PlayerBefore.class, evt.getEntity())
		);
		EventHelper.registerEvent(PlayerTickEvent.Post.class, (evt) ->
			invokeConsumerEvent(TickEvents.PlayerAfter.class, evt.getEntity())
		);
	}
	@Override
	public String getPlatformName()
	{
		return "NeoForge";
	}
	@Override
	public boolean isModLoaded(String modId)
	{
		return ModList.get().isLoaded(modId);
	}
	@Override
	public boolean isDevelopmentEnvironment()
	{
		return !FMLLoader.isProduction();
	}
	@Override
	public boolean isClientSide()
	{
		return FMLEnvironment.dist == Dist.CLIENT;
	}
	@Override
	public boolean hasChunkInk(ChunkAccess chunk)
	{
		return chunk.hasData(SplatcraftNeoForgeDataAttachments.CHUNK_INK);
	}
	@Override
	public boolean hasAndIsNotEmptyChunkInk(ChunkAccess chunk)
	{
		return hasChunkInk(chunk) && getChunkInk(chunk).isntEmpty();
	}
	@Override
	public ChunkInk getChunkInk(ChunkAccess chunk)
	{
		return chunk.getData(SplatcraftNeoForgeDataAttachments.CHUNK_INK);
	}
	@Override
	public void setChunkInk(ChunkAccess chunk, ChunkInk newData)
	{
		chunk.setData(SplatcraftNeoForgeDataAttachments.CHUNK_INK, newData);
	}
	@Override
	public void registerItemProperty(Item item, ResourceLocation id, ClampedItemPropertyFunction function)
	{
		ItemProperties.register(item.asItem(), id, function);
	}
	@Override
	public <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>> void registerCommandArgument(String argumentName, Class<A> infoClass, I argumentTypeInfo)
	{
		ARGUMENT_REGISTRY.register(argumentName, () -> ArgumentTypeInfos.registerByClass(infoClass, argumentTypeInfo));
	}
	@Override
	public void registerDataTracker(String name, EntityDataSerializer<?> handler)
	{
		DATA_SERIALIZER_REGISTRY.register(name, () -> handler);
	}
	@Override
	public MinecraftServer getServerInstance()
	{
		return ServerLifecycleHooks.getCurrentServer();
	}
	@Override
	public <T> DeferredRegister<T> createRegistry(Registry<T> registry)
	{
		return new NeoForgeDeferredRegister<>(registry, Splatcraft.MODID);
	}
}