package net.splatcraft.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.Splatcraft;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.capabilities.saveinfo.SaveInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.SendPlaySessionCreationPacket;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.tileentities.SpawnPadTileEntity;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class Stage implements Comparable<Stage>
{
	public static final TreeMap<String, GameRules.Key<GameRules.BooleanValue>> VALID_SETTINGS = new TreeMap<>();
	private static final StreamCodec<ByteBuf, ResourceKey<Level>> WORLD_KEY_STREAM_CODEC = ResourceKey.streamCodec(Registries.DIMENSION);
	private static final StreamCodec<ByteBuf, Object2ObjectOpenHashMap<String, Boolean>> SETTINGS_STREAM_CODEC = ByteBufCodecs.map(Object2ObjectOpenHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.BOOL);
	private static final StreamCodec<RegistryFriendlyByteBuf, Object2ObjectOpenHashMap<String, InkColor>> TEAMS_STREAM_CODEC = ByteBufCodecs.map(Object2ObjectOpenHashMap::new, ByteBufCodecs.STRING_UTF8, InkColor.STREAM_CODEC);
	private static final StreamCodec<ByteBuf, ObjectArrayList<BlockPos>> SPAWN_PAD_POSITIONS_STREAM_CODEC = BlockPos.STREAM_CODEC.apply(ByteBufCodecs.collection(ObjectArrayList::new));
	public static Codec<Stage> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		BlockPos.CODEC.fieldOf("min_corner").forGetter(v -> v.minCorner),
		BlockPos.CODEC.fieldOf("max_corner").forGetter(v -> v.maxCorner),
		ResourceKey.codec(Registries.DIMENSION).fieldOf("world_key").forGetter(v -> v.worldKey),
		CodecUtils.hashMapCodec(Codec.STRING, Codec.BOOL).fieldOf("settings").forGetter(v -> v.settings),
		CodecUtils.hashMapCodec(Codec.STRING, InkColor.HEX_CODEC).fieldOf("teams").forGetter(v -> v.teams),
		CodecUtils.arrayList(BlockPos.CODEC).fieldOf("spawn_pads").forGetter(v -> v.spawnPadPositions),
		ComponentSerialization.CODEC.fieldOf("Name").forGetter(v -> v.name),
		Codec.STRING.fieldOf("Id").forGetter(v -> v.id)
	).apply(inst, Stage::new));
	public static StreamCodec<RegistryFriendlyByteBuf, Stage> STREAM_CODEC = new StreamCodec<>()
	{
		@Override
		public @NotNull Stage decode(@NotNull RegistryFriendlyByteBuf buf)
		{
			BlockPos cornerA = BlockPos.STREAM_CODEC.decode(buf);
			BlockPos cornerB = BlockPos.STREAM_CODEC.decode(buf);
			ResourceKey<Level> worldKey = WORLD_KEY_STREAM_CODEC.decode(buf);
			Object2ObjectOpenHashMap<String, Boolean> settings = SETTINGS_STREAM_CODEC.decode(buf);
			Object2ObjectOpenHashMap<String, InkColor> teams = TEAMS_STREAM_CODEC.decode(buf);
			ObjectArrayList<BlockPos> spawnPadPositions = SPAWN_PAD_POSITIONS_STREAM_CODEC.decode(buf);
			Component name = ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(buf);
			String id = ByteBufCodecs.STRING_UTF8.decode(buf);
			return new Stage(cornerA, cornerB, worldKey, settings, teams, spawnPadPositions, name, id);
		}
		@Override
		public void encode(@NotNull RegistryFriendlyByteBuf buf, Stage value)
		{
			BlockPos.STREAM_CODEC.encode(buf, value.minCorner);
			BlockPos.STREAM_CODEC.encode(buf, value.maxCorner);
			WORLD_KEY_STREAM_CODEC.encode(buf, value.worldKey);
			SETTINGS_STREAM_CODEC.encode(buf, value.settings);
			TEAMS_STREAM_CODEC.encode(buf, value.teams);
			SPAWN_PAD_POSITIONS_STREAM_CODEC.encode(buf, value.spawnPadPositions);
			ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(buf, value.name);
			ByteBufCodecs.STRING_UTF8.encode(buf, value.id);
		}
	};
	static
	{
		registerGameruleSetting(SplatcraftGameRules.INK_DECAY);
		registerGameruleSetting(SplatcraftGameRules.UNIVERSAL_INK);
		registerGameruleSetting(SplatcraftGameRules.REQUIRE_INK_TANK);
		registerGameruleSetting(SplatcraftGameRules.KEEP_MATCH_ITEMS);
		registerGameruleSetting(SplatcraftGameRules.WATER_DAMAGE);
		registerGameruleSetting(SplatcraftGameRules.INK_FRIENDLY_FIRE);
		registerGameruleSetting(SplatcraftGameRules.INK_HEALING);
		registerGameruleSetting(SplatcraftGameRules.ALTERNATIVE_INK_HEALTH);
		registerGameruleSetting(SplatcraftGameRules.INK_HEALING_CONSUMES_HUNGER);
		registerGameruleSetting(SplatcraftGameRules.INKABLE_GROUND);
		registerGameruleSetting(SplatcraftGameRules.INK_DESTROYS_FOLIAGE);
		registerGameruleSetting(SplatcraftGameRules.RECHARGEABLE_INK_TANK);
		registerGameruleSetting(SplatcraftGameRules.GLOBAL_SUPERJUMPING);
		registerGameruleSetting(SplatcraftGameRules.BLOCK_DESTROY_INK);
	}
	public final String id;
	private final Object2ObjectOpenHashMap<String, Boolean> settings;
	private final Object2ObjectOpenHashMap<String, InkColor> teams;
	private final ObjectArrayList<BlockPos> spawnPadPositions;
	public BlockPos minCorner;
	public BlockPos maxCorner;
	public ResourceKey<Level> worldKey;
	private Component name;
	private boolean needsSpawnPadUpdate = false;
	public Stage(MinecraftServer server, ResourceKey<Level> worldKey, BlockPos posA, BlockPos posB, String id, Component name)
	{
		this.worldKey = worldKey;
		this.id = id;
		this.name = name;
		settings = new Object2ObjectOpenHashMap<>();
		teams = new Object2ObjectOpenHashMap<>();
		spawnPadPositions = new ObjectArrayList<>();
		
		updateBounds(server.getLevel(worldKey), posA, posB);
	}
	public Stage(BlockPos minCorner, BlockPos maxCorner, ResourceKey<Level> worldKey, Object2ObjectOpenHashMap<String, Boolean> settings, Object2ObjectOpenHashMap<String, InkColor> teams, ObjectArrayList<BlockPos> spawnPadPos, Component name, String id)
	{
		this.worldKey = worldKey;
		this.settings = settings;
		this.teams = teams;
		spawnPadPositions = spawnPadPos;
		this.name = name;
		this.minCorner = minCorner;
		this.maxCorner = maxCorner;
		this.id = id;
	}
	public static void registerGameruleSetting(GameRules.Key<GameRules.BooleanValue> rule)
	{
		VALID_SETTINGS.put(rule.toString().replace(Splatcraft.MODID + ".", ""), rule);
	}
	public static boolean targetsOnSameStage(Level world, Vec3 targetA, Vec3 targetB)
	{
		return !getStagesForPosition(world, targetA).stream()
			.filter(stage -> stage.getBounds().contains(targetB))
			.toList().isEmpty();
	}
	public static ArrayList<Stage> getAllStages()
	{
		return new ArrayList<>(SaveInfoCapability.get().stages().values());
	}
	public static Stage getStage(String id)
	{
		return SaveInfoCapability.get().stages().get(id);
	}
	public static ArrayList<Stage> getStagesForPosition(Level level, Vec3 pos)
	{
		ArrayList<Stage> stages = getAllStages();
		stages.removeIf(stage -> stage == null || !stage.worldKey.equals(level.dimension()) || !stage.getBounds().contains(pos));
		return stages;
	}
	public static ArrayList<Stage> getStagesForPosition(Level level, BlockPos pos)
	{
		ArrayList<Stage> stages = getAllStages();
		stages.removeIf(stage -> stage == null || !stage.worldKey.equals(level.dimension()) || !stage.getBounds().contains(pos.getX(), pos.getY(), pos.getZ()));
		return stages;
	}
	public Optional<PlaySession> tryGetPlaySession()
	{
		return Optional.ofNullable(SaveInfoCapability.get().playSessions().get(id));
	}
	public boolean hasSetting(String key)
	{
		return settings.containsKey(key);
	}
	public boolean hasSetting(GameRules.Key<GameRules.BooleanValue> rule)
	{
		return hasSetting(rule.toString().replace("splatcraft.", ""));
	}
	@Nullable
	public Boolean getSetting(String key)
	{
		return settings.getOrDefault(key, null);
	}
	public Boolean getSetting(GameRules.Key<GameRules.BooleanValue> rule)
	{
		return getSetting(rule.toString().replace("splatcraft.", ""));
	}
	public void applySetting(String key, @Nullable Boolean value)
	{
		if (value == null)
			settings.remove(key);
		else settings.put(key, value);
	}
	public boolean hasTeam(String teamId)
	{
		return teams.containsKey(teamId);
	}
	public InkColor getTeamColor(String teamId)
	{
		return hasTeam(teamId) ? teams.get(teamId) : InkColor.INVALID;
	}
	public void setTeamColor(String teamId, InkColor teamColor)
	{
		teams.put(teamId, teamColor);
	}
	public void removeTeam(String teamId)
	{
		teams.remove(teamId);
	}
	public Collection<String> getTeamIds()
	{
		return teams.keySet();
	}
	public AABB getBounds()
	{
		return AABB.encapsulatingFullBlocks(minCorner, maxCorner);
	}
	public Component getStageName()
	{
		return name;
	}
	public void setStageName(Component name)
	{
		this.name = name;
	}
	public BlockPos getMinCorner()
	{
		return minCorner;
	}
	public BlockPos getMaxCorner()
	{
		return maxCorner;
	}
	public void updateBounds(@Nullable Level world, BlockPos cornerA, BlockPos cornerB)
	{
		minCorner = BlockPos.min(cornerA, cornerB);
		maxCorner = BlockPos.max(cornerA, cornerB);
		
		if (world != null)
			updateSpawnPads(world);
	}
	public boolean needSpawnPadUpdate()
	{
		return needsSpawnPadUpdate;
	}
	public void updateSpawnPads(Level world)
	{
		spawnPadPositions.clear();
		
		for (BlockPos pos : BlockPos.betweenClosed(minCorner, maxCorner))
		{
			if (world.getBlockEntity(pos) instanceof SpawnPadTileEntity spawnPad)
				addSpawnPad(spawnPad);
		}
		
		needsSpawnPadUpdate = false;
	}
	public void addSpawnPad(SpawnPadTileEntity spawnPad)
	{
		if (!spawnPadPositions.contains(spawnPad.getBlockPos()))
			spawnPadPositions.add(spawnPad.getBlockPos());
	}
	public void removeSpawnPad(SpawnPadTileEntity spawnPad)
	{
		spawnPadPositions.remove(spawnPad.getBlockPos());
	}
	public boolean hasSpawnPads()
	{
		return !spawnPadPositions.isEmpty();
	}
	public List<BlockPos> getSpawnPadPositions()
	{
		return spawnPadPositions;
	}
	public Map<InkColor, List<SpawnPadTileEntity>> getSpawnPads(MinecraftServer server)
	{
		return getSpawnPads(getStageWorld(server));
	}
	public Object2ObjectArrayMap<InkColor, List<SpawnPadTileEntity>> getSpawnPads(Level stageWorld)
	{
		Object2ObjectArrayMap<InkColor, List<SpawnPadTileEntity>> result = new Object2ObjectArrayMap<>();
		for (BlockPos pos : spawnPadPositions)
			if (stageWorld.getBlockEntity(pos) instanceof SpawnPadTileEntity pad)
			{
				result.computeIfAbsent(pad.getInkColor(), v -> new ObjectArrayList<>()).add(pad);
			}
		return result;
	}
	public List<SpawnPadTileEntity> getAllSpawnPads(MinecraftServer server)
	{
		Level stageLevel = getStageWorld(server);
		return spawnPadPositions.stream().map(pos -> stageLevel.getBlockEntity(pos)).filter(te -> te instanceof SpawnPadTileEntity).map(te -> (SpawnPadTileEntity) te).toList();
	}
	public boolean superJumpToStage(ServerPlayer player)
	{
		if (!player.level().dimension().equals(worldKey) || getSpawnPadPositions().isEmpty())
			return false;
		
		InkColor playerColor = ColorUtils.getEntityColor(player);
		Map<InkColor, List<SpawnPadTileEntity>> spawnPads = getSpawnPads(player.getServer());
		
		if (!spawnPads.containsKey(playerColor))
		{
			playerColor = spawnPads.keySet().toArray(new InkColor[0])[player.getRandom().nextInt(spawnPadPositions.size())];
			ColorUtils.setPlayerColor(player, playerColor);
		}
		
		BlockPos targetPos = spawnPads.get(playerColor).get(player.getRandom().nextInt(spawnPads.get(playerColor).size())).getBlockPos();
		
		return SuperJumpCommand.superJump(player, new Vec3(targetPos.getX() + 0.5, targetPos.getY() + SuperJumpCommand.blockHeight(targetPos, player.level()), targetPos.getZ() + 0.5));
	}
	public boolean play(MinecraftServer server, Collection<ServerPlayer> players, StageGameMode gameMode)
	{
		SaveInfo saveInfo = SaveInfoCapability.get();
		if (saveInfo.playSessions().containsKey(id))
			return false;
		
		if (!gameMode.canDoOn(this, getStageWorld(server)))
			return false;
		
		PlaySession playSession = new PlaySession(players, this, gameMode);
		playSession.gameMode.onStart.consume(playSession, getStageWorld(server));
		saveInfo.playSessions().put(id, playSession);
		SplatcraftPacketHandler.sendToAll(new SendPlaySessionCreationPacket(playSession));
		return true;
	}
	public void iterateBlockPos(Consumer<BlockPos> consumer)
	{
		for (BlockPos pos : BlockPos.betweenClosed(minCorner.getX(), minCorner.getY(), minCorner.getZ(), maxCorner.getX(), maxCorner.getY(), maxCorner.getZ()))
		{
			consumer.accept(pos);
		}
	}
	public void iterateChunkPos(Consumer<ChunkPos> consumer)
	{
		for (ChunkPos pos : ChunkPos.rangeClosed(new ChunkPos(minCorner), new ChunkPos(maxCorner)).toList())
		{
			consumer.accept(pos);
		}
	}
	public @Nullable ServerLevel getStageWorld(MinecraftServer server)
	{
		return server.getLevel(worldKey);
	}
	@Override
	public int compareTo(Stage o)
	{
		return id.compareTo(o.id);
	}
	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof Stage oStage && id.equals(oStage.id);
	}
}