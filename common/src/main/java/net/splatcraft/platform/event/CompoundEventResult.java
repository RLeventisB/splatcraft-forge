package net.splatcraft.platform.event;

// yeah this is pretty much architectury's event result from here!!!
// https://github.com/architectury/architectury-api/blob/1.21.4/common/src/main/java/dev/architectury/event/CompoundEventResult.java
// so todo: maybe use architectury again (if they fix their hotswap compatibility) or use some other method that doesnt copy other code
public record CompoundEventResult<T>(EventResult result, T value)
{
	public static CompoundEventResult<?> PASS = new CompoundEventResult<>(EventResult.PASS, null);
	public static <T> CompoundEventResult<T> pass()
	{
		return (CompoundEventResult<T>) PASS;
	}
	public static <T> CompoundEventResult<T> interruptTrue(T value)
	{
		return new CompoundEventResult<>(EventResult.INTERRUPT_TRUE, value);
	}
	public static <T> CompoundEventResult<T> interruptFalse(T value)
	{
		return new CompoundEventResult<>(EventResult.INTERRUPT_FALSE, value);
	}
	public static <T> CompoundEventResult<T> interrupt(T value)
	{
		return new CompoundEventResult<>(EventResult.INTERRUPT, value);
	}
	public static <T> CompoundEventResult<T> interrupt(T value, boolean interruptType)
	{
		return new CompoundEventResult<>(EventResult.interrupt(interruptType), value);
	}
}
