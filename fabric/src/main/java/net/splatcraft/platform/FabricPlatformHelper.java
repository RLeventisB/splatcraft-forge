package net.splatcraft.platform;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.splatcraft.Splatcraft;
import net.splatcraft.platform.event.CommandRegistrationEvent;
import net.splatcraft.platform.event.LifecycleEvents;
import net.splatcraft.platform.services.IPlatformHelper;
import net.splatcraft.platform.services.ModInfo;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
	public <T> int @Nullable [] findItemMatches(List<T> inputs, List<? extends Predicate<T>> tests)
	{
		return NeoForgeRecipeManager.findMatches(inputs, tests);
	}
	@Override
	public void postConsumerEvent(String eventClassName, Object... params)
	{
	
	}
	@Override
	public Object postEvent(String eventClassName, Object... params)
	{
		return null;
	}
	@Override
	public Collection<ModInfo> getMods()
	{
		return FabricLoader.getInstance().getAllMods().stream()
			.map(ModContainer::getMetadata)
			.map(FabricPlatformHelper::getModInfo).toList();
	}
	private static ModInfo getModInfo(ModMetadata metadata)
	{
		return new ModInfo(
			metadata.getId(),
			metadata.getVersion().toString(),
			metadata.getName(),
			metadata.getDescription()
		);
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
	@Override
	public void registerShader(Function<ResourceProvider, Pair<ShaderInstance, Consumer<ShaderInstance>>> dataProvider)
	{
	}
	@Override
	public void registerRenderingCallback(RenderingCallback.RenderingStage stage, RenderingCallback callback)
	{
		switch (stage)
		{
			case AFTER_SKY:
				WorldRenderEvents.START.register(context ->
					callback.render(convertToFabric(context)));
				
				break;
			case AFTER_BLOCKS:
				WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, hitResult) ->
				{
					callback.render(convertToFabric(context));
					return true;
				});
				break;
			case AFTER_ENTITIES:
				WorldRenderEvents.AFTER_ENTITIES.register(context ->
					callback.render(convertToFabric(context)));
				
				break;
			case AFTER_PARTICLES:
				WorldRenderEvents.AFTER_TRANSLUCENT.register(context ->
					callback.render(convertToFabric(context)));
				break;
			case AFTER_DEBUG:
				WorldRenderEvents.LAST.register(context ->
					callback.render(convertToFabric(context)));
				break;
		}
	}
	public RenderingCallback.CallbackData convertToFabric(WorldRenderContext context)
	{
		return new RenderingCallback.CallbackData(
			context.worldRenderer(),
			context.matrixStack(),
			context.tickCounter(),
			context.camera(),
			context.gameRenderer(),
			context.projectionMatrix(),
			context.positionMatrix(),
			context.frustum(),
			context.consumers()
		);
	}
	@Override
	public void registerReloadListener(PackType packType, PreparableReloadListener reloadListener)
	{
		ResourceLocation id = Splatcraft.identifierOf(CommonUtils.makeStringIdentifierValid(reloadListener.getClass().getSimpleName()));
		ResourceManagerHelper.get(packType).registerReloadListener(new IdentifiableResourceReloadListener()
		{
			@Override
			public ResourceLocation getFabricId()
			{
				return id;
			}
			@Override
			public @NotNull CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor)
			{
				return reloadListener.reload(preparationBarrier, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
			}
		});
	}
	@Override
	public void registerKeyMapping(KeyMapping key)
	{
		KeyBindingHelper.registerKeyBinding(key);
	}
	@Override
	public <T extends ParticleOptions> void registerParticleFactories(ParticleType<T> type, Function<SpriteSet, ParticleProvider<T>> providerCreator)
	{
		ParticleFactoryRegistry.getInstance().register(type, providerCreator::apply);
	}
	@Override
	public <T extends BlockEntity> void registerBlockEntityRenderer(@NotNull Supplier<BlockEntityType<T>> type, BlockEntityRendererProvider<T> provider)
	{
		BlockEntityRenderers.register(type.get(), provider);
	}
	@Override
	public <T extends Entity> void registerEntityRenderer(@NotNull Supplier<? extends EntityType<? extends T>> type, EntityRendererProvider<T> provider)
	{
		EntityRendererRegistry.register(type.get(), provider);
	}
	@Override
	public void registerEntityLayerRenderer(@NotNull ModelLayerLocation location, Supplier<LayerDefinition> layerDefinitionSupplier)
	{
		EntityModelLayerRegistry.registerModelLayer(location, layerDefinitionSupplier::get);
	}
	@Override
	public void registerAttribute(Supplier<? extends EntityType<? extends LivingEntity>> type, Supplier<AttributeSupplier.Builder> attribute)
	{
	
	}
	@Override
	public void loadConfig()
	{
	
	}
	@Override
	public void initializeConfigs()
	{
	
	}
	@Override
	public Path getModConfigPath()
	{
		return null;
	}
}