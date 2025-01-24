package net.splatcraft.data.capabilities.saveinfo;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.platform.Platform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.Contract;

public class SaveInfoCapability
{
	@Environment(EnvType.CLIENT)
	public static SaveInfo clientSaveInfo;
	static
	{
		if (Platform.getEnv().equals(EnvType.CLIENT))
			clientSaveInfo = new SaveInfo(new SaveInfo.ImmutableObject2ObjectOpenHashMap<>(), new SaveInfo.ImmutableObject2ObjectOpenHashMap<>(), new SaveInfo.ImmutableObjectArrayList<>());
	}
	@Contract
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
