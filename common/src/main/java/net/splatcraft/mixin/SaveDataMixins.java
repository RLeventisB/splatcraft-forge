package net.splatcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.world.level.storage.LevelStorage;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.nio.file.Path;

public class SaveDataMixins
{
	// todo: test if this works
	@Mixin(LevelStorage.class)
	public static class LevelStorageMixin
	{
		@Inject(method = "readLevelProperties(Ljava/nio/file/Path;Lcom/mojang/datafixers/DataFixer;)Lcom/mojang/serialization/Dynamic;", at = @At(value = "INVOKE_ASSIGN", target = "Lcom/mojang/serialization/Dynamic;update(Ljava/lang/String;Ljava/util/function/Function;)Lcom/mojang/serialization/Dynamic;", ordinal = 1))
		private static void splatcraft$obtainWorldReadData(Path path, DataFixer dataFixer, CallbackInfoReturnable<Dynamic<?>> cir, @Local Dynamic dynamic)
		{
			Path forgeCapabilitiesPath = path.getParent().resolve("data/capabilities.dat");
			if (forgeCapabilitiesPath.toFile().exists())
				try
				{
					NbtCompound capabilitiesNbt = NbtIo.readCompressed(forgeCapabilitiesPath, NbtSizeTracker.of(104857600L));
					if (capabilitiesNbt.contains("data"))
					{
						NbtCompound data = capabilitiesNbt.getCompound("data");
						if (data.contains("splatcraft:save_info"))
						{
							SaveInfoCapability.loadLegacy((NbtCompound) data.get("splatcraft:save_info"));
						}
					}
				}
				catch (IOException e)
				{
					Splatcraft.LOGGER.info("Could not port forge save info.");
				}
		}
	}
}
