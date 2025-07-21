package net.splatcraft.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.splatcraft.blocks.StageMarkerBlock;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.items.remotes.InkDisruptorItem;
import net.splatcraft.items.remotes.TurfScannerItem;
import net.splatcraft.platform.Services;
import net.splatcraft.tileentities.StageMarkerTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public enum StageGameMode implements StringRepresentable
{
	RECON(60 * 60, StageGameMode::cleanOnStart, StageGameMode::noOvertime, StageGameMode::noop, StageGameMode::alwaysRun, StageGameMode::noop, null, null),
	TURF_WAR(3 * 60, StageGameMode::cleanOnStart, StageGameMode::noOvertime, StageGameMode::noop, StageGameMode::alwaysRun, StageGameMode::turfWarOnEnd,
		null, null),
	SPLAT_ZONES(5 * 60,
		StageGameMode::splatZonesCheckStart,
		StageGameMode::splatZonesCheckOvertime,
		StageGameMode::splatZonesOnStart,
		StageGameMode::splatZonesTick,
		StageGameMode::splatZonesOnEnd,
		RecordCodecBuilder.<Object>create(inst -> inst.group(
			Codec.INT.fieldOf("counter").forGetter(v -> (int) ((Object[]) v)[0]),
			InkColor.CODEC.optionalFieldOf("last_color").forGetter(v -> (Optional<InkColor>) ((Object[]) v)[1]),
			Codec.INT.fieldOf("penalty_counter").forGetter(v -> (int) ((Object[]) v)[2])
		).apply(inst, (x, y, z) -> new Object[] {x, y, z})),
		StreamCodec.composite(
			ByteBufCodecs.INT, v -> (int) ((Object[]) v)[0],
			ByteBufCodecs.optional(InkColor.STREAM_CODEC), v -> (Optional<InkColor>) ((Object[]) v)[1],
			ByteBufCodecs.INT, v -> (int) ((Object[]) v)[2],
			(x, y, z) -> new Object[] {x, y, z}
		)),
	RAINMAKER(5 * 60, StageGameMode::cleanOnStart, StageGameMode::noOvertime, StageGameMode::noop, StageGameMode::alwaysRun, StageGameMode::noop, null, null),
	CLAM_BLITZ(5 * 60, StageGameMode::cleanOnStart, StageGameMode::noOvertime, StageGameMode::noop, StageGameMode::alwaysRun, StageGameMode::noop, null, null);
	public static final int SPLAT_ZONES_COUNTER_TICKS = 12;
	public static final Codec<StageGameMode> CODEC = StringRepresentable.fromEnum(StageGameMode::values);
	public final int DEFAULT_TIME_SECONDS;
	public final GamemodeStagePredicateCallback playChecker;
	public final GamemodeOvertimeCallback overtimeChecker;
	public final GamemodeConsumerCallback onStart;
	public final GamemodePredicateCallback tick;
	public final GamemodeConsumerCallback onEnd;
	public final Codec<Object> customDataCodec;
	public final StreamCodec<ByteBuf, Object> customDataStreamCodec;
	StageGameMode(int defaultTime,
	              GamemodeStagePredicateCallback playChecker,
	              GamemodeOvertimeCallback overtimeChecker,
	              GamemodeConsumerCallback onStart,
	              GamemodePredicateCallback tick,
	              GamemodeConsumerCallback onEnd,
	              Codec<Object> customDataCodec,
	              StreamCodec<ByteBuf, Object> customDataStreamCodec)
	{
		DEFAULT_TIME_SECONDS = defaultTime;
		this.playChecker = playChecker;
		this.overtimeChecker = overtimeChecker;
		this.onStart = onStart;
		this.tick = tick;
		this.onEnd = onEnd;
		this.customDataCodec = customDataCodec;
		this.customDataStreamCodec = customDataStreamCodec;
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
	public interface GamemodeOvertimeCallback
	{
		float test(PlaySession session, Level world);
	}
	public interface GamemodePredicateCallback
	{
		boolean test(PlaySession session, Level world);
	}
	public interface GamemodeStagePredicateCallback
	{
		boolean test(Stage stage, ServerLevel world);
	}
	public static boolean cleanOnStart(Stage stage, ServerLevel world)
	{
		InkDisruptorItem.clearInk(world, stage.getMinCorner(), stage.getMaxCorner(), false);
		
		return true;
	}
	public static float noOvertime(PlaySession session, Level world)
	{
		return 0f;
	}
	public static void noop(PlaySession session, Level world)
	{
	}
	public static boolean alwaysRun(PlaySession session, Level world)
	{
		return true;
	}
	private static void turfWarOnEnd(PlaySession session, ServerLevel world)
	{
		Stage stage = SaveInfoCapability.get().stages().get(session.stageId);
		TurfScannerItem.scanTurf(world, world, stage.getMinCorner(), stage.getMaxCorner(), 0, session.playerUuids.stream().map(uuid -> (ServerPlayer) world.getPlayerByUUID(uuid)).filter(Objects::nonNull).toList());
	}
	public static boolean splatZonesCheckStart(Stage stage, ServerLevel world)
	{
		AtomicBoolean hasAnyZone = new AtomicBoolean(false);
		stage.iterateChunkPos(pos ->
		{
			if (hasAnyZone.get())
				return;
			
			List<StageMarkerTileEntity> markers = StageMarkerBlock.getMarkersInChunkPos(stage.getStageWorld(Services.PLATFORM.getServerInstance()), pos);
			hasAnyZone.set(markers.stream().anyMatch(v -> v.getMarkerType() == StageMarkerTileEntity.MarkerType.SPLAT_ZONE && stage.getBounds().intersects(v.getAABB())));
		});
		
		return hasAnyZone.get();
	}
	public static float splatZonesCheckOvertime(PlaySession session, Level level)
	{
		Optional<InkColor> activeColor = getActiveColor(session.getMarkers(true));
		Map.Entry<InkColor, PlaySession.TeamScore> colorWithMostPoints = session.scores.entrySet().stream().max(Comparator.comparingInt(x -> x.getValue().score())).get();
		int mostScore = colorWithMostPoints.getValue().score() / SPLAT_ZONES_COUNTER_TICKS * SPLAT_ZONES_COUNTER_TICKS;
		boolean moreThanOneOfSameColor = session.scores.entrySet().stream().filter(v -> v.getValue().score() / SPLAT_ZONES_COUNTER_TICKS * SPLAT_ZONES_COUNTER_TICKS == mostScore).count() > 1;
		
		if (activeColor.isEmpty())
		{
			if (session.customData != null)
			{
				Object[] dataList = (Object[]) session.customData;
				int counter = (int) dataList[0];
				Optional<InkColor> lastColor = (Optional<InkColor>) dataList[1];
				
				if (lastColor.isEmpty() || !moreThanOneOfSameColor && lastColor.get().equals(colorWithMostPoints.getKey()))
					return 0f;
				return (200 - counter) / 200f;
			}
			return 0f;
		}
		
		if (activeColor.get().equals(colorWithMostPoints.getKey()) && !moreThanOneOfSameColor)
			return 0f;
		return 1f;
	}
	private static void splatZonesOnStart(PlaySession session, ServerLevel level)
	{
		Stage stage = Stage.getStage(session.stageId);
		session.resetTeamPoints();
		session.getMarkers(true).forEach(marker -> marker.setActive(marker.getMarkerType() == StageMarkerTileEntity.MarkerType.SPLAT_ZONE));
		InkDisruptorItem.clearInk(level, stage.getMinCorner(), stage.getMaxCorner(), false);
		
		int counter = 0;
		Optional<InkColor> lastColor = Optional.empty();
		int penaltyCounter = 0;
		session.customData = new Object[] {counter, lastColor, penaltyCounter};
	}
	private static boolean splatZonesTick(PlaySession session, Level level)
	{
		int counter = 0;
		Optional<InkColor> lastColor = Optional.empty();
		int penaltyCounter = 0;
		if (session.customData != null)
		{
			Object[] dataList = (Object[]) session.customData;
			counter = (int) dataList[0];
			lastColor = (Optional<InkColor>) dataList[1];
			penaltyCounter = (int) dataList[2];
		}
		
		// stores the last color that was active for 10 seconds (for overtime)
		Optional<InkColor> activeColor = getActiveColor(session.getMarkers(true));
		if (activeColor.isEmpty())
		{
			if (counter < 200)
				counter++;
		}
		else
		{
			if (lastColor.isPresent() && !lastColor.equals(activeColor))
			{
				int finalPenaltyCounter = penaltyCounter;
				session.scores.computeIfPresent(lastColor.get(), (col, score) -> score.withPenalty(score.penalty() + (int) (finalPenaltyCounter * 0.75f)));
				penaltyCounter = -1;
			}
			
			PlaySession.TeamScore currentScore = session.scores.computeIfPresent(activeColor.get(), (col, score) ->
			{
				if (score.penalty() > 0)
					return score.withPenalty(score.penalty() - 1);
				return score.withScore(score.score() + 1);
			});
			if (currentScore == null || currentScore.score() > SPLAT_ZONES_COUNTER_TICKS * 100)
				return false;
			
			lastColor = activeColor;
			counter = 0;
			
			if (currentScore.penalty() == 0)
				penaltyCounter++;
		}
		
		session.customData = new Object[] {counter, lastColor, penaltyCounter};
		return true;
	}
	private static void splatZonesOnEnd(PlaySession session, ServerLevel world)
	{
		Stage stage = Stage.getStage(session.stageId);
		for (InkColor color : stage.getTeamIds().stream().map(teamId -> stage.getTeamColor(teamId)).toList())
		{
			//todo: NOT broadcast these messages to everyone
			world.getServer().getPlayerList().broadcastSystemMessage(
				Component.translatable("status.scan_points.score", ColorUtils.getFormatedColorName(color, false), String.valueOf(session.scores.get(color).score() / 12))
				, false);
		}
		Map.Entry<InkColor, PlaySession.TeamScore> colorWithMostPoints = session.scores.entrySet().stream().max(Comparator.comparingInt(x -> x.getValue().score())).get();
		world.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("status.scan_turf.winner", ColorUtils.getFormatedColorName(colorWithMostPoints.getKey(), false)), false);
	}
	public static Optional<InkColor> getActiveColor(List<StageMarkerTileEntity> markers)
	{
		InkColor color = null;
		for (StageMarkerTileEntity marker : markers)
		{
			Optional<InkColor> currentColor = marker.getCurrentColor();
			if (currentColor.isEmpty())
				return Optional.empty();
			
			if (color == null)
				color = currentColor.get();
			else if (!color.equals(currentColor.get()))
				return Optional.empty();
		}
		return Optional.ofNullable(color);
	}
}
