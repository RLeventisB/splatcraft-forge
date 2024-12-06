package net.splatcraft.data.capabilities.playerinfo;

import net.minecraft.core.Direction;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.splatcraft.entities.InkSquidEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerInfoCapability implements ICapabilitySerializable<NbtCompound>
{
    public static Capability<PlayerInfo> CAPABILITY = CapabilityManager.get(new CapabilityToken<>()
    {
    });
    private PlayerInfo playerInfo = null;
    private final LazyOptional<PlayerInfo> opt = LazyOptional.of(() -> playerInfo == null ? (playerInfo = new PlayerInfo()) : playerInfo);

    public static void register(RegisterCapabilitiesEvent event)
    {
        event.register(PlayerInfo.class);
    }

    @Nullable
    public static PlayerInfo get(LivingEntity entity)
    {
        PlayerInfo capability = entity.getDataTracker().get(CAPABILITY);
        return capability.resolve().orElse(null);
    }

    public static boolean hasCapability(LivingEntity entity)
    {
        return entity.getCapability(CAPABILITY).isPresent();
    }

    public static boolean isSquid(LivingEntity entity)
    {
        if (entity instanceof InkSquidEntity)
            return true;

        return hasCapability(entity) && get(entity).isSquid();
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
        return opt.orElseThrow(IllegalStateException::new).writeNBT(new NbtCompound());
    }

    @Override
    public void deserializeNBT(NbtCompound nbt)
    {
        opt.orElseThrow(IllegalStateException::new).readNBT(nbt);
    }
}
