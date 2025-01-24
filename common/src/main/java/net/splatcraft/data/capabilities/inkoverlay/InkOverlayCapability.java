package net.splatcraft.data.capabilities.inkoverlay;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Contract;

public class InkOverlayCapability
{
	@Contract
	@ExpectPlatform
	public static InkOverlayInfo get(LivingEntity entity)
	{
		throw new AssertionError();
	}
	@ExpectPlatform
	public static void set(LivingEntity entity, InkOverlayInfo newData)
	{
		throw new AssertionError();
	}
	@Contract
	@ExpectPlatform
	public static boolean hasCapability(LivingEntity entity)
	{
		throw new AssertionError();
	}
}
