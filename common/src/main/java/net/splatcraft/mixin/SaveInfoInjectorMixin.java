package net.splatcraft.mixin;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.world.level.storage.LevelStorage;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.IOException;
import java.nio.file.Path;

@Mixin(LevelStorage.class)
public class SaveInfoInjectorMixin
{
    @Inject(method = "readLevelProperties(Ljava/nio/file/Path;Lcom/mojang/datafixers/DataFixer;)Lcom/mojang/serialization/Dynamic;", locals = LocalCapture.CAPTURE_FAILEXCEPTION, at = @At(value = "INVOKE_ASSIGN", target = "Lcom/mojang/serialization/Dynamic;update(Ljava/lang/String;Ljava/util/function/Function;)Lcom/mojang/serialization/Dynamic;", ordinal = 1))
    private static void splatcraft$obtainWorldReadData(Path path, DataFixer dataFixer, CallbackInfoReturnable<Dynamic<?>> cir, NbtCompound nbtCompound, NbtCompound nbtCompound2, int i, Dynamic dynamic)
    {
        if (!SaveInfoCapability.load(dynamic))
        {
            Path forgeCapabilitiesPath = path.getParent().resolve("data/capabilities.dat");
            try
            {
                NbtCompound capabilitiesNbt = NbtIo.readCompressed(forgeCapabilitiesPath, NbtSizeTracker.of(104857600L));
                if (capabilitiesNbt.contains("data"))
                {
                    NbtCompound data = capabilitiesNbt.getCompound("data");
                    if (data.contains("splatcraft:save_info"))
                    {
                        SaveInfoCapability.load(new Dynamic<>(NbtOps.INSTANCE, data.get("splatcraft:save_info")));
                    }
                }
            }
            catch (IOException e)
            {
                Splatcraft.LOGGER.info("Could not read save info.");
            }
        }
    }

    @Inject(method = "updateProperties", at = @At("TAIL"))
    private void splatcraft$writeSaveInfo(DynamicRegistryManager registryManager, NbtCompound levelNbt, NbtCompound playerNbt, CallbackInfo ci)
    {
        SaveInfoCapability.save(levelNbt);
    }
}
