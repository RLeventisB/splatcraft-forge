package net.splatcraft.data.capabilities.inkoverlay;

import net.minecraft.world.entity.LivingEntity;
import net.splatcraft.platform.Services;
import org.jetbrains.annotations.Contract;

public class InkOverlayCapability
{
	@Contract
	public static InkOverlayInfo get(LivingEntity entity)
	{
		return Services.PLATFORM.getInkOverlayInfo(entity);
	}
	public static void set(LivingEntity entity, InkOverlayInfo newData)
	{
		Services.PLATFORM.setInkOverlayInfo(entity, newData);
	}
	@Contract
	public static boolean hasCapability(LivingEntity entity)
	{
		return Services.PLATFORM.hasInkOverlayInfo(entity);
	}
}
