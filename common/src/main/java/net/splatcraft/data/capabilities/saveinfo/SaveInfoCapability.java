package net.splatcraft.data.capabilities.saveinfo;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.util.InkColor;

public class SaveInfoCapability
{
	@ExpectPlatform
	public static SaveInfo get()
	{
		throw new AssertionError();
	}
	@ExpectPlatform
	public static void set(SaveInfo newData)
	{
		throw new AssertionError();
	}
	@ExpectPlatform
	public static void markUpdated()
	{
		throw new AssertionError();
	}
	public static boolean loadLegacy(NbtCompound nbt)
	{
		return SaveInfo.CODEC.decode(NbtOps.INSTANCE, nbt).ifSuccess(v ->
		{
			for (InkColor color : v.getFirst().colorScores())
			{
				ScoreboardHandler.createColorCriterion(color);
			}
			set(v.getFirst());
			markUpdated();
		}).isSuccess();
	}
}
