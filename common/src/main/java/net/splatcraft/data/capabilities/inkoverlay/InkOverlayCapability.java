package net.splatcraft.data.capabilities.inkoverlay;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Contract;

public class InkOverlayCapability
{
	@Contract
	public static InkOverlayInfo get(LivingEntity entity)
	{
		throw new AssertionError();
	}
	public static void set(LivingEntity entity, InkOverlayInfo newData)
	{
		throw new AssertionError();
	}
	@Contract
	public static boolean hasCapability(LivingEntity entity)
	{
		throw new AssertionError();
	}
}
