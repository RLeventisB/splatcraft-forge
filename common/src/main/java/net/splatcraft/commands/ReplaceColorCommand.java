package net.splatcraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;
import net.splatcraft.commands.arguments.InkColorArgument;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.items.remotes.ColorChangerItem;
import net.splatcraft.items.remotes.RemoteItem;
import net.splatcraft.util.InkColor;

public class ReplaceColorCommand
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        dispatcher.register(CommandManager.literal("replacecolor").requires(commandSource -> commandSource.hasPermissionLevel(2))
            .then(CommandManager.argument("from", BlockPosArgumentType.blockPos()).then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                .then(CommandManager.argument("color", InkColorArgument.inkColor())
                    .executes(context -> execute(context, 0))
                    .then(CommandManager.literal("only").then(CommandManager.argument("affectedColor", InkColorArgument.inkColor()).executes(context -> execute(context, 1))))
                    .then(CommandManager.literal("keep").then(CommandManager.argument("affectedColor", InkColorArgument.inkColor()).executes(context -> execute(context, 2))))
                )))
            .then(StageCommand.stageId("stage")
                .then(CommandManager.argument("color", InkColorArgument.inkColor())
                    .executes(context -> executeStage(context, 0))
                    .then(CommandManager.literal("only")
                        .then(StageCommand.stageTeam("affectedTeam", "stage").executes(context -> executeStageForTeam(context, 1)))
                        .then(CommandManager.argument("affectedColor", InkColorArgument.inkColor()).executes(context -> executeStage(context, 1))))
                    .then(CommandManager.literal("keep")
                        .then(StageCommand.stageTeam("affectedTeam", "stage").executes(context -> executeStageForTeam(context, 2)))
                        .then(CommandManager.argument("affectedColor", InkColorArgument.inkColor()).executes(context -> executeStage(context, 2))))
                ))
        );
    }

    public static int executeStage(CommandContext<ServerCommandSource> context, int mode) throws CommandSyntaxException
    {
        String stageId = StringArgumentType.getString(context, "stage");
        Stage stage = SaveInfoCapability.get().getStages().get(stageId);

        if (stage == null)
            throw StageCommand.STAGE_NOT_FOUND.create(null);

        if (mode == 0)
        {
            return execute(context.getSource(), stage.cornerA, stage.cornerB, InkColorArgument.getInkColor(context, "color"), InkColor.INVALID, mode, stageId, "");
        }
        return execute(context.getSource(), stage.cornerA, stage.cornerB, InkColorArgument.getInkColor(context, "color"), InkColorArgument.getInkColor(context, "affectedColor"), mode, stageId, "");
    }

    public static int executeStageForTeam(CommandContext<ServerCommandSource> context, int mode) throws CommandSyntaxException
    {
        String stageId = StringArgumentType.getString(context, "stage");
        Stage stage = SaveInfoCapability.get().getStages().get(stageId);

        if (stage == null)
            throw StageCommand.STAGE_NOT_FOUND.create(stageId);

        InkColor color = InkColorArgument.getInkColor(context, "color");
        String team = StringArgumentType.getString(context, "affectedTeam");

        if (mode == 0)
            return execute(context.getSource(), stage.cornerA, stage.cornerB, color, InkColor.INVALID, mode, stageId, team);

        if (!stage.hasTeam(team))
            throw StageCommand.TEAM_NOT_FOUND.create(new Object[]{team, stageId});

        InkColor teamColor = stage.getTeamColor(team);
        stage.setTeamColor(team, color);

        return execute(context.getSource(), stage.cornerA, stage.cornerB, color, teamColor, mode, stageId, team);
    }

    public static int execute(CommandContext<ServerCommandSource> context, int mode) throws CommandSyntaxException
    {
        if (mode == 0)
        {
            return execute(context.getSource(), BlockPosArgumentType.getLoadedBlockPos(context, "from"), BlockPosArgumentType.getLoadedBlockPos(context, "to"), InkColorArgument.getInkColor(context, "color"), InkColor.INVALID, mode, "", "");
        }
        return execute(context.getSource(), StageCommand.getOrLoadBlockPos(context, "from"), StageCommand.getOrLoadBlockPos(context, "to"), InkColorArgument.getInkColor(context, "color"), InkColorArgument.getInkColor(context, "affectedColor"), mode, "", "");
    }

    public static int execute(ServerCommandSource source, BlockPos from, BlockPos to, InkColor color, InkColor affectedColor, int mode, String affectedStage, String affectedTeam)
    {
        RemoteItem.RemoteResult result = ColorChangerItem.replaceColor(source.getWorld(), from, to, color, mode, affectedColor, affectedStage, affectedTeam);

        source.sendFeedback(result::getOutput, true);
        return result.getCommandResult();
    }
}
