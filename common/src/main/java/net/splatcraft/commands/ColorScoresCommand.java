package net.splatcraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.splatcraft.commands.arguments.ColorCriterionArgument;
import net.splatcraft.commands.arguments.InkColorArgument;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateColorScoresPacket;
import net.splatcraft.util.structs.InkColor;

import java.util.ArrayList;
import java.util.Collection;

public class ColorScoresCommand
{
	private static final SimpleCommandExceptionType CRITERION_ALREADY_EXISTS_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("commands.colorscores.add.duplicate"));
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		dispatcher.register(Commands.literal("colorscores").requires(commandSource -> commandSource.hasPermission(2))
			.then(Commands.literal("add").then(Commands.argument("color", InkColorArgument.inkColor()).executes(ColorScoresCommand::add)))
			.then(Commands.literal("remove").then(Commands.argument("color", ColorCriterionArgument.colorCriterion()).executes(ColorScoresCommand::remove)))
			.then(Commands.literal("list").executes(ColorScoresCommand::list))
		);
	}
	protected static void update()
	{
		SplatcraftPacketHandler.sendToAll(new UpdateColorScoresPacket(true, true, new ArrayList<>(ScoreboardHandler.getCriteriaKeySet())));
	}
	protected static int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		InkColor color = InkColorArgument.getInkColor(context, "color");
		CommandSourceStack source = context.getSource();
		
		if (ScoreboardHandler.hasColorCriterion(color))
		{
			throw CRITERION_ALREADY_EXISTS_EXCEPTION.create();
		}
		ScoreboardHandler.createColorCriterion(color);
		SaveInfoCapability.get().addInitializedColorScores(color);
		update();
		
		source.sendSuccess(() -> Component.translatable("commands.colorscores.add.success", InkColorCommand.getColorName(color)), true);
		
		return color.getColor();
	}
	protected static int remove(CommandContext<CommandSourceStack> context)
	{
		InkColor color = ColorCriterionArgument.getInkColor(context, "color");
		ScoreboardHandler.removeColorCriterion(color);
		SaveInfoCapability.get().removeColorScore(color);
		update();
		
		context.getSource().sendSuccess(() -> Component.translatable("commands.colorscores.remove.success", InkColorCommand.getColorName(color)), true);
		
		return color.getColor();
	}
	protected static int list(CommandContext<CommandSourceStack> context)
	{
		Collection<InkColor> collection = ScoreboardHandler.getCriteriaKeySet();
		
		if (collection.isEmpty())
		{
			context.getSource().sendSuccess(() -> Component.translatable("commands.colorscores.list.empty"), false);
		}
		else
		{
			context.getSource().sendSuccess(() -> Component.translatable("commands.colorscores.list.count", collection.size()), false);
			collection.forEach(color ->
				context.getSource().sendSuccess(() -> Component.translatable("commands.colorscores.list.entry", color.getLocalizedName(), InkColorCommand.getColorName(color)), false));
		}
		
		return collection.size();
	}
}
