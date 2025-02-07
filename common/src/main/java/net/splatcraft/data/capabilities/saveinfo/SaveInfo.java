package net.splatcraft.data.capabilities.saveinfo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.platform.Platform;
import it.unimi.dsi.fastutil.objects.Object2ObjectFunction;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.fabricmc.api.EnvType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.splatcraft.data.PlaySession;
import net.splatcraft.data.Stage;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateStageListPacket;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.InkColor;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.function.BiFunction;

public record SaveInfo(Object2ObjectOpenHashMap<String, PlaySession> playSessions,
                       Object2ObjectOpenHashMap<String, Stage> stages,
                       ObjectArrayList<InkColor> colorScores)
{
	public static final Codec<SaveInfo> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		CodecUtils.hashMapCodec(Codec.STRING, PlaySession.CODEC).fieldOf("play_sessions").forGetter(SaveInfo::playSessions),
		CodecUtils.hashMapCodec(Codec.STRING, Stage.CODEC).fieldOf("stages").forGetter(SaveInfo::stages),
		CodecUtils.arrayList(InkColor.HEX_CODEC).fieldOf("color_scores").forGetter(SaveInfo::colorScores)
	).apply(inst, SaveInfo::new));
	public SaveInfo()
	{
		this(new Object2ObjectOpenHashMap<>(), new Object2ObjectOpenHashMap<>(), new ObjectArrayList<>());
	}
	public static void registerEvents()
	{
		TickEvent.SERVER_POST.register(SaveInfo::tickPlaySessions);
		if (Platform.getEnv().equals(EnvType.CLIENT))
			ClientTickEvent.CLIENT_LEVEL_POST.register(SaveInfo::tickPlaySessionsClient);
	}
	private static void tickPlaySessionsClient(ClientWorld world)
	{
		SaveInfo info = SaveInfoCapability.get();
		for (PlaySession session : info.playSessions().values())
		{
			session.tick(null);
		}
	}
	private static void tickPlaySessions(MinecraftServer server)
	{
		SaveInfo info = SaveInfoCapability.get();
		for (PlaySession session : info.playSessions().values())
		{
			session.tick(server);
		}
	}
	private static AssertionError throwException()
	{
		return new AssertionError("This map is not meant to be modified in the client since it won't save! Modify it in the server-side instead.");
	}
	public void addInitializedColorScores(InkColor... colors)
	{
		for (InkColor color : colors)
		{
			if (!colorScores.contains(color))
			{
				colorScores.add(color);
			}
		}
	}
	public void removeColorScore(InkColor color)
	{
		colorScores.remove(color);
	}
	public boolean createOrEditStage(MinecraftServer server, RegistryKey<World> worldKey, String stageId, BlockPos corner1, BlockPos corner2, Text stageName)
	{
		if (stages.containsKey(stageId))
		{
			Stage stage = stages.get(stageId);
			stage.worldKey = worldKey;
			stage.setStageName(stageName);
			stage.updateBounds(stage.getStageWorld(server), corner1, corner2);
		}
		else
			stages.put(stageId, new Stage(server, worldKey, corner1, corner2, stageId, stageName));
		
		SplatcraftPacketHandler.sendToAll(new UpdateStageListPacket(stages));
		return true;
	}
	public boolean createStage(ServerWorld world, String stageId, BlockPos corner1, BlockPos corner2, Text stageName)
	{
		if (world.isClient())
			return false;
		
		if (stages.containsKey(stageId))
			return false;
		
		stages.put(stageId, new Stage(world.getServer(), world.getRegistryKey(), corner1, corner2, stageId, stageName));
		SplatcraftPacketHandler.sendToAll(new UpdateStageListPacket(stages));
		return true;
	}
	public boolean createStage(ServerWorld world, String stageId, BlockPos corner1, BlockPos corner2)
	{
		return createStage(world, stageId, corner1, corner2, Text.literal(stageId));
	}
	// this is mostly so if i do something funny
	public static class ImmutableObjectArrayList<A> extends ObjectArrayList<A>
	{
		@Override
		public void add(int index, A a)
		{
			throw throwException();
		}
		@Override
		public boolean add(A a)
		{
			throw throwException();
		}
		@Override
		public boolean addAll(int index, Collection<? extends A> c)
		{
			throw throwException();
		}
		@Override
		public boolean addAll(int index, ObjectList<? extends A> l)
		{
			throw throwException();
		}
		@Override
		public void addElements(int index, A[] a, int offset, int length)
		{
			throw throwException();
		}
		@Override
		public A set(int index, A a)
		{
			throw throwException();
		}
		@Override
		public A remove(int index)
		{
			throw throwException();
		}
		@Override
		public void sort(Comparator<? super A> comp)
		{
			throw throwException();
		}
		@Override
		public boolean remove(Object k)
		{
			throw throwException();
		}
		@Override
		public boolean removeAll(Collection<?> c)
		{
			throw throwException();
		}
		@Override
		public void removeElements(int from, int to)
		{
			throw throwException();
		}
		@Override
		public void unstableSort(Comparator<? super A> comp)
		{
			throw throwException();
		}
		@Override
		public boolean addAll(Collection<? extends A> c)
		{
			throw throwException();
		}
		@Override
		public void addElements(int index, A[] a)
		{
			throw throwException();
		}
		@Override
		public boolean addAll(ObjectList<? extends A> l)
		{
			throw throwException();
		}
		@Override
		public void setElements(A[] a)
		{
			throw throwException();
		}
		@Override
		public void setElements(int index, A[] a, int offset, int length)
		{
			throw throwException();
		}
	}
	public static class ImmutableObject2ObjectOpenHashMap<A, E> extends Object2ObjectOpenHashMap<A, E>
	{
		private final boolean lockPutAll;
		public ImmutableObject2ObjectOpenHashMap()
		{
			super();
			lockPutAll = true;
		}
		public ImmutableObject2ObjectOpenHashMap(Object2ObjectOpenHashMap<A, E> stages)
		{
			super(stages);
			lockPutAll = true;
		}
		@Override
		public void putAll(Map<? extends A, ? extends E> m)
		{
			if (lockPutAll)
				throw throwException();
			super.putAll(m);
		}
		@Override
		public E putIfAbsent(A a, E e)
		{
			throw throwException();
		}
		@Override
		public boolean remove(Object k, Object v)
		{
			throw throwException();
		}
		@Override
		public boolean replace(A a, E oldValue, E e)
		{
			throw throwException();
		}
		@Override
		public E replace(A a, E e)
		{
			throw throwException();
		}
		@Override
		public E merge(A a, E e, BiFunction<? super E, ? super E, ? extends E> remappingFunction)
		{
			throw throwException();
		}
		@Override
		public E put(A a, E e)
		{
			if (lockPutAll)
				throw throwException();
			return super.put(a, e);
		}
		@Override
		public E remove(Object k)
		{
			throw throwException();
		}
		@Override
		public E computeIfAbsent(A key, Object2ObjectFunction<? super A, ? extends E> mappingFunction)
		{
			throw throwException();
		}
		@Override
		public E computeIfPresent(A a, BiFunction<? super A, ? super E, ? extends E> remappingFunction)
		{
			throw throwException();
		}
		@Override
		public E compute(A a, BiFunction<? super A, ? super E, ? extends E> remappingFunction)
		{
			throw throwException();
		}
	}
}