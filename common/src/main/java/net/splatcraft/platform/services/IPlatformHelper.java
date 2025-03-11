package net.splatcraft.platform.services;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Registry;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.platform.ModSide;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.platform.event.IEventMap;

public interface IPlatformHelper extends IEventMap
{
	/**
	 * Gets the name of the current platform
	 *
	 * @return The name of the current platform.
	 */
	String getPlatformName();
	/**
	 * Checks if a mod with the given id is loaded.
	 *
	 * @param modId The mod to check if it is loaded.
	 * @return True if the mod is loaded, false otherwise.
	 */
	boolean isModLoaded(String modId);
	/**
	 * Check if the game is currently in a development environment.
	 *
	 * @return True if in a development environment, false otherwise.
	 */
	boolean isDevelopmentEnvironment();
	/**
	 * Gets the name of the environment type as a string.
	 *
	 * @return The name of the environment type.
	 */
	default String getEnvironmentName()
	{
		return isDevelopmentEnvironment() ? "development" : "production";
	}
	boolean isClientSide();
	default IPlatformHelper start()
	{
		init();
		return this;
	}
	void init();
	default ModSide getModSide()
	{
		return isClientSide() ? ModSide.CLIENT : ModSide.SERVER;
	}
	boolean hasChunkInk(ChunkAccess chunk);
	boolean hasAndIsNotEmptyChunkInk(ChunkAccess chunk);
	ChunkInk getChunkInk(ChunkAccess chunk);
	void setChunkInk(ChunkAccess chunk, ChunkInk newData);
	// yes this could've used architectury api because these are literally the same functions from https://github.com/architectury/architectury-api
	// however, that is one more jar and i am FRIGHTENED that the mod doesnt let me hotswap
	void registerItemProperty(Item item, ResourceLocation id, ClampedItemPropertyFunction function);
	<A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>> void registerCommandArgument(String argumentName, Class<A> infoClass, I argumentTypeInfo);
	void registerDataTracker(String name, EntityDataSerializer<?> handler);
	MinecraftServer getServerInstance();
	<T> DeferredRegister<T> createRegistry(Registry<T> registry);
	void addItemToVanillaCreativeTab(ResourceKey<CreativeModeTab> creativeTab, RegistrySupplier<Item> item);
}