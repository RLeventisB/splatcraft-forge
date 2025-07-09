package net.splatcraft.platform.services;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Registry;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfo;
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.platform.ModSide;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.platform.event.IEventMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
	// yes this could've used architectury api because these are literally the same functions from https://github.com/architectury/architectury-api
	// however, that is one more jar and i am FRIGHTENED that the mod doesnt let me hotswap
	@OnlyIn(Dist.CLIENT)
	void registerItemProperty(Item item, ResourceLocation id, ClampedItemPropertyFunction function);
	<A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>> void registerCommandArgument(String argumentName, Class<A> infoClass, I argumentTypeInfo);
	void registerDataTracker(String name, EntityDataSerializer<?> handler);
	MinecraftServer getServerInstance();
	<T> DeferredRegister<T> createRegistry(Registry<T> registry);
	void addItemToVanillaCreativeTab(ResourceKey<CreativeModeTab> creativeTab, RegistrySupplier<Item> item);
	void registerReloadListener(PackType packType, PreparableReloadListener reloadListener);
	void registerKeyMapping(KeyMapping key);
	<T extends BlockEntity> void registerBlockEntityRenderer(@NotNull Supplier<BlockEntityType<T>> type, BlockEntityRendererProvider<T> provider);
	<T extends Entity> void registerEntityRenderer(@NotNull Supplier<? extends EntityType<? extends T>> type, EntityRendererProvider<T> provider);
	void registerEntityLayerRenderer(@NotNull ModelLayerLocation location, Supplier<LayerDefinition> layerDefinitionSupplier);
	void registerAttribute(Supplier<? extends EntityType<? extends LivingEntity>> type, Supplier<AttributeSupplier.Builder> attribute);
	void loadConfig();
	void initializeConfigs();
	Path getModConfigPath();
	// todo: do a neoforge-like abstract capability id thingy that is resolved when serializing and deserializing by the modloader or something
	boolean hasChunkInk(ChunkAccess chunk);
	boolean hasAndIsNotEmptyChunkInk(ChunkAccess chunk);
	ChunkInk getChunkInk(ChunkAccess chunk);
	void setChunkInk(ChunkAccess chunk, ChunkInk newData);
	SaveInfo getSaveInfo();
	void setSaveInfo(SaveInfo newData);
	InkOverlayInfo getInkOverlayInfo(LivingEntity entity);
	boolean hasInkOverlayInfo(LivingEntity entity);
	void setInkOverlayInfo(LivingEntity entity, InkOverlayInfo newData);
	EntityInfo getEntityInfo(LivingEntity entity);
	boolean hasEntityInfo(LivingEntity entity);
	void setEntityInfo(LivingEntity entity, EntityInfo newData);
	<T> int @Nullable [] findItemMatches(List<T> inputs, List<? extends Predicate<T>> tests);
	void postConsumerEvent(String eventClassName, Object... params);
	Object postEvent(String eventClassName, Object... params);
	Collection<ModInfo> getMods();
	default boolean anyModThat(Predicate<ModInfo> predicate)
	{
		return getMods().stream().anyMatch(predicate);
	}
}