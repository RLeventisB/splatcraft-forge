package net.splatcraft.platform.event;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.splatcraft.platform.event.types.ConsumerEvent;

public interface CommandRegistrationEvent extends ConsumerEvent.Tri<CommandDispatcher<CommandSourceStack>, CommandBuildContext, Commands.CommandSelection>
{
	void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registry, Commands.CommandSelection selection);
	default void invoke(CommandDispatcher<CommandSourceStack> parameter1, CommandBuildContext parameter2, Commands.CommandSelection parameter3)
	{
		register(parameter1, parameter2, parameter3);
	}
}

