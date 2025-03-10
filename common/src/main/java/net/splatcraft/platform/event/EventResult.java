package net.splatcraft.platform.event;

public enum EventResult
{
	PASS,
	INTERRUPT,
	INTERRUPT_TRUE,
	INTERRUPT_FALSE;
	public static EventResult pass()
	{
		return PASS;
	}
	public static EventResult interrupt()
	{
		return INTERRUPT;
	}
	public static EventResult interruptTrue()
	{
		return INTERRUPT_TRUE;
	}
	public static EventResult interruptFalse()
	{
		return INTERRUPT_FALSE;
	}
	public static EventResult interrupt(boolean value)
	{
		return value ? INTERRUPT_TRUE : INTERRUPT_FALSE;
	}
}
