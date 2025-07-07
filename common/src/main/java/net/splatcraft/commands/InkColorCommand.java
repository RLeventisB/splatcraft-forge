package net.splatcraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.splatcraft.commands.arguments.InkColorArgument;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;

import java.util.Collection;
import java.util.Map;

public class InkColorCommand
{
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		dispatcher.register(Commands.literal("inkcolor").requires(commandSource -> commandSource.hasPermission(2))
			.then(Commands.argument("color", InkColorArgument.inkColor()).executes(
				context -> setColor(context.getSource(), InkColorArgument.getInkColor(context, "color"))
			).then(Commands.argument("targets", EntityArgument.players()).executes(
				context -> setColor(context.getSource(), InkColorArgument.getInkColor(context, "color"), EntityArgument.getPlayers(context, "targets"))
			)))
			.then(StageCommand.stageId("stage").then(StageCommand.stageTeam("team", "stage")
				.executes(context -> setColorByTeam(context.getSource(), StringArgumentType.getString(context, "stage"), StringArgumentType.getString(context, "team")))
				.then(Commands.argument("targets", EntityArgument.players())
					.executes(context -> setColorByTeam(context.getSource(), StringArgumentType.getString(context, "stage"), StringArgumentType.getString(context, "team"), EntityArgument.getPlayers(context, "targets")))
				))));
	}
	private static int setColor(CommandSourceStack source, InkColor color) throws CommandSyntaxException
	{
		ColorUtils.setPlayerColor(source.getPlayerOrException(), color);
		
		source.sendSuccess(() ->
		{
			try
			{
				return Component.translatable("commands.inkcolor.success.single", source.getPlayerOrException().getDisplayName(), getColorName(color));
			}
			catch (CommandSyntaxException e)
			{
				throw new RuntimeException(e);
			}
		}/*ColorUtils.getFormatedColorName(color, false)*/, true);
		return 1;
	}
	//TODO server friendly feedback message
	public static MutableComponent getColorName(InkColor color)
	{
		return Component.literal("#" + String.format("%06X", color.getColor()).toUpperCase()).setStyle(Style.EMPTY.withColor(color.getTextColor()));
	}
	private static int setColor(CommandSourceStack source, InkColor color, Collection<ServerPlayer> targets)
	{
		targets.forEach(player -> ColorUtils.setPlayerColor(player, color));
		
		if (targets.size() == 1)
		{
			source.sendSuccess(() -> Component.translatable("commands.inkcolor.success.single", targets.iterator().next().getDisplayName(), getColorName(color)), true);
		}
		else
		{
			source.sendSuccess(() -> Component.translatable("commands.inkcolor.success.multiple", targets.size(), getColorName(color)), true);
		}
		
		return targets.size();
	}
	private static int setColorByTeam(CommandSourceStack source, String stageId, String teamId, Collection<ServerPlayer> targets) throws CommandSyntaxException
	{
		Map<String, Stage> stages = SaveInfoCapability.get().stages();
		if (!stages.containsKey(stageId))
			throw StageCommand.STAGE_NOT_FOUND.create(stageId);
		
		Stage stage = stages.get(stageId);
		
		if (!stage.hasTeam(teamId))
			throw StageCommand.TEAM_NOT_FOUND.create(new Object[] {teamId, stageId});
		
		return setColor(source, stage.getTeamColor(teamId), targets);
	}
	private static int setColorByTeam(CommandSourceStack source, String stageId, String teamId) throws CommandSyntaxException
	{
		Map<String, Stage> stages = SaveInfoCapability.get().stages();
		if (!stages.containsKey(stageId))
			throw StageCommand.STAGE_NOT_FOUND.create(stageId);
		
		Stage stage = stages.get(stageId);
		
		if (!stage.hasTeam(teamId))
			throw StageCommand.TEAM_NOT_FOUND.create(new Object[] {teamId, stageId});
		
		return setColor(source, stage.getTeamColor(teamId));
	}
}