package net.splatcraft.util.action;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Represents and end result from an {@link EntityAction}, which modifies how the action is processed:
 *
 * @param resultingAction The resulting action, which is used for {@code doSet}
 * @param doSync          Whether to synchronize the entity action to the clients.
 * @param tickAfter
 * @param doSet           Whether to run {@code EntityAction.setEntityAction} after the action is ticked, using {@code resultingAction} as the parameter.
 */
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
	/**
	 * Merges two results
	 *
	 * @param current The current end result to merge with.
	 * @param last    The last end result to merge with
	 * @return An merged end result, or {@code ActionEndResult.END_ACTION} if {@code current} and {@code last} are null.
	 */
	public static ActionEndResult merge(ActionEndResult current, ActionEndResult last)
	{
		if (last == null)
			if (current == null)
				return END_ACTION;
			else
				return current;

		return new ActionEndResult(
			last.resultingAction().flatMap(v -> current.resultingAction()),
			current.doSync() || last.doSync(),
			current.tickAfter(),
			current.doSet() || last.doSet()
		);
	}
}
