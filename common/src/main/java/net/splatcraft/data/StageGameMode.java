package net.splatcraft.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

import java.util.function.Consumer;
import java.util.function.Function;

public enum StageGameMode implements StringIdentifiable
{
	RECON(60 * 60, stage -> true, session -> false, session ->
	{
	}, session ->
	{
	}),
	TURF_WAR(3 * 60, stage -> true, session -> false, session ->
	{
	}, session ->
	{
	}),
	SPLAT_ZONES(5 * 60, stage -> true, session -> false, session ->
	{
	}, session ->
	{
	}),
	RAINMAKER(5 * 60, stage -> true, session -> false, session ->
	{
	}, session ->
	{
	}),
	CLAM_BLITZ(5 * 60, stage -> true, session -> false, session ->
	{
	}, session ->
	{
	});
	public static final Codec<StageGameMode> CODEC = StringIdentifiable.createCodec(StageGameMode::values);
	public final int DEFAULT_TIME_SECONDS;
	public final Function<Stage, Boolean> playChecker;
	public final Function<PlaySession, Boolean> overtimeChecker;
	public final Consumer<PlaySession> tick;
	public final Consumer<PlaySession> onEnd;
	StageGameMode(int defaultTime, Function<Stage, Boolean> playChecker, Function<PlaySession, Boolean> overtimeChecker, Consumer<PlaySession> tick, Consumer<PlaySession> onEnd)
	{
		DEFAULT_TIME_SECONDS = defaultTime;
		this.playChecker = playChecker;
		this.overtimeChecker = overtimeChecker;
		this.tick = tick;
		this.onEnd = onEnd;
	}
	public boolean canDoOn(Stage stage)
	{
		return playChecker.apply(stage);
	}
	@Override
	public String asString()
	{
		return name();
	}
}
