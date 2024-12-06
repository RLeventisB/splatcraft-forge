package net.splatcraft.forge.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.splatcraft.forge.data.Stage;
import net.splatcraft.forge.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.forge.items.remotes.InkDisruptorItem;
import net.splatcraft.forge.items.remotes.RemoteItem;

public class ClearInkCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
            Commands.literal("clearink").requires(commandSource -> commandSource.hasPermission(2))
                .then(Commands.argument("from", BlockPosArgument.blockPos())
                    .then(Commands.argument("to", BlockPosArgument.blockPos())
                        .executes(context -> execute(context, false))
                        .then(Commands.argument("removePermanent", BoolArgumentType.bool())
                            .executes(context -> execute(context, BoolArgumentType.getBool(context, "removePermanent")))
                        )
                    )
                ).then(StageCommand.stageId("stage").executes(context1 -> executeStage(context1, false))
                    .then(Commands.argument("removePermanent", BoolArgumentType.bool())
                        .executes(context -> executeStage(context, BoolArgumentType.getBool(context, "removePermanent")))
                    )
                )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context, boolean removePermanent) throws CommandSyntaxException
    {
        return execute(context.getSource(), StageCommand.getOrLoadBlockPos(context, "from"), StageCommand.getOrLoadBlockPos(context, "to"), removePermanent);
    }

    private static int executeStage(CommandContext<CommandSourceStack> context, boolean removePermanent) throws CommandSyntaxException
    {
        String stageId = StringArgumentType.getString(context, "stage");
        Stage stage = SaveInfoCapability.get(context.getSource().getServer()).getStages().get(stageId);

        if (stage == null)
            throw StageCommand.STAGE_NOT_FOUND.create(stageId);

        return execute(context.getSource(), stage.cornerA, stage.cornerB, removePermanent);
    }

    private static int execute(CommandSourceStack source, BlockPos from, BlockPos to, boolean removePermanent)
    {
        RemoteItem.RemoteResult result = InkDisruptorItem.clearInk(source.getLevel(), from, to, removePermanent);

        source.sendSuccess(result::getOutput, true);
        return result.getCommandResult();
    }
}
