package net.splatcraft.platform;

import com.mojang.brigadier.arguments.ArgumentType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Registry;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfo;
import net.splatcraft.platform.event.CommandRegistrationEvent;
import net.splatcraft.platform.event.LifecycleEvents;
import net.splatcraft.platform.services.IPlatformHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class FabricPlatformHelper implements IPlatformHelper
{
	private static MinecraftServer server = null;
	// This is bad... need to think of a better cascade, recursion instead of stack?
	private static boolean claim(int[] ret, BitSet data, int claimed, int elements)
	{
		Queue<Integer> pending = new LinkedList<>();
		pending.add(claimed);

		while (pending.peek() != null)
		{
			int test = pending.poll();
			int offset = (test + 2) * elements;
			int used = data.nextSetBit(offset) - offset;

			if (used >= elements || used < 0)
				throw new IllegalStateException("What? We matched something, but it wasn't set in the range of this test! Test: " + test + " Used: " + used);

			data.set(used);
			data.set(elements + test);
			ret[used] = test;

			for (int x = 0; x < elements; x++)
			{
				offset = (x + 2) * elements;
				if (data.get(offset + used) && !data.get(elements + x))
				{
					data.clear(offset + used);
					int count = 0;
					for (int y = offset; y < offset + elements; y++)
						if (data.get(y))
							count++;

					if (count == 0)
						return false; //Claiming this caused another test to lose its last match..

					if (count == 1)
						pending.add(x);
				}
			}
		}

		return true;
	}
	//We use recursion here, why? Because I feel like it. Also because we should only ever be working in data sets < 9
	private static boolean backtrack(java.util.BitSet data, int[] ret, int start, int elements)
	{
		int test = data.nextClearBit(elements + start) - elements;
		if (test >= elements)
			return true; //Could not find the next unused test.

		if (test < 0)
			throw new IllegalStateException("This should never happen, negative test in backtrack!");

		int offset = (test + 2) * elements;
		for (int x = 0; x < elements; x++)
		{
			if (!data.get(offset + x) || data.get(x))
				continue;

			data.set(x);

			if (backtrack(data, ret, test + 1, elements))
			{
				ret[x] = test;
				return true;
			}

			data.clear(x);
		}

		return false;
	}
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
	public SaveInfo getSaveInfo()
	{
		return null;
	}
	@Override
	public void setSaveInfo(SaveInfo newData)
	{

	}
	@Override
	public InkOverlayInfo getInkOverlayInfo(LivingEntity entity)
	{
		return null;
	}
	@Override
	public boolean hasInkOverlayInfo(LivingEntity entity)
	{
		return false;
	}
	@Override
	public void setInkOverlayInfo(LivingEntity entity, InkOverlayInfo newData)
	{

	}
	@Override
	public EntityInfo getEntityInfo(LivingEntity entity)
	{
		return null;
	}
	@Override
	public boolean hasEntityInfo(LivingEntity entity)
	{
		return false;
	}
	@Override
	public void setEntityInfo(LivingEntity entity, EntityInfo newData)
	{

	}
	// taken from https://github.com/neoforged/NeoForge/blob/79d86eb0a94a29652901bcd8a93f4e0817296a80/src/main/java/net/neoforged/neoforge/common/util/RecipeMatcher.java#L16
	@Override
	public <T> int @Nullable [] findItemMatches(List<T> inputs, List<? extends Predicate<T>> tests)
	{
		int elements = inputs.size();
		if (elements != tests.size())
			return null; // There will not be a 1:1 mapping of inputs -> tests

		int[] ret = new int[elements];
		Arrays.fill(ret, -1);

		// [UnusedInputs] [UnusedIngredients] [IngredientMatchMask]...
		BitSet data = new BitSet((elements + 2) * elements);
		for (int x = 0; x < elements; x++)
		{
			int matched = 0;
			int offset = (x + 2) * elements;
			Predicate<T> test = tests.get(x);

			for (int y = 0; y < elements; y++)
			{
				if (data.get(y))
					continue;

				if (test.test(inputs.get(y)))
				{
					data.set(offset + y);
					matched++;
				}
			}

			if (matched == 0)
				return null; //We have an test that matched non of the inputs

			if (matched == 1)
			{
				if (!claim(ret, data, x, elements))
					return null; //We failed to claim this index, which means it caused something else to go to 0 matches, which makes the whole thing fail
			}
		}

		if (data.nextClearBit(0) >= elements) //All items have been used, which means all tests have a match!
			return ret;

		// We should be in a state where multiple tests are satified by multiple inputs. So we need to try a branching recursive test.
		// However for performance reasons, we should probably make that check a sub-set of the entire graph.
		if (backtrack(data, ret, 0, elements))
			return ret;

		return null; //Backtrack failed, no matches, we cry and go home now :( <- so true..,.,
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
	public void registerReloadListener(PackType packType, PreparableReloadListener reloadListener)
	{

	}

	@Override
	public void registerKeyMapping(KeyMapping key)
	{

	}

	@Override
	public <T extends BlockEntity> void registerBlockEntityRenderer(@NotNull Supplier<BlockEntityType<T>> type, BlockEntityRendererProvider<T> provider)
	{

	}

	@Override
	public <T extends Entity> void registerEntityRenderer(@NotNull Supplier<? extends EntityType<? extends T>> type, EntityRendererProvider<T> provider)
	{

	}

	@Override
	public void registerEntityLayerRenderer(@NotNull ModelLayerLocation location, Supplier<LayerDefinition> layerDefinitionSupplier)
	{

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