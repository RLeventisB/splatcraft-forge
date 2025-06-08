package net.splatcraft.platform;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
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
import net.neoforged.bus.api.Event;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
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
import net.splatcraft.SplatcraftConfigImpl;
import net.splatcraft.SplatcraftNeoForgeDataAttachments;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.platform.event.*;
import net.splatcraft.platform.services.IPlatformHelper;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class NeoForgePlatformHelper implements IPlatformHelper
{
	public static final NeoForgeDeferredRegister<EntityDataSerializer<?>> DATA_SERIALIZER_REGISTRY = new NeoForgeDeferredRegister<>(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS, Splatcraft.MODID);
	public static NeoForgeDeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_REGISTRY = new NeoForgeDeferredRegister<>(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, Splatcraft.MODID);
	public static NeoForgePlatformHelper INSTANCE;

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
		EventHelper.registerEvent(InputEvent.MouseButton.Pre.class, (evt) ->
			invokeConsumerEvent(ClientRawInputEvent.PreMouseClicked.class, Minecraft.getInstance(), evt.getButton(), evt.getAction(), evt.getModifiers())
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
	public SaveInfo getSaveInfo()
	{
		if (Services.PLATFORM.isClientSide() && !Minecraft.getInstance().isLocalServer())
			return SaveInfoCapability.clientSaveInfo;
		return Services.PLATFORM.getServerInstance().overworld().getData(SplatcraftNeoForgeDataAttachments.SAVE_INFO);
	}

	@Override
	public void setSaveInfo(SaveInfo newData)
	{
		if (Services.PLATFORM.isClientSide() && !Minecraft.getInstance().isLocalServer())
			throw new NotImplementedException("SaveInfo cannot be set on the client");
		Services.PLATFORM.getServerInstance().overworld().setData(SplatcraftNeoForgeDataAttachments.SAVE_INFO, newData);
	}
	@Override
	public InkOverlayInfo getInkOverlayInfo(LivingEntity entity)
	{
		return entity.getData(SplatcraftNeoForgeDataAttachments.INK_OVERLAY);
	}

	@Override
	public boolean hasInkOverlayInfo(LivingEntity entity)
	{
		return entity.hasData(SplatcraftNeoForgeDataAttachments.INK_OVERLAY);
	}

	@Override
	public void setInkOverlayInfo(LivingEntity entity, InkOverlayInfo newData)
	{
		entity.setData(SplatcraftNeoForgeDataAttachments.INK_OVERLAY, newData);
	}

	@Override
	public EntityInfo getEntityInfo(LivingEntity entity)
	{
		return entity.getData(SplatcraftNeoForgeDataAttachments.ENTITY_INFO);
	}

	@Override
	public boolean hasEntityInfo(LivingEntity entity)
	{
		return entity != null && entity.hasData(SplatcraftNeoForgeDataAttachments.ENTITY_INFO);
	}

	@Override
	public void setEntityInfo(LivingEntity entity, EntityInfo newData)
	{
		entity.setData(SplatcraftNeoForgeDataAttachments.ENTITY_INFO, newData);
	}
	@Override
	public <T> int @Nullable [] findItemMatches(List<T> inputs, List<? extends Predicate<T>> tests)
	{
		return RecipeMatcher.findMatches(inputs, tests);
	}
	@Override
	public void postConsumerEvent(String eventClassName, Object... params)
	{
		try
		{
			Constructor<? extends Event> constructor = (Constructor<? extends Event>) Class.forName(eventClassName).getConstructor(Arrays.stream(params).map(Object::getClass).toArray(Class[]::new));
			EventHelper.postEvent(constructor.newInstance(params));
		}
		catch (NoSuchMethodException | ClassNotFoundException | InvocationTargetException | InstantiationException |
		       IllegalAccessException | ClassCastException e)
		{
		}
	}
	@Override
	public Object postEvent(String eventClassName, Object... params)
	{
		try
		{
			Constructor<?> constructor = Class.forName(eventClassName).getConstructor(Arrays.stream(params).map(Object::getClass).toArray(Class[]::new));
			return EventHelper.postEvent((Event) constructor.newInstance(params));
		}
		catch (NoSuchMethodException | ClassNotFoundException | InvocationTargetException | InstantiationException |
		       IllegalAccessException | ClassCastException e)
		{
		}
		return null;
	}

	@OnlyIn(Dist.CLIENT)
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
		EventHelper.addToEventSpecificMap(BuildCreativeModeTabContentsEvent.class, creativeTab, item,
			(buildTab, tabKey, itemToAdd) ->
			{
				if (buildTab.getTabKey().equals(tabKey))
				{
					buildTab.accept(new ItemStack(itemToAdd));
				}
			}
		);
	}

	@Override
	public void registerReloadListener(PackType packType, PreparableReloadListener reloadListener)
	{
		if (packType == PackType.SERVER_DATA)
			EventHelper.addToEventSpecificList(AddReloadListenerEvent.class, reloadListener,
				AddReloadListenerEvent::addListener
			);
	}
	@Override
	public void registerKeyMapping(KeyMapping key)
	{
		EventHelper.addToEventSpecificList(RegisterKeyMappingsEvent.class, key,
			RegisterKeyMappingsEvent::register
		);
	}
	@Override
	public <T extends BlockEntity> void registerBlockEntityRenderer(@NotNull Supplier<BlockEntityType<T>> type, BlockEntityRendererProvider<T> provider)
	{
		EventHelper.addToEventSpecificMap(EntityRenderersEvent.RegisterRenderers.class, type, provider,
			(registerRenderers, blockEntityType, blockEntityRendererProvider) -> registerRenderers.registerBlockEntityRenderer(blockEntityType.get(), blockEntityRendererProvider)
		);
	}

	@Override
	public <T extends Entity> void registerEntityRenderer(@NotNull Supplier<? extends EntityType<? extends T>> type, EntityRendererProvider<T> provider)
	{
		EventHelper.addToEventSpecificMap(EntityRenderersEvent.RegisterRenderers.class, type, provider,
			(registerRenderers, entityType, entityRendererProvider) -> registerRenderers.registerEntityRenderer(entityType.get(), entityRendererProvider)
		);
	}

	@Override
	public void registerEntityLayerRenderer(@NotNull ModelLayerLocation location, Supplier<LayerDefinition> layerDefinitionSupplier)
	{
		EventHelper.addToEventSpecificMap(EntityRenderersEvent.RegisterLayerDefinitions.class, location, layerDefinitionSupplier,
			EntityRenderersEvent.RegisterLayerDefinitions::registerLayerDefinition
		);
	}

	@Override
	public void registerAttribute(Supplier<? extends EntityType<? extends LivingEntity>> type, Supplier<AttributeSupplier.Builder> attributeBuilder)
	{
		EventHelper.addToEventSpecificMap(EntityAttributeCreationEvent.class, type, attributeBuilder,
			(registerAttributes, entityType, attributeSupplier) -> registerAttributes.put(entityType.get(), attributeSupplier.get().build())
		);
	}

	@Override
	public void loadConfig()
	{
		SplatcraftConfigImpl.loadConfig();
	}

	@Override
	public void initializeConfigs()
	{
		SplatcraftConfigImpl.initializeConfigs();
	}

	@Override
	public Path getModConfigPath()
	{
		return SplatcraftConfigImpl.getModConfigPath();
	}
}