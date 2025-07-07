package net.splatcraft.data.capabilities.saveinfo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.platform.Services;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.Contract;

public class SaveInfoCapability
{
	@OnlyIn(Dist.CLIENT)
	public static SaveInfo clientSaveInfo;
	static
	{
		if (Services.PLATFORM.isClientSide())
			clientSaveInfo = new SaveInfo(new SaveInfo.ImmutableObject2ObjectOpenHashMap<>(), new SaveInfo.ImmutableObject2ObjectOpenHashMap<>(), new SaveInfo.ImmutableObjectArrayList<>());
	}
	// todo: create an "register" method so get doesn't automatically instantiates a saveinfo
	@Contract
	public static SaveInfo get()
	{
		return Services.PLATFORM.getSaveInfo();
	}
	public static void set(SaveInfo newData)
	{
		Services.PLATFORM.setSaveInfo(newData);
	}
	public static boolean loadLegacy(CompoundTag nbt)
	{
		return SaveInfo.CODEC.decode(NbtOps.INSTANCE, nbt).ifSuccess(v ->
		{
			for (InkColor color : v.getFirst().colorScores())
			{
				ScoreboardHandler.createColorCriterion(color);
			}
			set(v.getFirst());
		}).isSuccess();
	}
}
