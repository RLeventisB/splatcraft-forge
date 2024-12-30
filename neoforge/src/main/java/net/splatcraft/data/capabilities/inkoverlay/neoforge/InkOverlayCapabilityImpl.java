package net.splatcraft.data.capabilities.inkoverlay.neoforge;

import net.minecraft.entity.LivingEntity;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.neoforge.SplatcraftNeoForgeDataAttachments;

public class InkOverlayCapabilityImpl
{
	public static InkOverlayInfo get(LivingEntity entity)
	{
		return entity.getData(SplatcraftNeoForgeDataAttachments.INK_OVERLAY);
	}
	public static void set(LivingEntity entity, InkOverlayInfo newData)
	{
		entity.setData(SplatcraftNeoForgeDataAttachments.INK_OVERLAY, newData);
	}
	public static boolean hasCapability(LivingEntity entity)
	{
		return entity.hasData(SplatcraftNeoForgeDataAttachments.INK_OVERLAY);
	}
}
