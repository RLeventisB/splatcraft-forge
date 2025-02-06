package net.splatcraft.data;

import com.mojang.serialization.Codec;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.world.World;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.items.remotes.InkDisruptorItem;
import net.splatcraft.items.remotes.TurfScannerItem;

import java.util.Objects;

public enum StageGameMode implements StringIdentifiable
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
	public static final Codec<StageGameMode> CODEC = StringIdentifiable.createCodec(StageGameMode::values);
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
	public boolean canDoOn(Stage stage, World world)
	{
		return playChecker.test(stage, (ServerWorld) world);
	}
	@Override
	public String asString()
	{
		return name();
	}
	public interface GamemodeConsumerCallback
	{
		void consume(PlaySession session, ServerWorld world);
	}
	public interface GamemodePredicateCallback
	{
		boolean test(PlaySession session, ServerWorld world);
	}
	public interface GamemodeStagePredicateCallback
	{
		boolean test(Stage stage, ServerWorld world);
	}
	public static class TurfWar
	{
		private static void onEnd(PlaySession session, ServerWorld world)
		{
			Stage stage = SaveInfoCapability.get().stages().get(session.stageId);
			TurfScannerItem.scanTurf(world, world, stage.cornerA, stage.cornerB, 0, session.playerUuids.stream().map(uuid -> (ServerPlayerEntity) world.getPlayerByUuid(uuid)).filter(Objects::nonNull).toList());
		}
		public static boolean canStart(Stage stage, ServerWorld world)
		{
			InkDisruptorItem.clearInk(world, stage.cornerA, stage.cornerB, false);
			
			return true;
		}
	}
}
