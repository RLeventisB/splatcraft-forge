package net.splatcraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.items.remotes.InkDisruptorItem;
import net.splatcraft.items.remotes.RemoteItem;

public class ClearInkCommand
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        dispatcher.register(
            CommandManager.literal("clearink").requires(commandSource -> commandSource.hasPermissionLevel(2))
                .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                    .then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                        .executes(context -> execute(context, false))
                        .then(CommandManager.argument("removePermanent", BoolArgumentType.bool())
                            .executes(context -> execute(context, BoolArgumentType.getBool(context, "removePermanent")))
                        )
                    )
                ).then(StageCommand.stageId("stage").executes(context1 -> executeStage(context1, false))
                    .then(CommandManager.argument("removePermanent", BoolArgumentType.bool())
                        .executes(context -> executeStage(context, BoolArgumentType.getBool(context, "removePermanent")))
                    )
                )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context, boolean removePermanent) throws CommandSyntaxException
    {
        return execute(context.getSource(), StageCommand.getOrLoadBlockPos(context, "from"), StageCommand.getOrLoadBlockPos(context, "to"), removePermanent);
    }

    private static int executeStage(CommandContext<ServerCommandSource> context, boolean removePermanent) throws CommandSyntaxException
    {
        String stageId = StringArgumentType.getString(context, "stage");
        Stage stage = SaveInfoCapability.get().getStages().get(stageId);

        if (stage == null)
            throw StageCommand.STAGE_NOT_FOUND.create(stageId);

        return execute(context.getSource(), stage.cornerA, stage.cornerB, removePermanent);
    }

    private static int execute(ServerCommandSource source, BlockPos from, BlockPos to, boolean removePermanent)
    {
        RemoteItem.RemoteResult result = InkDisruptorItem.clearInk(source.getWorld(), from, to, removePermanent);

        source.sendFeedback(result::getOutput, true);
        return result.getCommandResult();
    }
}
