package net.splatcraft.registries;

import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.splatcraft.commands.*;
import net.splatcraft.commands.arguments.ColorCriterionArgument;
import net.splatcraft.commands.arguments.InkColorArgument;
import net.splatcraft.commands.arguments.StageGameModeArgument;
import net.splatcraft.platform.Services;

public class SplatcraftCommands
{
	public static void registerCommands()
	{
		Services.PLATFORM.registerCommands((dispatcher, registryAccess, environment) ->
		{
			InkColorCommand.register(dispatcher);
			ScanTurfCommand.register(dispatcher);
			ClearInkCommand.register(dispatcher);
			ReplaceColorCommand.register(dispatcher);
			ColorScoresCommand.register(dispatcher);
			StageCommand.register(dispatcher);
			SuperJumpCommand.register(dispatcher);
		});
	}
	public static void registerArguments()
	{
		Services.PLATFORM.registerCommandArgument("stage_gamemode", StageGameModeArgument.class, SingletonArgumentInfo.contextFree(StageGameModeArgument::stageGamemode));
		Services.PLATFORM.registerCommandArgument("ink_color", InkColorArgument.class, SingletonArgumentInfo.contextFree(InkColorArgument::inkColor));
		Services.PLATFORM.registerCommandArgument("color_criterion", ColorCriterionArgument.class, SingletonArgumentInfo.contextFree(ColorCriterionArgument::colorCriterion));
	}
}
