package net.splatcraft.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
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
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Stage implements Comparable<Stage>
{
	public static final TreeMap<String, GameRules.Key<GameRules.BooleanRule>> VALID_SETTINGS = new TreeMap<>();
	private static final PacketCodec<ByteBuf, RegistryKey<World>> WORLD_KEY_PACKET_CODEC = RegistryKey.createPacketCodec(RegistryKeys.WORLD);
	private static final PacketCodec<ByteBuf, Object2ObjectOpenHashMap<String, Boolean>> SETTINGS_PACKET_CODEC = PacketCodecs.map(Object2ObjectOpenHashMap::new, PacketCodecs.STRING, PacketCodecs.BOOL);
	private static final PacketCodec<RegistryByteBuf, Object2ObjectOpenHashMap<String, InkColor>> TEAMS_PACKET_CODEC = PacketCodecs.map(Object2ObjectOpenHashMap::new, PacketCodecs.STRING, InkColor.PACKET_CODEC);
	private static final PacketCodec<ByteBuf, ObjectArrayList<BlockPos>> SPAWN_PAD_POSITIONS_PACKET_CODEC = BlockPos.PACKET_CODEC.collect(PacketCodecs.toCollection(ObjectArrayList::new));
	public static Codec<Stage> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		BlockPos.CODEC.fieldOf("corner_a").forGetter(v -> v.cornerA),
		BlockPos.CODEC.fieldOf("corner_b").forGetter(v -> v.cornerB),
		RegistryKey.createCodec(RegistryKeys.WORLD).fieldOf("world_key").forGetter(v -> v.worldKey),
		CodecUtils.hashMapCodec(Codec.STRING, Codec.BOOL).fieldOf("settings").forGetter(v -> v.settings),
		CodecUtils.hashMapCodec(Codec.STRING, InkColor.HEX_CODEC).fieldOf("teams").forGetter(v -> v.teams),
		CodecUtils.arrayList(BlockPos.CODEC).fieldOf("spawn_pads").forGetter(v -> v.spawnPadPositions),
		TextCodecs.CODEC.fieldOf("Name").forGetter(v -> v.name),
		Codec.STRING.fieldOf("Id").forGetter(v -> v.id)
	).apply(inst, Stage::new));
	public static PacketCodec<RegistryByteBuf, Stage> PACKET_CODEC = new PacketCodec<>()
	{
		@Override
		public Stage decode(RegistryByteBuf buf)
		{
			BlockPos cornerA = BlockPos.PACKET_CODEC.decode(buf);
			BlockPos cornerB = BlockPos.PACKET_CODEC.decode(buf);
			RegistryKey<World> worldKey = WORLD_KEY_PACKET_CODEC.decode(buf);
			Object2ObjectOpenHashMap<String, Boolean> settings = SETTINGS_PACKET_CODEC.decode(buf);
			Object2ObjectOpenHashMap<String, InkColor> teams = TEAMS_PACKET_CODEC.decode(buf);
			ObjectArrayList<BlockPos> spawnPadPositions = SPAWN_PAD_POSITIONS_PACKET_CODEC.decode(buf);
			Text name = TextCodecs.PACKET_CODEC.decode(buf);
			String id = PacketCodecs.STRING.decode(buf);
			return new Stage(cornerA, cornerB, worldKey, settings, teams, spawnPadPositions, name, id);
		}
		@Override
		public void encode(RegistryByteBuf buf, Stage value)
		{
			BlockPos.PACKET_CODEC.encode(buf, value.cornerA);
			BlockPos.PACKET_CODEC.encode(buf, value.cornerB);
			WORLD_KEY_PACKET_CODEC.encode(buf, value.worldKey);
			SETTINGS_PACKET_CODEC.encode(buf, value.settings);
			TEAMS_PACKET_CODEC.encode(buf, value.teams);
			SPAWN_PAD_POSITIONS_PACKET_CODEC.encode(buf, value.spawnPadPositions);
			TextCodecs.PACKET_CODEC.encode(buf, value.name);
			PacketCodecs.STRING.encode(buf, value.id);
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
	public BlockPos cornerA;
	public BlockPos cornerB;
	public RegistryKey<World> worldKey;
	private Text name;
	private boolean needsSpawnPadUpdate = false;
	public Stage(MinecraftServer server, RegistryKey<World> worldKey, BlockPos posA, BlockPos posB, String id, Text name)
	{
		this.worldKey = worldKey;
		this.id = id;
		this.name = name;
		settings = new Object2ObjectOpenHashMap<>();
		teams = new Object2ObjectOpenHashMap<>();
		spawnPadPositions = new ObjectArrayList<>();
		
		updateBounds(server.getWorld(worldKey), posA, posB);
	}
	public Stage(Stage stage, String id)
	{
		worldKey = stage.worldKey;
		settings = stage.settings;
		teams = stage.teams;
		spawnPadPositions = stage.spawnPadPositions;
		name = stage.name;
		cornerA = stage.cornerA;
		cornerB = stage.cornerB;
		this.id = id;
	}
	public Stage(BlockPos cornerA, BlockPos cornerB, RegistryKey<World> worldKey, Object2ObjectOpenHashMap<String, Boolean> settings, Object2ObjectOpenHashMap<String, InkColor> teams, ObjectArrayList<BlockPos> spawnPadPos, Text name, String id)
	{
		this.worldKey = worldKey;
		this.settings = settings;
		this.teams = teams;
		spawnPadPositions = spawnPadPos;
		this.name = name;
		this.cornerA = cornerA;
		this.cornerB = cornerB;
		this.id = id;
	}
	public static void registerGameruleSetting(GameRules.Key<GameRules.BooleanRule> rule)
	{
		VALID_SETTINGS.put(rule.toString().replace(Splatcraft.MODID + ".", ""), rule);
	}
	public static boolean targetsOnSameStage(World world, Vec3d targetA, Vec3d targetB)
	{
		return !getStagesForPosition(world, targetA).stream().filter(stage -> stage.getBounds().contains(targetB)).toList().isEmpty();
	}
	public static ArrayList<Stage> getAllStages()
	{
		return new ArrayList<>(SaveInfoCapability.get().stages().values());
	}
	public static Stage getStage(String id)
	{
		return SaveInfoCapability.get().stages().get(id);
	}
	public static ArrayList<Stage> getStagesForPosition(World world, Vec3d pos)
	{
		ArrayList<Stage> stages = getAllStages();
		stages.removeIf(stage -> stage == null || !stage.worldKey.equals(world.getDimension().effects()) || !stage.getBounds().contains(pos));
		return stages;
	}
	public boolean hasSetting(String key)
	{
		return settings.containsKey(key);
	}
	public boolean hasSetting(GameRules.Key<GameRules.BooleanRule> rule)
	{
		return hasSetting(rule.toString().replace("splatcraft.", ""));
	}
	@Nullable
	public Boolean getSetting(String key)
	{
		return settings.getOrDefault(key, null);
	}
	public Boolean getSetting(GameRules.Key<GameRules.BooleanRule> rule)
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
	public Box getBounds()
	{
		return Box.enclosing(cornerA, cornerB);
	}
	public Text getStageName()
	{
		return name;
	}
	public void setStageName(Text name)
	{
		this.name = name;
	}
	public BlockPos getCornerA()
	{
		return cornerA;
	}
	public BlockPos getCornerB()
	{
		return cornerB;
	}
	public void updateBounds(@Nullable World world, BlockPos cornerA, BlockPos cornerB)
	{
		this.cornerA = cornerA;
		this.cornerB = cornerB;
		if (world != null)
			updateSpawnPads(world);
	}
	public boolean needSpawnPadUpdate()
	{
		return needsSpawnPadUpdate;
	}
	public void updateSpawnPads(World world)
	{
		spawnPadPositions.clear();
		
		BlockPos blockpos2 = new BlockPos(Math.min(cornerA.getX(), cornerB.getX()), Math.min(cornerB.getY(), cornerA.getY()), Math.min(cornerA.getZ(), cornerB.getZ()));
		BlockPos blockpos3 = new BlockPos(Math.max(cornerA.getX(), cornerB.getX()), Math.max(cornerB.getY(), cornerA.getY()), Math.max(cornerA.getZ(), cornerB.getZ()));
		
		for (int x = blockpos2.getX(); x <= blockpos3.getX(); x++)
			for (int y = blockpos2.getY(); y <= blockpos3.getY(); y++)
				for (int z = blockpos2.getZ(); z <= blockpos3.getZ(); z++)
				{
					BlockPos pos = new BlockPos(x, y, z);
					if (world
						.getBlockEntity(pos) instanceof SpawnPadTileEntity spawnPad)
						addSpawnPad(spawnPad);
				}
		
		needsSpawnPadUpdate = false;
	}
	public void addSpawnPad(SpawnPadTileEntity spawnPad)
	{
		if (!spawnPadPositions.contains(spawnPad.getPos()))
			spawnPadPositions.add(spawnPad.getPos());
	}
	public void removeSpawnPad(SpawnPadTileEntity spawnPad)
	{
		spawnPadPositions.remove(spawnPad.getPos());
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
	public Object2ObjectArrayMap<InkColor, List<SpawnPadTileEntity>> getSpawnPads(World stageWorld)
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
		World stageLevel = getStageWorld(server);
		return spawnPadPositions.stream().map(pos -> stageLevel.getBlockEntity(pos)).filter(te -> te instanceof SpawnPadTileEntity).map(te -> (SpawnPadTileEntity) te).toList();
	}
	public boolean superJumpToStage(ServerPlayerEntity player)
	{
		if (!player.getWorld().getDimension().effects().equals(worldKey) || getSpawnPadPositions().isEmpty())
			return false;
		
		InkColor playerColor = ColorUtils.getEntityColor(player);
		Map<InkColor, List<SpawnPadTileEntity>> spawnPads = getSpawnPads(player.getServer());
		
		if (!spawnPads.containsKey(playerColor))
		{
			playerColor = spawnPads.keySet().toArray(new InkColor[0])[player.getRandom().nextInt(spawnPadPositions.size())];
			ColorUtils.setPlayerColor(player, playerColor);
		}
		
		BlockPos targetPos = spawnPads.get(playerColor).get(player.getRandom().nextInt(spawnPads.get(playerColor).size())).getPos();
		
		return SuperJumpCommand.superJump(player, new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + SuperJumpCommand.blockHeight(targetPos, player.getWorld()), targetPos.getZ() + 0.5));
	}
	public boolean play(MinecraftServer server, Collection<ServerPlayerEntity> players, StageGameMode gameMode)
	{
		SaveInfo saveInfo = SaveInfoCapability.get();
		if (saveInfo.playSessions().containsKey(id))
			return false;
		
		if (!gameMode.canDoOn(this, getStageWorld(server)))
			return false;
		
		PlaySession playSession = new PlaySession(players, this, gameMode);
		saveInfo.playSessions().put(id, playSession);
		SplatcraftPacketHandler.sendToAll(new SendPlaySessionCreationPacket(playSession));
		return true;
	}
	public @Nullable ServerWorld getStageWorld(MinecraftServer server)
	{
		return server.getWorld(worldKey);
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