package net.splatcraft.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.splatcraft.data.StageGameMode;

public class StageGameModeArgument extends EnumArgumentType<StageGameMode>
{
	protected StageGameModeArgument()
	{
		super(StageGameMode.CODEC, StageGameMode::values);
	}
	public static StageGameMode getStageGameMode(CommandContext<ServerCommandSource> context, String id)
	{
		return context.getArgument(id, StageGameMode.class);
	}
	public static StageGameModeArgument stageGamemode()
	{
		return new StageGameModeArgument();
	}
}
