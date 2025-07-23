package net.splatcraft.registries;

import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.splatcraft.commands.*;
import net.splatcraft.commands.arguments.ColorCriterionArgument;
import net.splatcraft.commands.arguments.HighlightTypeArgument;
import net.splatcraft.commands.arguments.InkColorArgument;
import net.splatcraft.commands.arguments.StageGameModeArgument;
import net.splatcraft.platform.Services;
import net.splatcraft.platform.event.CommandRegistrationEvent;

public class SplatcraftCommands
{
	public static void registerCommands()
	{
		Services.PLATFORM.registerListener(CommandRegistrationEvent.class, (dispatcher, registryAccess, environment) ->
		{
			InkColorCommand.register(dispatcher);
			BatchWaxCommand.register(dispatcher);
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
		Services.PLATFORM.registerCommandArgument("highlight_type", HighlightTypeArgument.class, SingletonArgumentInfo.contextFree(HighlightTypeArgument::highlightType));
		Services.PLATFORM.registerCommandArgument("stage_gamemode", StageGameModeArgument.class, SingletonArgumentInfo.contextFree(StageGameModeArgument::stageGamemode));
		Services.PLATFORM.registerCommandArgument("ink_color", InkColorArgument.class, SingletonArgumentInfo.contextFree(InkColorArgument::inkColor));
		Services.PLATFORM.registerCommandArgument("color_criterion", ColorCriterionArgument.class, SingletonArgumentInfo.contextFree(ColorCriterionArgument::colorCriterion));
	}
}
