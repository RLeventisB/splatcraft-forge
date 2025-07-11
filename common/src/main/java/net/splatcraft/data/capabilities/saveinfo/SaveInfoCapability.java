package net.splatcraft.data.capabilities.saveinfo;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.platform.Components;
import net.splatcraft.platform.Services;
import net.splatcraft.util.structs.InkColor;

public class SaveInfoCapability
{
	@OnlyIn(Dist.CLIENT)
	public static SaveInfo clientSaveInfo;
	static
	{
		if (Services.PLATFORM.isClientSide())
			clientSaveInfo = new SaveInfo(new SaveInfo.ImmutableObject2ObjectOpenHashMap<>(), new SaveInfo.ImmutableObject2ObjectOpenHashMap<>(), new SaveInfo.ImmutableObjectArrayList<>());
	}
	public static SaveInfo get()
	{
		if (Services.PLATFORM.isClientSide() && !Minecraft.getInstance().isLocalServer())
			return clientSaveInfo;
		
		return Components.SAVE_INFO.get(Services.PLATFORM.getServerInstance());
	}
	public static void set(SaveInfo newData)
	{
		if (Services.PLATFORM.isClientSide() && !Minecraft.getInstance().isLocalServer())
			throw new UnsupportedOperationException("SaveInfo cannot be set on the client");
		
		Components.SAVE_INFO.setOrErase(Services.PLATFORM.getServerInstance(), newData);
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
