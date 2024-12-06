package net.splatcraft.data.capabilities.saveinfo;

import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SaveInfoCapability implements ICapabilityProvider, INBTSerializable<NbtCompound>
{
    public static Capability<SaveInfo> CAPABILITY = CapabilityManager.get(new CapabilityToken<>()
    {
    });
    private SaveInfo saveInfo = null;
    private final LazyOptional<SaveInfo> opt = LazyOptional.of(() ->
        saveInfo == null ? (saveInfo = new SaveInfo()) : saveInfo);

    public static SaveInfo get(MinecraftServer server) throws NullPointerException
    {
        return server.getLevel(Level.OVERWORLD).getCapability(CAPABILITY).orElseThrow(() -> new NullPointerException("Couldn't find WorldData capability!"));
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
    {
        return cap == CAPABILITY ? opt.cast() : LazyOptional.empty();
    }

    @Override
    public NbtCompound serializeNBT()
    {
        return opt.orElse(null).writeNBT(new NbtCompound());
    }

    @Override
    public void deserializeNBT(NbtCompound nbt)
    {
        opt.orElse(null).readNBT(nbt);
    }
}
