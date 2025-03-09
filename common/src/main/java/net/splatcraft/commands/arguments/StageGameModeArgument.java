package net.splatcraft.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.StringRepresentableArgument;
import net.splatcraft.data.StageGameMode;

public class StageGameModeArgument extends StringRepresentableArgument<StageGameMode>
{
	protected StageGameModeArgument()
	{
		super(StageGameMode.CODEC, StageGameMode::values);
	}
	public static StageGameMode getStageGameMode(CommandContext<CommandSourceStack> context, String id)
	{
		return context.getArgument(id, StageGameMode.class);
	}
	public static StageGameModeArgument stageGamemode()
	{
		return new StageGameModeArgument();
	}
}
