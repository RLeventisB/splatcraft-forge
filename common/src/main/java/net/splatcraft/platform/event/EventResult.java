package net.splatcraft.platform.event;

import net.minecraft.world.InteractionResult;

import java.util.Optional;

public enum EventResult
{
	PASS(null, false),
	INTERRUPT(null, true),
	INTERRUPT_TRUE(true, true),
	INTERRUPT_FALSE(false, true);
	public final Optional<Boolean> value;
	public final Boolean interrupts;
	EventResult(Boolean value, Boolean interrupts)
	{
		this.value = Optional.ofNullable(value);
		this.interrupts = interrupts;
	}
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
	public InteractionResult convertToInteractionResult()
	{
		return switch (this)
		{
			case PASS -> InteractionResult.PASS;
			case INTERRUPT, INTERRUPT_FALSE -> InteractionResult.FAIL;
			case INTERRUPT_TRUE -> InteractionResult.SUCCESS;
		};
	}
	public boolean interruptsOrFalse()
	{
		return this == INTERRUPT || this == INTERRUPT_FALSE;
	}
}
