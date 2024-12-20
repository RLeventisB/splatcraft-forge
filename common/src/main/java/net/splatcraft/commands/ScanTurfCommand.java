package net.splatcraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.BlockPos;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.items.remotes.RemoteItem;
import net.splatcraft.items.remotes.TurfScannerItem;
import net.splatcraft.registries.SplatcraftStats;
import net.splatcraft.util.InkColor;

import java.util.Collection;
import java.util.Collections;

public class ScanTurfCommand
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        dispatcher.register(CommandManager.literal("scanturf").requires(commandSource -> commandSource.hasPermissionLevel(2)).then(CommandManager.argument("from", BlockPosArgumentType.blockPos()).then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                .executes(ScanTurfCommand::executeOnSelf)
                .then(CommandManager.argument("target", EntityArgumentType.players()).executes(context -> execute(context, 0))
                    .then(CommandManager.literal("topDown").executes(context -> execute(context, 0)))
                    .then(CommandManager.literal("multiLayered").executes(context -> execute(context, 1))))
            ))
            .then(StageCommand.stageId("stage").executes(ScanTurfCommand::executeStageOnSelf)
                .then(CommandManager.argument("target", EntityArgumentType.players()).executes(context -> executeStage(context, 0))
                    .then(CommandManager.literal("topDown").executes(context -> executeStage(context, 0)))
                    .then(CommandManager.literal("multiLayered").executes(context -> executeStage(context, 1))))));
    }

    private static int executeStageOnSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        return executeStage(context.getSource(), StringArgumentType.getString(context, "stage"), 0,
            context.getSource().getEntity() instanceof ServerPlayerEntity player ? Collections.singletonList(player) : RemoteItem.ALL_TARGETS);
    }

    private static int executeOnSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        return execute(context.getSource(), StageCommand.getOrLoadBlockPos(context, "from"), StageCommand.getOrLoadBlockPos(context, "to"), 0,
            context.getSource().getEntity() instanceof ServerPlayerEntity ? Collections.singletonList((ServerPlayerEntity) context.getSource().getEntity()) : RemoteItem.ALL_TARGETS);
    }

    private static int executeStage(CommandContext<ServerCommandSource> context, int mode) throws CommandSyntaxException
    {
        return executeStage(context.getSource(), StringArgumentType.getString(context, "stage"), mode, EntityArgumentType.getPlayers(context, "target"));
    }

    private static int execute(CommandContext<ServerCommandSource> context, int mode) throws CommandSyntaxException
    {
        return execute(context.getSource(), StageCommand.getOrLoadBlockPos(context, "from"), StageCommand.getOrLoadBlockPos(context, "to"), mode, EntityArgumentType.getPlayers(context, "target"));
    }

    private static int executeStage(ServerCommandSource source, String stageId, int mode, Collection<ServerPlayerEntity> targets) throws CommandSyntaxException
    {

        Stage stage = SaveInfoCapability.get().getStages().get(stageId);

        if (stage == null)
            throw StageCommand.STAGE_NOT_FOUND.create(stageId);

        int result = execute(source, stage.cornerA, stage.cornerB, mode, targets);

        for (String team : stage.getTeamIds())
        {
            if (stage.getTeamColor(team) == InkColor.constructOrReuse(result))
                source.getWorld().getScoreboard().forEachScore(Stats.CUSTOM.getOrCreateStat(SplatcraftStats.TURF_WARS_WON), ScoreHolder.fromName("[" + team + "]"), score -> score.incrementScore(1));
        }

        return result;
    }

    private static int execute(ServerCommandSource source, BlockPos from, BlockPos to, int mode, Collection<ServerPlayerEntity> targets)
    {
        RemoteItem.RemoteResult result = TurfScannerItem.scanTurf(source.getWorld(), source.getWorld(), from, to, mode, targets);

        source.sendFeedback(result::getOutput, true);

        return result.getCommandResult();
    }
}
