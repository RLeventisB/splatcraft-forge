package net.splatcraft.data;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.items.remotes.InkDisruptorItem;
import net.splatcraft.items.remotes.TurfScannerItem;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public enum StageGameMode implements StringRepresentable
{
	RECON(60 * 60, (session, world) -> true, (session, world) -> false, (session, world) ->
	{
	}, (session, world) ->
	{
	}),
	TURF_WAR(3 * 60, TurfWar::canStart, (session, world) -> false, (session, world) ->
	{
	}, TurfWar::onEnd),
	SPLAT_ZONES(5 * 60, (session, world) -> true, (session, world) -> false, (session, world) ->
	{
	}, (session, world) ->
	{
	}),
	RAINMAKER(5 * 60, (session, world) -> true, (session, world) -> false, (session, world) ->
	{
	}, (session, world) ->
	{
	}),
	CLAM_BLITZ(5 * 60, (session, world) -> true, (session, world) -> false, (session, world) ->
	{
	}, (session, world) ->
	{
	});
	public static final Codec<StageGameMode> CODEC = StringRepresentable.fromEnum(StageGameMode::values);
	public final int DEFAULT_TIME_SECONDS;
	public final GamemodeStagePredicateCallback playChecker;
	public final GamemodePredicateCallback overtimeChecker;
	public final GamemodeConsumerCallback tick;
	public final GamemodeConsumerCallback onEnd;
	StageGameMode(int defaultTime,
	              GamemodeStagePredicateCallback playChecker,
	              GamemodePredicateCallback overtimeChecker,
	              GamemodeConsumerCallback tick,
	              GamemodeConsumerCallback onEnd)
	{
		DEFAULT_TIME_SECONDS = defaultTime;
		this.playChecker = playChecker;
		this.overtimeChecker = overtimeChecker;
		this.tick = tick;
		this.onEnd = onEnd;
	}
	public boolean canDoOn(Stage stage, Level world)
	{
		return playChecker.test(stage, (ServerLevel) world);
	}
	@Override
	public @NotNull String getSerializedName()
	{
		return name();
	}
	public interface GamemodeConsumerCallback
	{
		void consume(PlaySession session, ServerLevel world);
	}
	public interface GamemodePredicateCallback
	{
		boolean test(PlaySession session, ServerLevel world);
	}
	public interface GamemodeStagePredicateCallback
	{
		boolean test(Stage stage, ServerLevel world);
	}
	public static class TurfWar
	{
		private static void onEnd(PlaySession session, ServerLevel world)
		{
			Stage stage = SaveInfoCapability.get().stages().get(session.stageId);
			TurfScannerItem.scanTurf(world, world, stage.cornerA, stage.cornerB, 0, session.playerUuids.stream().map(uuid -> (ServerPlayer) world.getPlayerByUUID(uuid)).filter(Objects::nonNull).toList());
		}
		public static boolean canStart(Stage stage, ServerLevel world)
		{
			InkDisruptorItem.clearInk(world, stage.cornerA, stage.cornerB, false);

			return true;
		}
	}
}
