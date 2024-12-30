package net.splatcraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.splatcraft.blocks.SpawnPadBlock;
import net.splatcraft.commands.arguments.InkColorArgument;
import net.splatcraft.commands.arguments.StageGameModeArgument;
import net.splatcraft.data.Stage;
import net.splatcraft.data.StageGameMode;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateStageListPacket;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.tileentities.SpawnPadTileEntity;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class StageCommand
{
	public static final DynamicCommandExceptionType TEAM_NOT_FOUND = new DynamicCommandExceptionType(p_208663_0_ -> Text.translatable("arg.stageTeam.notFound", ((Object[]) p_208663_0_)[0], ((Object[]) p_208663_0_)[1]));
	public static final DynamicCommandExceptionType STAGE_NOT_FOUND = new DynamicCommandExceptionType(p_208663_0_ -> Text.translatable("arg.stage.notFound", p_208663_0_));
	public static final DynamicCommandExceptionType NOT_ENOUGH_TEAMS = new DynamicCommandExceptionType(p_208663_0_ -> Text.translatable("arg.stage.notEnoughTeams", p_208663_0_));
	public static final DynamicCommandExceptionType ALREADY_PLAYING = new DynamicCommandExceptionType(p_208663_0_ -> Text.translatable("arg.stage.alreadyPlaying", p_208663_0_));
	public static final DynamicCommandExceptionType STAGE_ALREADY_EXISTS = new DynamicCommandExceptionType(p_208663_0_ -> Text.translatable("arg.stage.alreadyExists", p_208663_0_));
	public static final DynamicCommandExceptionType SETTING_NOT_FOUND = new DynamicCommandExceptionType(p_208663_0_ -> Text.translatable("arg.stageSetting.notFound", p_208663_0_));
	private static final DynamicCommandExceptionType NO_SPAWN_PADS_FOUND = new DynamicCommandExceptionType(p_208663_0_ -> Text.translatable("arg.stageWarp.noSpawnPads", p_208663_0_));
	private static final DynamicCommandExceptionType NO_PLAYERS_FOUND = new DynamicCommandExceptionType(p_208663_0_ -> Text.translatable("arg.stageWarp.noPlayers", p_208663_0_));
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
	{
		dispatcher.register(CommandManager.literal("stage").requires(commandSource -> commandSource.hasPermissionLevel(2))
			.then(CommandManager.literal("add")
				.then(CommandManager.argument("name", StringArgumentType.word())
					.then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
						.then(CommandManager.argument("to", BlockPosArgumentType.blockPos()).executes(StageCommand::add)))))
			.then(CommandManager.literal("remove")
				.then(stageId("stage")
					.executes(StageCommand::remove)))
			.then(CommandManager.literal("list")
				.executes(StageCommand::list))
			.then(CommandManager.literal("settings").then(stageId("stage")
				.then(CommandManager.literal("cornerA").executes(context -> getStageCorner(context, true))
					.then(CommandManager.argument("pos", BlockPosArgumentType.blockPos()).executes(context -> setStageCorner(context, true))))
				.then(CommandManager.literal("cornerB").executes(context -> getStageCorner(context, false))
					.then(CommandManager.argument("pos", BlockPosArgumentType.blockPos()).executes(context -> setStageCorner(context, false))))
				.then(stageSetting("setting").executes(StageCommand::getSetting)
					.then(CommandManager.literal("true").executes(context -> setSetting(context, true)))
					.then(CommandManager.literal("false").executes(context -> setSetting(context, false)))
					.then(CommandManager.literal("default").executes(context -> setSetting(context, null))))))
			.then(CommandManager.literal("teams").then(stageId("stage")
				.then(CommandManager.literal("set")
					.then(stageTeam("teamName", "stage")
						.then(CommandManager.argument("teamColor", InkColorArgument.inkColor()).executes(StageCommand::setTeam))))
				.then(CommandManager.literal("remove")
					.then(stageTeam("teamName", "stage").executes(StageCommand::removeTeam)))
				.then(CommandManager.literal("get")
					.then(stageTeam("teamName", "stage").executes(StageCommand::getTeam)))))
			.then(CommandManager.literal("warp").then(stageId("stage").executes(StageCommand::warpSelf)
				.then(CommandManager.argument("players", EntityArgumentType.players()).executes(context -> warp(context, false))
					.then(CommandManager.argument("setSpawn", BoolArgumentType.bool())
						.then(CommandManager.literal("self").executes(context -> warp(context, BoolArgumentType.getBool(context, "setSpawn"))))
						.then(CommandManager.literal("any").executes(context -> warpAny(context, BoolArgumentType.getBool(context, "setSpawn"))))
						.then(CommandManager.literal("color").then(CommandManager.argument("color", InkColorArgument.inkColor()).executes(context -> warp(context, BoolArgumentType.getBool(context, "setSpawn"), InkColorArgument.getInkColor(context, "color")))))
						.then(CommandManager.literal("team").then(stageTeam("team", "stage").executes(context -> warpToTeam(context, BoolArgumentType.getBool(context, "setSpawn"), StringArgumentType.getString(context, "team")))))
						.executes(context -> warp(context, BoolArgumentType.getBool(context, "setSpawn")))))))
			.then(CommandManager.literal("play")
				.then(stageId("stage")
					.executes(StageCommand::playRecon)
					.then(CommandManager.argument("players", EntityArgumentType.players())
						.executes(StageCommand::playWithPlayers)
						.then(CommandManager.argument("assignTeams", BoolArgumentType.bool())
							.executes(StageCommand::playWithSetAssignedTeams))
						.then(CommandManager.argument("gamemode", StageGameModeArgument.stageGamemode())
							.executes(StageCommand::play))
					)))
		);
	}
	public static RequiredArgumentBuilder<ServerCommandSource, String> stageId(String argumentName)
	{
		return CommandManager.argument(argumentName, StringArgumentType.word()).suggests((context, builder) -> CommandSource.suggestMatching((context.getSource().getWorld().isClient() ? ClientUtils.clientStages : SaveInfoCapability.get().getStages()).keySet(), builder));
	}
	public static RequiredArgumentBuilder<ServerCommandSource, String> stageTeam(String argumentName, String stageArgumentName)
	{
		return CommandManager.argument(argumentName, StringArgumentType.word()).suggests((context, builder) ->
		{
			try
			{
				Stage stage = (context.getSource().getWorld().isClient() ? ClientUtils.clientStages : SaveInfoCapability.get().getStages()).get(StringArgumentType.getString(context, stageArgumentName));
				if (stage == null)
					return Suggestions.empty();
				return CommandSource.suggestMatching(stage.getTeamIds(), builder);
			}
			catch (IllegalArgumentException ignored)
			{
			} //happens when used inside execute, vanilla won't bother to fix it so neither will i >_>
			
			return Suggestions.empty();
		});
	}
	public static RequiredArgumentBuilder<ServerCommandSource, String> stageSetting(String argumentName)
	{
		return CommandManager.argument(argumentName, StringArgumentType.word()).suggests((context, builder) ->
			CommandSource.suggestMatching(Stage.VALID_SETTINGS.keySet(), builder));
	}
	private static int add(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
	{
		return add(context.getSource(), StringArgumentType.getString(context, "name"), getOrLoadBlockPos(context, "from"), getOrLoadBlockPos(context, "to"));
	}
	private static int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
	{
		return remove(context.getSource(), StringArgumentType.getString(context, "stage"));
	}
	private static int list(CommandContext<ServerCommandSource> context)
	{
		return listStages(context.getSource());
	}
	private static int setSetting(CommandContext<ServerCommandSource> context, @Nullable Boolean value) throws CommandSyntaxException
	{
		return setSetting(context.getSource(), StringArgumentType.getString(context, "stage"), StringArgumentType.getString(context, "setting"), value);
	}
	private static int setStageCorner(CommandContext<ServerCommandSource> context, boolean isCornerA) throws CommandSyntaxException
	{
		return setStageCoords(context.getSource(), StringArgumentType.getString(context, "stage"), BlockPosArgumentType.getLoadedBlockPos(context, "pos"), isCornerA);
	}
	private static int getStageCorner(CommandContext<ServerCommandSource> context, boolean isCornerA) throws CommandSyntaxException
	{
		return getStageCoords(context.getSource(), StringArgumentType.getString(context, "stage"), isCornerA);
	}
	private static int getSetting(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
	{
		return getSetting(context.getSource(), StringArgumentType.getString(context, "stage"), StringArgumentType.getString(context, "setting"));
	}
	private static int setTeam(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
	{
		return setTeam(context.getSource(), StringArgumentType.getString(context, "stage"), StringArgumentType.getString(context, "teamName"), InkColorArgument.getInkColor(context, "teamColor"));
	}
	private static int getTeam(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
	{
		return getTeam(context.getSource(), StringArgumentType.getString(context, "stage"), StringArgumentType.getString(context, "teamName")).getColor();
	}
	private static int removeTeam(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
	{
		return removeTeam(context.getSource(), StringArgumentType.getString(context, "stage"), StringArgumentType.getString(context, "teamName")).getColor();
	}
	private static int warp(CommandContext<ServerCommandSource> context, boolean setSpawn) throws CommandSyntaxException
	{
		return warpPlayers(context.getSource(), StringArgumentType.getString(context, "stage"), EntityArgumentType.getPlayers(context, "players"), setSpawn);
	}
	private static int warp(CommandContext<ServerCommandSource> context, boolean setSpawn, InkColor color) throws CommandSyntaxException
	{
		return warpPlayers(context.getSource(), StringArgumentType.getString(context, "stage"), EntityArgumentType.getPlayers(context, "players"), setSpawn, color);
	}
	private static int warpAny(CommandContext<ServerCommandSource> context, boolean setSpawn) throws CommandSyntaxException
	{
		return warpPlayersToAny(context.getSource(), StringArgumentType.getString(context, "stage"), EntityArgumentType.getPlayers(context, "players"), setSpawn);
	}
	private static int warpToTeam(CommandContext<ServerCommandSource> context, boolean setSpawn, String team) throws CommandSyntaxException
	{
		String stageId = StringArgumentType.getString(context, "stage");
		Map<String, Stage> stages = SaveInfoCapability.get().getStages();
		if (!stages.containsKey(stageId))
			throw STAGE_NOT_FOUND.create(stageId);
		
		Stage stage = stages.get(stageId);
		
		if (!stage.hasTeam(team))
			throw TEAM_NOT_FOUND.create(new Object[] {team, stageId});
		
		return warpPlayers(context.getSource(), stageId, EntityArgumentType.getPlayers(context, "players"), setSpawn, stage.getTeamColor(team));
	}
	private static int warpSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
	{
		return warpPlayers(context.getSource(), StringArgumentType.getString(context, "stage"), Collections.singleton(context.getSource().getPlayerOrThrow()), false);
	}
	private static int playRecon(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
	{
		return playStage(context.getSource(), StringArgumentType.getString(context, "stage"), Collections.singletonList(context.getSource().getPlayerOrThrow()), true, StageGameMode.RECON);
	}
	private static int playWithPlayers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
	{
		return playStage(context.getSource(), StringArgumentType.getString(context, "stage"), EntityArgumentType.getPlayers(context, "players"), true, StageGameMode.TURF_WAR);
	}
	private static int playWithSetAssignedTeams(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
	{
		return playStage(context.getSource(), StringArgumentType.getString(context, "stage"), EntityArgumentType.getPlayers(context, "players"), BoolArgumentType.getBool(context, "assignTeams"), StageGameMode.TURF_WAR);
	}
	private static int play(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
	{
		return playStage(context.getSource(), StringArgumentType.getString(context, "stage"), EntityArgumentType.getPlayers(context, "players"), BoolArgumentType.getBool(context, "assignTeams"), StageGameModeArgument.getStageGameMode(context, "gamemode"));
	}
	private static int add(ServerCommandSource source, String stageId, BlockPos from, BlockPos to) throws CommandSyntaxException
	{
		if (!SaveInfoCapability.get().createStage(source.getWorld(), stageId, from, to))
			throw STAGE_ALREADY_EXISTS.create(stageId);
		
		source.sendFeedback(() -> Text.translatable("commands.stage.add.success", stageId), true);
		
		return 1;
	}
	private static int remove(ServerCommandSource source, String stageId) throws CommandSyntaxException
	{
		Map<String, Stage> stages = SaveInfoCapability.get().getStages();
		if (!stages.containsKey(stageId))
			throw STAGE_NOT_FOUND.create(stageId);
		
		stages.remove(stageId);
		
		source.sendFeedback(() -> Text.translatable("commands.stage.remove.success", stageId), true);
		
		SplatcraftPacketHandler.sendToAll(new UpdateStageListPacket(stages));
		
		return 1;
	}
	private static int listStages(ServerCommandSource source)
	{
		Map<String, Stage> stages = SaveInfoCapability.get().getStages();
		StringBuilder builder = new StringBuilder();
		for (String key : stages.keySet())
		{
			builder.append('[');
			builder.append(key);
			builder.append(']');
		}
		source.sendFeedback(() -> Text.translatable("commands.stage.list.get", builder.toString()), true);
		return stages.size();
	}
	private static int setSetting(ServerCommandSource source, String stageId, String setting, @Nullable Boolean value) throws CommandSyntaxException
	{
		Map<String, Stage> stages = SaveInfoCapability.get().getStages();
		
		if (!stages.containsKey(stageId))
			throw STAGE_NOT_FOUND.create(stageId);
		
		if (!Stage.VALID_SETTINGS.containsKey(setting))
			throw SETTING_NOT_FOUND.create(setting);
		
		Stage stage = stages.get(stageId);
		
		stage.applySetting(setting, value);
		
		if (value == null)
			source.sendFeedback(() -> Text.translatable("commands.stage.setting.success.default", setting, stageId), true);
		else
			source.sendFeedback(() -> Text.translatable("commands.stage.setting.success", setting, stageId, value), true);
		
		SplatcraftPacketHandler.sendToAll(new UpdateStageListPacket(stages));
		
		return 1;
	}
	private static int getSetting(ServerCommandSource source, String stageId, String setting) throws CommandSyntaxException
	{
		Map<String, Stage> stages = SaveInfoCapability.get().getStages();
		
		if (!stages.containsKey(stageId))
			throw STAGE_NOT_FOUND.create(stageId);
		
		if (!Stage.VALID_SETTINGS.containsKey(setting))
			throw SETTING_NOT_FOUND.create(setting);
		
		Stage stage = stages.get(stageId);
		
		if (!stage.hasSetting(setting))
			source.sendFeedback(() -> Text.translatable("commands.stage.setting.get.default", setting, stageId), true);
		else
			source.sendFeedback(() -> Text.translatable("commands.stage.setting.get", setting, stageId, stage.getSetting(setting)), true);
		
		return 1;
	}
	private static int setTeam(ServerCommandSource source, String stageId, String teamId, InkColor teamColor) throws CommandSyntaxException
	{
		Map<String, Stage> stages = SaveInfoCapability.get().getStages();
		
		if (!stages.containsKey(stageId))
			throw STAGE_NOT_FOUND.create(stageId);
		
		Stage stage = stages.get(stageId);
		World stageLevel = stage.getStageLevel(source.getServer());
		
		BlockPos blockpos2 = new BlockPos(Math.min(stage.cornerA.getX(), stage.cornerB.getX()), Math.min(stage.cornerB.getY(), stage.cornerA.getY()), Math.min(stage.cornerA.getZ(), stage.cornerB.getZ()));
		BlockPos blockpos3 = new BlockPos(Math.max(stage.cornerA.getX(), stage.cornerB.getX()), Math.max(stage.cornerB.getY(), stage.cornerA.getY()), Math.max(stage.cornerA.getZ(), stage.cornerB.getZ()));
		
		int affectedBlocks = 0;
		
		for (int x = blockpos2.getX(); x <= blockpos3.getX(); x++)
			for (int y = blockpos2.getY(); y <= blockpos3.getY(); y++)
				for (int z = blockpos2.getZ(); z <= blockpos3.getZ(); z++)
				{
					BlockPos pos = new BlockPos(x, y, z);
					
					if (stageLevel.getBlockEntity(pos) instanceof InkColorTileEntity colorTile)
					{
						if (colorTile.getInkColor() == teamColor && !colorTile.getTeam().equals(teamId))
						{
							colorTile.setTeam(teamId);
							affectedBlocks++;
						}
					}
				}
		
		stage.setTeamColor(teamId, teamColor);
		int finalAffectedBlocks = affectedBlocks;
		source.sendFeedback(() -> Text.translatable("commands.stage.teams.set.success", finalAffectedBlocks, stageId, Text.literal(teamId).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(teamColor.getColor())))), true);
		
		SplatcraftPacketHandler.sendToAll(new UpdateStageListPacket(stages));
		
		return 1;
	}
	private static InkColor getTeam(ServerCommandSource source, String stageId, String teamId) throws CommandSyntaxException
	{
		Map<String, Stage> stages = SaveInfoCapability.get().getStages();
		
		if (!stages.containsKey(stageId))
			throw STAGE_NOT_FOUND.create(stageId);
		
		Stage stage = stages.get(stageId);
		
		if (!stage.hasTeam(teamId))
			throw TEAM_NOT_FOUND.create(new Object[] {teamId, stageId});
		
		InkColor teamColor = stage.getTeamColor(teamId);
		
		source.sendFeedback(() -> Text.translatable("commands.stage.teams.get.success", teamId, stageId, ColorUtils.getFormatedColorName(teamColor, false)), true);
		return teamColor;
	}
	private static InkColor removeTeam(ServerCommandSource source, String stageId, String teamId) throws CommandSyntaxException
	{
		Map<String, Stage> stages = SaveInfoCapability.get().getStages();
		
		if (!stages.containsKey(stageId))
			throw STAGE_NOT_FOUND.create(stageId);
		
		Stage stage = stages.get(stageId);
		
		if (!stage.hasTeam(teamId))
			throw TEAM_NOT_FOUND.create(new Object[] {teamId, stageId});
		
		InkColor teamColor = stage.getTeamColor(teamId);
		
		World stageLevel = stage.getStageLevel(source.getServer());
		BlockPos blockpos2 = new BlockPos(Math.min(stage.cornerA.getX(), stage.cornerB.getX()), Math.min(stage.cornerB.getY(), stage.cornerA.getY()), Math.min(stage.cornerA.getZ(), stage.cornerB.getZ()));
		BlockPos blockpos3 = new BlockPos(Math.max(stage.cornerA.getX(), stage.cornerB.getX()), Math.max(stage.cornerB.getY(), stage.cornerA.getY()), Math.max(stage.cornerA.getZ(), stage.cornerB.getZ()));
		
		int affectedBlocks = 0;
		
		for (int x = blockpos2.getX(); x <= blockpos3.getX(); x++)
			for (int y = blockpos2.getY(); y <= blockpos3.getY(); y++)
				for (int z = blockpos2.getZ(); z <= blockpos3.getZ(); z++)
				{
					BlockPos pos = new BlockPos(x, y, z);
					
					if (stageLevel.getBlockEntity(pos) instanceof InkColorTileEntity colorTile)
					{
						if (colorTile.getInkColor() == teamColor && !colorTile.getTeam().equals(teamId))
						{
							colorTile.setTeam("");
							affectedBlocks++;
						}
					}
				}
		
		stage.removeTeam(teamId);
		
		final int finalAffectedBlocks = affectedBlocks;
		source.sendFeedback(() -> Text.translatable("commands.stage.teams.remove.success", Text.literal(teamId).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(teamColor.getColor()))), stageId, finalAffectedBlocks), true);
		return teamColor;
	}
	private static int warpPlayers(ServerCommandSource source, String stageId, Collection<ServerPlayerEntity> targets, boolean setSpawn) throws CommandSyntaxException
	{
		return warpPlayers(source, stageId, targets, setSpawn, InkColor.INVALID);
	}
	private static int warpPlayers(ServerCommandSource source, String stageId, Collection<ServerPlayerEntity> targets, boolean setSpawn, InkColor color) throws CommandSyntaxException
	{
		Map<String, Stage> stages = SaveInfoCapability.get().getStages();
		
		if (!stages.containsKey(stageId))
			throw STAGE_NOT_FOUND.create(stageId);
		
		Stage stage = stages.get(stageId);
		HashMap<InkColor, ArrayList<SpawnPadTileEntity>> spawnPads = stage.getSpawnPads(source.getWorld());
		ServerWorld stageLevel = stage.getStageLevel(source.getServer());
		
		if (spawnPads.isEmpty())
			throw NO_SPAWN_PADS_FOUND.create(stageId);
		
		HashMap<InkColor, Integer> playersTeleported = new HashMap<>();
		for (ServerPlayerEntity player : targets)
		{
			InkColor playerColor = color.isInvalid() ? ColorUtils.getEntityColor(player) : color;
			
			if (spawnPads.containsKey(playerColor))
			{
				if (!playersTeleported.containsKey(playerColor))
					playersTeleported.put(playerColor, 0);
				
				SpawnPadTileEntity te = spawnPads.get(playerColor).get(playersTeleported.get(playerColor) % spawnPads.get(playerColor).size());
				
				float pitch = te.getWorld().getBlockState(te.getPos()).get(SpawnPadBlock.DIRECTION).asRotation();
				
				if (stageLevel == player.getWorld())
					player.networkHandler.requestTeleport(te.getPos().getX() + .5, te.getPos().getY() + .5, te.getPos().getZ() + .5, pitch, 0);
				else
					player.teleport(stageLevel, te.getPos().getX() + .5, te.getPos().getY() + .5, te.getPos().getZ(), pitch, 0);
				
				if (setSpawn)
				{
					player.setSpawnPoint(player.getWorld().getRegistryKey(), te.getPos(), player.getWorld().getBlockState(te.getPos()).get(SpawnPadBlock.DIRECTION).asRotation(), false, true);
				}
				
				playersTeleported.put(playerColor, playersTeleported.get(playerColor) + 1);
			}
		}
		
		int result = playersTeleported.values().stream().mapToInt(i -> i).sum();
		
		if (result == 0)
			throw NO_PLAYERS_FOUND.create(stageId);
		
		source.sendFeedback(() -> Text.translatable("commands.stage.warp.success", result, stageId), true);
		return result;
	}
	private static int warpPlayersToAny(ServerCommandSource source, String stageId, Collection<ServerPlayerEntity> targets, boolean setSpawn) throws CommandSyntaxException
	{
		Map<String, Stage> stages = SaveInfoCapability.get().getStages();
		
		if (!stages.containsKey(stageId))
			throw STAGE_NOT_FOUND.create(stageId);
		
		Stage stage = stages.get(stageId);
		ServerWorld stageLevel = stage.getStageLevel(source.getServer());
		ArrayList<SpawnPadTileEntity> spawnPads = new ArrayList<>(stage.getAllSpawnPads(stageLevel));
		
		if (spawnPads.isEmpty())
			throw NO_SPAWN_PADS_FOUND.create(stageId);
		
		int playersTeleported = 0;
		for (ServerPlayerEntity player : targets)
		{
			SpawnPadTileEntity te = spawnPads.get(playersTeleported % spawnPads.size());
			
			float pitch = te.getWorld().getBlockState(te.getPos()).get(SpawnPadBlock.DIRECTION).asRotation();
			
			if (stageLevel == player.getWorld())
				player.networkHandler.requestTeleport(te.getPos().getX() + .5, te.getPos().getY() + .5, te.getPos().getZ() + .5, pitch, 0);
			else
				player.teleport(stageLevel, te.getPos().getX() + .5, te.getPos().getY() + .5, te.getPos().getZ(), pitch, 0);
			
			if (setSpawn)
				player.setSpawnPoint(player.getWorld().getRegistryKey(), te.getPos(), player.getWorld().getBlockState(te.getPos()).get(SpawnPadBlock.DIRECTION).asRotation(), false, true);
			
			playersTeleported++;
		}
		
		int result = playersTeleported;
		
		if (result == 0)
			throw NO_PLAYERS_FOUND.create(stageId);
		
		source.sendFeedback(() -> Text.translatable("commands.stage.warp.success", result, stageId), true);
		return result;
	}
	public static int playStage(ServerCommandSource source, String stageId, Collection<ServerPlayerEntity> players, boolean assignTeams, StageGameMode gameMode) throws CommandSyntaxException
	{
		Map<String, Stage> stages = SaveInfoCapability.get().getStages();
		
		if (!stages.containsKey(stageId))
			throw STAGE_NOT_FOUND.create(stageId);
		
		Stage stage = stages.get(stageId);
		World stageLevel = stage.getStageLevel(source.getServer());
		Collection<String> teamIds = stage.getTeamIds();
		
		if (teamIds.size() < 2)
			throw NOT_ENOUGH_TEAMS.create(stageId);
		
		players = players.stream().filter(v -> !EntityInfoCapability.get(v).isPlaying()).toList();
		
		if (assignTeams)
		{
			List<String> availableTeams = new ArrayList<>(teamIds);
			for (ServerPlayerEntity player : players)
			{
				String teamId = Util.getRandom(availableTeams, stageLevel.getRandom());
				ColorUtils.setPlayerColor(player, stage.getTeamColor(teamId), true);
				availableTeams.remove(teamId);
				
				if (availableTeams.isEmpty())
					availableTeams.addAll(teamIds);
			}
		}
		
		int playersTeleported = warpPlayers(source, stageId, players, true);
		
		if (!stage.play(stageLevel, players, gameMode))
			throw ALREADY_PLAYING.create(stageId);
		
		return playersTeleported;
	}
	private static int setStageCoords(ServerCommandSource source, String stageId, BlockPos pos, boolean isCornerA) throws CommandSyntaxException
	{
		Map<String, Stage> stages = SaveInfoCapability.get().getStages();
		
		if (!stages.containsKey(stageId))
			throw STAGE_NOT_FOUND.create(stageId);
		
		Stage stage = stages.get(stageId);
		
		if (isCornerA)
			stage.updateBounds(source.getWorld(), pos, stage.cornerB);
		else
			stage.updateBounds(source.getWorld(), stage.cornerA, pos);
		
		SplatcraftPacketHandler.sendToAll(new UpdateStageListPacket(stages));
		source.sendFeedback(() -> Text.translatable("commands.stage.setting.area.success", isCornerA ? "A" : "B", stageId, pos.getX(), pos.getY(), pos.getZ()), true);
		return 1;
	}
	private static int getStageCoords(ServerCommandSource source, String stageId, boolean isCornerA) throws CommandSyntaxException
	{
		Map<String, Stage> stages = SaveInfoCapability.get().getStages();
		
		if (!stages.containsKey(stageId))
			throw STAGE_NOT_FOUND.create(stageId);
		
		Stage stage = stages.get(stageId);
		
		BlockPos pos = isCornerA ? stage.cornerA : stage.cornerB;
		
		source.sendFeedback(() -> Text.translatable("commands.stage.setting.area.get", isCornerA ? "A" : "B", stageId, pos.getX(), pos.getY(), pos.getZ()), true);
		
		return 1;
	}
	public static BlockPos getOrLoadBlockPos(CommandContext<ServerCommandSource> source, String p_118244_) throws CommandSyntaxException
	{
		BlockPos blockpos = source.getArgument(p_118244_, PosArgument.class).toAbsoluteBlockPos(source.getSource());
		if (!source.getSource().getWorld().isInBuildLimit(blockpos))
		{
			throw BlockPosArgumentType.OUT_OF_WORLD_EXCEPTION.create();
		}
		else
		{
			return blockpos;
		}
	}
}