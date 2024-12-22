package net.splatcraft.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

import java.util.function.Consumer;
import java.util.function.Function;

public enum StageGameMode implements StringIdentifiable
{
	RECON(-1, stage -> true, session -> false, session ->
	{
	}),
	TURF_WAR(3 * 60 * 20, stage -> true, session -> false, session ->
	{
	}),
	SPLAT_ZONES(5 * 60 * 20, stage -> true, session -> false, session ->
	{
	}),
	RAINMAKER(5 * 60 * 20, stage -> true, session -> false, session ->
	{
	}),
	CLAM_BLITZ(5 * 60 * 20, stage -> true, session -> false, session ->
	{
	});
	public static final Codec<StageGameMode> CODEC = StringIdentifiable.createCodec(StageGameMode::values);
	public final int DEFAULT_TIME;
	public final Function<Stage, Boolean> playChecker;
	public final Function<PlaySession, Boolean> overtimeChecker;
	public final Consumer<PlaySession> tick;
	StageGameMode(int defaultTime, Function<Stage, Boolean> playChecker, Function<PlaySession, Boolean> overtimeChecker, Consumer<PlaySession> tick)
	{
		DEFAULT_TIME = defaultTime;
		this.playChecker = playChecker;
		this.overtimeChecker = overtimeChecker;
		this.tick = tick;
	}
	public boolean canDoOn(Stage stage)
	{
		return playChecker.apply(stage);
	}
	@Override
	public String asString()
	{
		return toString();
	}
}
