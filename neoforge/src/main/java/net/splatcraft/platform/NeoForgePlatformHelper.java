package net.splatcraft.platform;

import com.mojang.brigadier.arguments.ArgumentType;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.neoforge.SplatcraftNeoForgeDataAttachments;
import net.splatcraft.platform.event.*;
import net.splatcraft.platform.services.IPlatformHelper;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class NeoForgePlatformHelper implements IPlatformHelper
{
	private static final NeoForgeDeferredRegister<EntityDataSerializer<?>> DATA_SERIALIZER_REGISTRY = new NeoForgeDeferredRegister<>(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS, Splatcraft.MODID);
	private static final List<PreparableReloadListener> serverDataPacks = new ObjectArrayList<>();
	public static NeoForgePlatformHelper INSTANCE;
	public static NeoForgeDeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_REGISTRY = new NeoForgeDeferredRegister<>(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, Splatcraft.MODID);
	private final Map<ResourceKey<CreativeModeTab>, List<RegistrySupplier<Item>>> creativeTabAppends = new HashMap<>();
	private final List<KeyMapping> keyMappingsToAdd = new ObjectArrayList<>();
	private final Map<ModelLayerLocation, Supplier<LayerDefinition>> layerDefinitionsToAdd = new Object2ObjectOpenHashMap<>();
	public void init()
	{
		INSTANCE = this;
		if (isClientSide())
			registerClientSideEvents();
		
		EventHelper.registerEvent(RegisterCommandsEvent.class, (evt) ->
			invokeConsumerEvent(CommandRegistrationEvent.class, evt.getDispatcher(), evt.getBuildContext(), evt.getCommandSelection())
		);
		EventHelper.registerEvent(ServerStartingEvent.class, (evt) ->
			invokeConsumerEvent(LifecycleEvents.ServerStarting.class, evt.getServer())
		);
		EventHelper.registerEvent(ServerStartedEvent.class, (evt) ->
			invokeConsumerEvent(LifecycleEvents.ServerStarted.class, evt.getServer())
		);
		EventHelper.registerEvent(ServerStoppedEvent.class, (evt) ->
			invokeConsumerEvent(LifecycleEvents.ServerStopped.class, evt.getServer())
		);
		EventHelper.registerEvent(PlayerTickEvent.Pre.class, (evt) ->
			invokeConsumerEvent(TickEvents.PlayerBefore.class, evt.getEntity())
		);
		EventHelper.registerEvent(PlayerTickEvent.Post.class, (evt) ->
			invokeConsumerEvent(TickEvents.PlayerAfter.class, evt.getEntity())
		);
		EventHelper.registerEvent(LevelTickEvent.Pre.class, (evt) ->
			{
				if (evt.getLevel() instanceof ServerLevel serverLevel)
					invokeConsumerEvent(TickEvents.ServerLevelBefore.class, serverLevel);
			}
		);
		EventHelper.registerEvent(LevelTickEvent.Post.class, (evt) ->
			{
				if (evt.getLevel() instanceof ServerLevel serverLevel)
					invokeConsumerEvent(TickEvents.ServerLevelAfter.class, serverLevel);
			}
		);
		EventHelper.registerEvent(ServerTickEvent.Pre.class, (evt) ->
			invokeConsumerEvent(TickEvents.ServerBefore.class, evt.getServer())
		);
		EventHelper.registerEvent(ServerTickEvent.Post.class, (evt) ->
			invokeConsumerEvent(TickEvents.ServerAfter.class, evt.getServer())
		);
		EventHelper.registerEvent(BuildCreativeModeTabContentsEvent.class, (evt) ->
			{
				List<RegistrySupplier<Item>> itemsToAdd = creativeTabAppends.get(evt.getTabKey());
				if (itemsToAdd != null)
				{
					for (var holder : itemsToAdd)
					{
						evt.accept(new ItemStack(holder));
					}
				}
			}
		);
		EventHelper.registerEvent(PlayerInteractEvent.LeftClickEmpty.class, (evt) ->
			{
				if (evt.getSide() == LogicalSide.CLIENT)
					invokeConsumerEvent(InteractionEvents.ClientLeftClickAir.class, evt.getEntity(), evt.getHand(), evt.getFace(), evt.getItemStack(), evt.getLevel(), evt.getPos());
			}
		);
		EventHelper.registerEvent(PlayerInteractEvent.RightClickEmpty.class, (evt) ->
			{
				if (evt.getSide() == LogicalSide.CLIENT)
					invokeConsumerEvent(InteractionEvents.ClientRightClickAir.class, evt.getEntity(), evt.getHand(), evt.getFace(), evt.getItemStack(), evt.getLevel(), evt.getPos());
			}
		);
		EventHelper.registerEvent(PlayerInteractEvent.LeftClickBlock.class, (evt) ->
			{
				EventResult result = invokeEvent(InteractionEvents.LeftClickBlock.class, evt.getEntity(), evt.getHand(), evt.getFace(), evt.getItemStack(), evt.getLevel(), evt.getPos());
				result.value.ifPresent(evt::setCanceled);
			}
		);
		EventHelper.registerEvent(PlayerInteractEvent.RightClickBlock.class, (evt) ->
			{
				EventResult result = invokeEvent(InteractionEvents.RightClickBlock.class, evt.getEntity(), evt.getHand(), evt.getFace(), evt.getItemStack(), evt.getLevel(), evt.getPos());
				evt.setCancellationResult(result.convertToInteractionResult());
				if (result.interrupts)
					evt.setCanceled(true);
			}
		);
		EventHelper.registerEvent(PlayerInteractEvent.RightClickItem.class, (evt) ->
			{
				EventResult result = invokeEvent(InteractionEvents.RightClickItem.class, evt.getEntity(), evt.getHand(), evt.getFace(), evt.getItemStack(), evt.getLevel(), evt.getPos());
				evt.setCancellationResult(result.convertToInteractionResult());
				if (result.interrupts)
					evt.setCanceled(true);
			}
		);
		EventHelper.registerEvent(PlayerInteractEvent.EntityInteract.class, (evt) ->
			{
				EventResult result = invokeEvent(InteractionEvents.InteractEntity.class, evt.getEntity(), evt.getHand(), evt.getFace(), evt.getEntity(), evt.getLevel(), evt.getPos());
				evt.setCancellationResult(result.convertToInteractionResult());
				if (result.interrupts)
					evt.setCanceled(true);
			}
		);
		EventHelper.registerEvent(PlayerEvent.Clone.class, (evt) ->
			invokeConsumerEvent(PlayerEvents.PlayerClone.class, (ServerPlayer) evt.getOriginal(), (ServerPlayer) evt.getEntity(), !evt.isWasDeath())
		);
		EventHelper.registerEvent(AttackEntityEvent.class, (evt) ->
			{
				EventResult result = invokeEvent(PlayerEvents.AttackEntity.class, evt.getEntity(), evt.getEntity().level(), evt.getTarget(), evt.getEntity().getUsedItemHand(), null);
				if (result.interrupts)
					evt.setCanceled(true);
			}
		);
		EventHelper.registerEvent(InputEvent.MouseScrollingEvent.class, (evt) ->
			{
				EventResult result = invokeEvent(ClientRawInputEvent.MouseScrolled.class, Minecraft.getInstance(), evt.getMouseX(), evt.getMouseY());
				if (result.interruptsOrFalse())
					evt.setCanceled(true);
			}
		);
		EventHelper.registerEvent(InputEvent.MouseButton.class, (evt) ->
			invokeConsumerEvent(ClientRawInputEvent.MouseClicked.class, Minecraft.getInstance(), evt.getButton(), evt.getAction(), evt.getModifiers())
		);
		EventHelper.registerEvent(InputEvent.Key.class, (evt) ->
			invokeConsumerEvent(ClientRawInputEvent.KeyPressed.class, Minecraft.getInstance(), evt.getKey(), evt.getScanCode(), evt.getAction(), evt.getModifiers())
		);
		EventHelper.registerEvent(LivingDeathEvent.class, (evt) ->
			{
				EventResult result = invokeEvent(EntityEvents.LivingDeath.class, evt.getEntity(), evt.getSource());
				if (result.interruptsOrFalse())
					evt.setCanceled(true);
			}
		);
		EventHelper.registerEvent(ClientChatReceivedEvent.class, (evt) ->
			{
				CompoundEventResult<Component> result = invokeCompoundEvent(InteractionEvents.ClientChatReceive.class, evt.getBoundChatType(), evt.getMessage(), evt.getSender());
				if (result.value() != null)
				{
					evt.setMessage(result.value());
				}
				if (result.result().interruptsOrFalse())
					evt.setCanceled(true);
			}
		);
		EventHelper.registerEvent(PlayerEvent.PlayerLoggedInEvent.class, (evt) ->
			invokeConsumerEvent(PlayerEvents.LogIn.class, (ServerPlayer) evt.getEntity())
		);
		EventHelper.registerEvent(PlayerEvent.PlayerLoggedOutEvent.class, (evt) ->
			invokeConsumerEvent(PlayerEvents.Quit.class, (ServerPlayer) evt.getEntity())
		);
		
		EventHelper.registerEvent(AddReloadListenerEvent.class, (evt) ->
			serverDataPacks.forEach(evt::addListener)
		);
		EventHelper.registerEvent(RegisterKeyMappingsEvent.class, (evt) ->
			keyMappingsToAdd.forEach(evt::register)
		);
		EventHelper.registerEvent(EntityRenderersEvent.RegisterLayerDefinitions.class, (evt) ->
			layerDefinitionsToAdd.forEach(evt::registerLayerDefinition)
		);
		EventHelper.registerEvent(BlockEvent.BreakEvent.class, (evt) ->
			{
				EventResult result = invokeEvent(InteractionEvents.BlockBreak.class, evt.getPlayer(), evt.getLevel(), evt.getPos(), evt.getState());
				if (result.interruptsOrFalse())
					evt.setCanceled(true);
			}
		);
	}
	@OnlyIn(Dist.CLIENT)
	private void registerClientSideEvents()
	{
		EventHelper.registerEvent(ClientTickEvent.Pre.class, (evt) ->
			invokeConsumerEvent(TickEvents.ClientBefore.class, Minecraft.getInstance())
		);
		EventHelper.registerEvent(ClientTickEvent.Post.class, (evt) ->
			invokeConsumerEvent(TickEvents.ClientAfter.class, Minecraft.getInstance())
		);
		EventHelper.registerEvent(LevelTickEvent.Pre.class, (evt) ->
			{
				if (evt.getLevel() instanceof ClientLevel clientLevel)
					invokeConsumerEvent(TickEvents.ClientLevelBefore.class, clientLevel);
			}
		);
		EventHelper.registerEvent(LevelTickEvent.Post.class, (evt) ->
			{
				if (evt.getLevel() instanceof ClientLevel clientLevel)
					invokeConsumerEvent(TickEvents.ClientLevelAfter.class, clientLevel);
			}
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
	@Override
	public void addItemToVanillaCreativeTab(ResourceKey<CreativeModeTab> creativeTab, RegistrySupplier<Item> item)
	{
		creativeTabAppends.computeIfAbsent(creativeTab, v -> new ObjectArrayList<>()).add(item);
	}
	@Override
	public void registerReloadListener(PackType packType, PreparableReloadListener reloadListener)
	{
		if (packType == PackType.SERVER_DATA)
			serverDataPacks.add(reloadListener);
	}
	@Override
	public void registerKeyMapping(KeyMapping key)
	{
		keyMappingsToAdd.add(key);
	}
	@Override
	public <T extends BlockEntity> void registerBlockEntityRenderer(@NotNull BlockEntityType<T> type, BlockEntityRendererProvider<T> provider)
	{
		BlockEntityRenderers.register(type, provider);
	}
	@Override
	public <T extends Entity> void registerEntityRenderer(@NotNull Supplier<? extends EntityType<? extends T>> type, EntityRendererProvider<T> provider)
	{
		EntityRenderers.register(type.get(), provider);
	}
	@Override
	public void registerEntityLayerRenderer(@NotNull ModelLayerLocation location, Supplier<LayerDefinition> layerDefinitionSupplier)
	{
		layerDefinitionsToAdd.put(location, layerDefinitionSupplier);
	}
	@Override
	public void registerAttribute(Supplier<? extends EntityType<? extends LivingEntity>> type, Supplier<AttributeSupplier.Builder> attribute)
	{
	
	}
}