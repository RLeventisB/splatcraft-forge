package net.splatcraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.splatcraft.commands.arguments.InkColorArgument;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;

import java.util.Collection;
import java.util.Map;

public class InkColorCommand
{
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
	{
		dispatcher.register(CommandManager.literal("inkcolor").requires(commandSource -> commandSource.hasPermissionLevel(2))
			.then(CommandManager.argument("color", InkColorArgument.inkColor()).executes(
				context -> setColor(context.getSource(), InkColorArgument.getInkColor(context, "color"))
			).then(CommandManager.argument("targets", EntityArgumentType.players()).executes(
				context -> setColor(context.getSource(), InkColorArgument.getInkColor(context, "color"), EntityArgumentType.getPlayers(context, "targets"))
			)))
			.then(StageCommand.stageId("stage").then(StageCommand.stageTeam("team", "stage")
				.executes(context -> setColorByTeam(context.getSource(), StringArgumentType.getString(context, "stage"), StringArgumentType.getString(context, "team")))
				.then(CommandManager.argument("targets", EntityArgumentType.players())
					.executes(context -> setColorByTeam(context.getSource(), StringArgumentType.getString(context, "stage"), StringArgumentType.getString(context, "team"), EntityArgumentType.getPlayers(context, "targets")))
				))));
	}
	private static int setColor(ServerCommandSource source, InkColor color) throws CommandSyntaxException
	{
		ColorUtils.setPlayerColor(source.getPlayerOrThrow(), color);
		
		source.sendFeedback(() ->
		{
			try
			{
				return Text.translatable("commands.inkcolor.success.single", source.getPlayerOrThrow().getDisplayName(), getColorName(color));
			}
			catch (CommandSyntaxException e)
			{
				throw new RuntimeException(e);
			}
		}/*ColorUtils.getFormatedColorName(color, false)*/, true);
		return 1;
	}
	//TODO server friendly feedback message
	public static MutableText getColorName(InkColor color)
	{
		return Text.literal("#" + String.format("%06X", color.getColor()).toUpperCase()).setStyle(Style.EMPTY.withColor(color.getTextColor()));
	}
	private static int setColor(ServerCommandSource source, InkColor color, Collection<ServerPlayerEntity> targets)
	{
		targets.forEach(player -> ColorUtils.setPlayerColor(player, color));
		
		if (targets.size() == 1)
		{
			source.sendFeedback(() -> Text.translatable("commands.inkcolor.success.single", targets.iterator().next().getDisplayName(), getColorName(color)), true);
		}
		else
		{
			source.sendFeedback(() -> Text.translatable("commands.inkcolor.success.multiple", targets.size(), getColorName(color)), true);
		}
		
		return targets.size();
	}
	private static int setColorByTeam(ServerCommandSource source, String stageId, String teamId, Collection<ServerPlayerEntity> targets) throws CommandSyntaxException
	{
		Map<String, Stage> stages = SaveInfoCapability.get().getStages();
		if (!stages.containsKey(stageId))
			throw StageCommand.STAGE_NOT_FOUND.create(stageId);
		
		Stage stage = stages.get(stageId);
		
		if (!stage.hasTeam(teamId))
			throw StageCommand.TEAM_NOT_FOUND.create(new Object[] {teamId, stageId});
		
		return setColor(source, stage.getTeamColor(teamId), targets);
	}
	private static int setColorByTeam(ServerCommandSource source, String stageId, String teamId) throws CommandSyntaxException
	{
		Map<String, Stage> stages = SaveInfoCapability.get().getStages();
		if (!stages.containsKey(stageId))
			throw StageCommand.STAGE_NOT_FOUND.create(stageId);
		
		Stage stage = stages.get(stageId);
		
		if (!stage.hasTeam(teamId))
			throw StageCommand.TEAM_NOT_FOUND.create(new Object[] {teamId, stageId});
		
		return setColor(source, stage.getTeamColor(teamId));
	}
}