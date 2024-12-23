package net.splatcraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.splatcraft.commands.arguments.ColorCriterionArgument;
import net.splatcraft.commands.arguments.InkColorArgument;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateColorScoresPacket;
import net.splatcraft.util.InkColor;

import java.util.ArrayList;
import java.util.Collection;

public class ColorScoresCommand
{
	private static final SimpleCommandExceptionType CRITERION_ALREADY_EXISTS_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.colorscores.add.duplicate"));
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
	{
		dispatcher.register(CommandManager.literal("colorscores").requires(commandSource -> commandSource.hasPermissionLevel(2))
			.then(CommandManager.literal("add").then(CommandManager.argument("color", InkColorArgument.inkColor()).executes(ColorScoresCommand::add)))
			.then(CommandManager.literal("remove").then(CommandManager.argument("color", ColorCriterionArgument.colorCriterion()).executes(ColorScoresCommand::remove)))
			.then(CommandManager.literal("list").executes(ColorScoresCommand::list))
		);
	}
	protected static void update()
	{
		SplatcraftPacketHandler.sendToAll(new UpdateColorScoresPacket(true, true, new ArrayList<>(ScoreboardHandler.getCriteriaKeySet())));
	}
	protected static int add(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
	{
		InkColor color = InkColorArgument.getInkColor(context, "color");
		ServerCommandSource source = context.getSource();
		
		if (ScoreboardHandler.hasColorCriterion(color))
		{
			throw CRITERION_ALREADY_EXISTS_EXCEPTION.create();
		}
		ScoreboardHandler.createColorCriterion(color);
		SaveInfoCapability.get().addInitializedColorScores(color);
		update();
		
		source.sendFeedback(() -> Text.translatable("commands.colorscores.add.success", InkColorCommand.getColorName(color)), true);
		
		return color.getColor();
	}
	protected static int remove(CommandContext<ServerCommandSource> context)
	{
		InkColor color = ColorCriterionArgument.getInkColor(context, "color");
		ScoreboardHandler.removeColorCriterion(color);
		SaveInfoCapability.get().removeColorScore(color);
		update();
		
		context.getSource().sendFeedback(() -> Text.translatable("commands.colorscores.remove.success", InkColorCommand.getColorName(color)), true);
		
		return color.getColor();
	}
	protected static int list(CommandContext<ServerCommandSource> context)
	{
		Collection<InkColor> collection = ScoreboardHandler.getCriteriaKeySet();
		
		if (collection.isEmpty())
		{
			context.getSource().sendFeedback(() -> Text.translatable("commands.colorscores.list.empty"), false);
		}
		else
		{
			context.getSource().sendFeedback(() -> Text.translatable("commands.colorscores.list.count", collection.size()), false);
			collection.forEach(color ->
				context.getSource().sendFeedback(() -> Text.translatable("commands.colorscores.list.entry", color, InkColorCommand.getColorName(color)), false));
		}
		
		return collection.size();
	}
}
