package net.splatcraft.registries.neoforge;

import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.splatcraft.commands.arguments.ColorCriterionArgument;
import net.splatcraft.commands.arguments.InkColorArgument;
import net.splatcraft.commands.arguments.StageGameModeArgument;
import net.splatcraft.registries.SplatcraftCommands;

public class SplatcraftCommandsImpl
{
	public static void registerArguments()
	{
		SplatcraftCommands.ARGUMENT_REGISTRY.register("stage_gamemode", () -> ArgumentTypes.registerByClass(StageGameModeArgument.class, ConstantArgumentSerializer.of(StageGameModeArgument::stageGamemode)));
		SplatcraftCommands.ARGUMENT_REGISTRY.register("ink_color", () -> ArgumentTypes.registerByClass(InkColorArgument.class, ConstantArgumentSerializer.of(InkColorArgument::inkColor)));
		SplatcraftCommands.ARGUMENT_REGISTRY.register("color_criterion", () -> ArgumentTypes.registerByClass(ColorCriterionArgument.class, ConstantArgumentSerializer.of(ColorCriterionArgument::colorCriterion)));
	}
}
