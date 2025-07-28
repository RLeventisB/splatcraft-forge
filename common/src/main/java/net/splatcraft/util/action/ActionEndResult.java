package net.splatcraft.util.action;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record ActionEndResult(
	Optional<EntityAction> resultingAction,
	boolean doSync,
	boolean tickAfter,
	boolean doSet
)
{
	public static final ActionEndResult END_ACTION = new ActionEndResult(Optional.empty(), true, false, true);
	public static ActionEndResult dontEnd(@NotNull EntityAction action)
	{
		return new ActionEndResult(Optional.of(action), false, true, true);
	}
	public static ActionEndResult dontEndWithoutSet(@NotNull EntityAction action)
	{
		return new ActionEndResult(Optional.of(action), false, true, false);
	}
	public static ActionEndResult dontEndWithSync(@NotNull EntityAction action)
	{
		return new ActionEndResult(Optional.of(action), true, true, true);
	}
}
