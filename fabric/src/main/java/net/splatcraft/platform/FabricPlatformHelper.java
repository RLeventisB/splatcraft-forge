package net.splatcraft.platform;

import com.mojang.brigadier.arguments.ArgumentType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Registry;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.platform.event.CommandRegistrationEvent;
import net.splatcraft.platform.event.LifecycleEvents;
import net.splatcraft.platform.services.IPlatformHelper;

public class FabricPlatformHelper implements IPlatformHelper
{
	private static MinecraftServer server = null;
	public void init()
	{
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) ->
			invokeConsumerEvent(CommandRegistrationEvent.class, dispatcher, context, selection));
		if (isClientSide())
		{
			registerClientSideEvents();
		}
		else
		{
			registerServerSideEvents();
		}
	}
	@Environment(EnvType.SERVER)
	private void registerServerSideEvents()
	{
		ServerLifecycleEvents.SERVER_STARTING.register(server ->
		{
			invokeConsumerEvent(LifecycleEvents.ServerStarted.class, server);
			FabricPlatformHelper.server = server;
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server ->
		{
			invokeConsumerEvent(LifecycleEvents.ServerStopped.class, server);
			FabricPlatformHelper.server = null;
		});
	}
	@Environment(EnvType.CLIENT)
	private void registerClientSideEvents()
	{
		ClientLifecycleEvents.CLIENT_STARTED.register((client) ->
			invokeConsumerEvent(LifecycleEvents.ClientStarted.class, client));
		ClientLifecycleEvents.CLIENT_STOPPING.register((client) ->
			invokeConsumerEvent(LifecycleEvents.ClientStopped.class, client));
	}
	@Override
	public String getPlatformName()
	{
		return "Fabric";
	}
	@Override
	public boolean isModLoaded(String modId)
	{
		return FabricLoader.getInstance().isModLoaded(modId);
	}
	@Override
	public boolean isDevelopmentEnvironment()
	{
		return FabricLoader.getInstance().isDevelopmentEnvironment();
	}
	@Override
	public boolean isClientSide()
	{
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
	}
	@Override
	public boolean hasChunkInk(ChunkAccess chunk)
	{
		return false;
	}
	@Override
	public boolean hasAndIsNotEmptyChunkInk(ChunkAccess chunk)
	{
		return false;
	}
	@Override
	public ChunkInk getChunkInk(ChunkAccess chunk)
	{
		return null;
	}
	@Override
	public void setChunkInk(ChunkAccess chunk, ChunkInk newData)
	{
	
	}
	@Override
	public void registerItemProperty(Item item, ResourceLocation id, ClampedItemPropertyFunction function)
	{
		ItemProperties.register(item.asItem(), id, function);
	}
	@Override
	public <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>> void registerCommandArgument(String argumentName, Class<A> infoClass, I argumentTypeInfo)
	{
		ArgumentTypeRegistry.registerArgumentType(
			Splatcraft.identifierOf(argumentName),
			infoClass,
			argumentTypeInfo
		);
	}
	@Override
	public void registerDataTracker(String name, EntityDataSerializer<?> handler)
	{
		EntityDataSerializers.registerSerializer(handler);
	}
	@Override
	public MinecraftServer getServerInstance()
	{
		if (isClientSide() && server == null)
		{
			server = Minecraft.getInstance().getSingleplayerServer();
		}
		return server;
	}
	@Override
	public <T> DeferredRegister<T> createRegistry(Registry<T> registry)
	{
		return new FabricDeferredRegister<>(registry, Splatcraft.MODID);
	}
}