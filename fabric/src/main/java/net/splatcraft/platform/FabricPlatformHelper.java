package net.splatcraft.platform;

import com.mojang.brigadier.arguments.ArgumentType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Registry;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
		
		ServerLifecycleEvents.SERVER_STARTING.register(server1 ->
		{
			invokeConsumerEvent(LifecycleEvents.ServerStarting.class, server1);
			server = server1;
		});
		ServerLifecycleEvents.SERVER_STARTED.register(server1 ->
		{
			invokeConsumerEvent(LifecycleEvents.ServerStarted.class, server1);
			server = server1;
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server1 ->
		{
			invokeConsumerEvent(LifecycleEvents.ServerStopped.class, server1);
			server = null;
		});
	}
	@OnlyIn(Dist.CLIENT)
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
	@Override
	public void addItemToVanillaCreativeTab(ResourceKey<CreativeModeTab> creativeTab, RegistrySupplier<Item> item)
	{
		ItemGroupEvents.modifyEntriesEvent(creativeTab).register((tab) ->
			tab.accept(new ItemStack(item)));
	}
}