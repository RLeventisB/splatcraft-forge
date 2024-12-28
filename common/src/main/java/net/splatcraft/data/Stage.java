package net.splatcraft.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.splatcraft.Splatcraft;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.capabilities.saveinfo.SaveInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.tileentities.SpawnPadTileEntity;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Stage implements Comparable<Stage>
{
	public static final TreeMap<String, GameRules.Key<GameRules.BooleanRule>> VALID_SETTINGS = new TreeMap<>();
	public static Codec<Stage> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		BlockPos.CODEC.fieldOf("CornerA").forGetter(v -> v.cornerA),
		BlockPos.CODEC.fieldOf("CornerB").forGetter(v -> v.cornerB),
		Identifier.CODEC.fieldOf("Dimension").forGetter(v -> v.dimID),
		Codec.unboundedMap(Codec.STRING, Codec.BOOL).fieldOf("Settings").forGetter(v -> v.settings),
		Codec.unboundedMap(Codec.STRING, InkColor.CODEC).fieldOf("Teams").forGetter(v -> v.teams),
		Codec.list(BlockPos.CODEC).fieldOf("SpawnPads").forGetter(v -> v.spawnPadPositions),
		TextCodecs.CODEC.fieldOf("Name").forGetter(v -> v.name),
		Codec.STRING.fieldOf("Id").forGetter(v -> v.id)
	).apply(inst, Stage::new));
	public static PacketCodec<RegistryByteBuf, Stage> PACKET_CODEC = new PacketCodec<>()
	{
		@Override
		public Stage decode(RegistryByteBuf buf)
		{
			return null;
		}
		@Override
		public void encode(RegistryByteBuf buf, Stage value)
		{
			BlockPos.PACKET_CODEC.encode(buf, value.cornerA);
			BlockPos.PACKET_CODEC.encode(buf, value.cornerB);
			Identifier.PACKET_CODEC.encode(buf, value.dimID);
			PacketCodecs.map(HashMap::new, PacketCodecs.STRING, PacketCodecs.BOOL).encode(buf, (HashMap<String, Boolean>) value.settings);
			PacketCodecs.map(HashMap::new, PacketCodecs.STRING, InkColor.PACKET_CODEC).encode(buf, (HashMap<String, InkColor>) value.teams);
			BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()).encode(buf, value.spawnPadPositions);
			TextCodecs.PACKET_CODEC.encode(buf, value.name);
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
	private final Map<String, Boolean> settings;
	private final Map<String, InkColor> teams;
	private final List<BlockPos> spawnPadPositions;
	public BlockPos cornerA;
	public BlockPos cornerB;
	public Identifier dimID;
	private Text name;
	private boolean needsSpawnPadUpdate = false;
	public Stage(World world, BlockPos posA, BlockPos posB, String id, Text name)
	{
		dimID = world.getDimension().effects();
		this.id = id;
		this.name = name;
		settings = new HashMap<>();
		teams = new HashMap<>();
		spawnPadPositions = new ArrayList<>();
		
		updateBounds(world, posA, posB);
	}
	public Stage(Stage stage, String id)
	{
		dimID = stage.dimID;
		settings = stage.settings;
		teams = stage.teams;
		spawnPadPositions = stage.spawnPadPositions;
		name = stage.name;
		cornerA = stage.cornerA;
		cornerB = stage.cornerB;
		this.id = id;
	}
	public Stage(BlockPos cornerA, BlockPos cornerB, Identifier dimID, Map<String, Boolean> settings, Map<String, InkColor> teams, List<BlockPos> spawnPadPos, Text name, String id)
	{
		this.dimID = dimID;
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
	public static ArrayList<Stage> getAllStages(World world)
	{
		return new ArrayList<>(world.isClient() ? ClientUtils.clientStages.values() : SaveInfoCapability.get().getStages().values());
	}
	public static Stage getStage(World world, String id)
	{
		return (world.isClient() ? ClientUtils.clientStages : SaveInfoCapability.get().getStages()).get(id);
	}
	public static ArrayList<Stage> getStagesForPosition(World world, Vec3d pos)
	{
		ArrayList<Stage> stages = getAllStages(world);
		stages.removeIf(stage -> !stage.dimID.equals(world.getDimension().effects()) || !stage.getBounds().contains(pos));
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
	public NbtCompound writeData()
	{
		NbtCompound nbt = new NbtCompound();
		
		nbt.put("CornerA", NbtHelper.fromBlockPos(cornerA));
		nbt.put("CornerB", NbtHelper.fromBlockPos(cornerB));
		nbt.putString("Dimension", dimID.toString());
		
		NbtCompound settingsNbt = new NbtCompound();
		NbtCompound teamsNbt = new NbtCompound();
		
		for (Map.Entry<String, Boolean> setting : settings.entrySet())
			settingsNbt.putBoolean(setting.getKey(), setting.getValue());
		nbt.put("Settings", settingsNbt);
		
		for (Map.Entry<String, InkColor> team : teams.entrySet())
			teamsNbt.put(team.getKey(), team.getValue().getNbt());
		nbt.put("Teams", teamsNbt);
		
		if (!needsSpawnPadUpdate)
		{
			NbtList list = new NbtList();
			for (BlockPos spawnPadPos : spawnPadPositions)
				list.add(NbtHelper.fromBlockPos(spawnPadPos));
			
			nbt.put("SpawnPads", list);
		}
		
		nbt.putString("Name", Text.Serialization.toJsonString(name, MinecraftClient.getInstance().getServer().getRegistryManager()));
		
		return nbt;
	}
	public boolean needSpawnPadUpdate()
	{
		return needsSpawnPadUpdate;
	}
	public void updateSpawnPads(World world)
	{
		spawnPadPositions.clear();
		World stageLevel = getWorld(world);
		
		BlockPos blockpos2 = new BlockPos(Math.min(cornerA.getX(), cornerB.getX()), Math.min(cornerB.getY(), cornerA.getY()), Math.min(cornerA.getZ(), cornerB.getZ()));
		BlockPos blockpos3 = new BlockPos(Math.max(cornerA.getX(), cornerB.getX()), Math.max(cornerB.getY(), cornerA.getY()), Math.max(cornerA.getZ(), cornerB.getZ()));
		
		for (int x = blockpos2.getX(); x <= blockpos3.getX(); x++)
			for (int y = blockpos2.getY(); y <= blockpos3.getY(); y++)
				for (int z = blockpos2.getZ(); z <= blockpos3.getZ(); z++)
				{
					BlockPos pos = new BlockPos(x, y, z);
					if (stageLevel.getBlockEntity(pos) instanceof SpawnPadTileEntity spawnPad)
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
	public HashMap<InkColor, ArrayList<SpawnPadTileEntity>> getSpawnPads(World world)
	{
		HashMap<InkColor, ArrayList<SpawnPadTileEntity>> result = new HashMap<>();
		World stageWorld = getWorld(world);
		
		for (BlockPos pos : spawnPadPositions)
			if (stageWorld.getBlockEntity(pos) instanceof SpawnPadTileEntity pad)
			{
				if (!result.containsKey(pad.getInkColor()))
					result.put(pad.getInkColor(), new ArrayList<>());
				result.get(pad.getInkColor()).add(pad);
			}
		
		return result;
	}
	private @Nullable ServerWorld getWorld(World world)
	{
		return world.getServer().getWorld(RegistryKeys.toWorldKey(RegistryKey.of(RegistryKeys.DIMENSION, dimID)));
	}
	public List<SpawnPadTileEntity> getAllSpawnPads(World world)
	{
		World stageLevel = getWorld(world);
		return spawnPadPositions.stream().map(pos -> stageLevel.getBlockEntity(pos)).filter(te -> te instanceof SpawnPadTileEntity).map(te -> (SpawnPadTileEntity) te).toList();
	}
	public boolean superJumpToStage(ServerPlayerEntity player)
	{
		if (!player.getWorld().getDimension().effects().equals(dimID) || getSpawnPadPositions().isEmpty())
			return false;
		
		InkColor playerColor = ColorUtils.getEntityColor(player);
		HashMap<InkColor, ArrayList<SpawnPadTileEntity>> spawnPads = getSpawnPads(player.getWorld());
		
		if (!spawnPads.containsKey(playerColor))
		{
			playerColor = spawnPads.keySet().toArray(new InkColor[0])[player.getRandom().nextInt(spawnPadPositions.size())];
			ColorUtils.setPlayerColor(player, playerColor);
		}
		
		BlockPos targetPos = spawnPads.get(playerColor).get(player.getRandom().nextInt(spawnPads.get(playerColor).size())).getPos();
		
		return SuperJumpCommand.superJump(player, new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + SuperJumpCommand.blockHeight(targetPos, player.getWorld()), targetPos.getZ() + 0.5));
	}
	@Environment(EnvType.SERVER)
	public boolean play(World world, Collection<ServerPlayerEntity> players, StageGameMode gameMode)
	{
		SaveInfo saveInfo = SaveInfoCapability.get();
		if (saveInfo.playSessions().containsKey(id))
			return false;
		
		if (!gameMode.canDoOn(this))
			return false;
		
		PlaySession playSession = new PlaySession(world, players, this, gameMode);
		saveInfo.playSessions().put(id, playSession);
		
		return true;
	}
	public ServerWorld getStageLevel(MinecraftServer server)
	{
		return server.getWorld(RegistryKey.of(RegistryKeys.WORLD, dimID));
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
	public Stage withId(String key)
	{
		return new Stage(this, key);
	}
}