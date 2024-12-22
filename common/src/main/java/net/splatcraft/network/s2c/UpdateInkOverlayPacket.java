package net.splatcraft.network.s2c;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayCapability;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.util.CommonUtils;

public class UpdateInkOverlayPacket extends PlayS2CPacket
{
    public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(UpdateInkOverlayPacket.class);
    int entityId;
    NbtCompound nbt;
    public UpdateInkOverlayPacket(LivingEntity entity, InkOverlayInfo info)
    {
        this(entity.getId(), info.writeNBT(new NbtCompound()));
    }

    public UpdateInkOverlayPacket(int entity, NbtCompound info)
    {
        entityId = entity;
        nbt = info;
    }

    public static UpdateInkOverlayPacket decode(RegistryByteBuf buffer)
    {
        return new UpdateInkOverlayPacket(buffer.readInt(), buffer.readNbt());
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void execute()
    {
        Entity entity = MinecraftClient.getInstance().world.getEntityById(entityId);

        if (!(entity instanceof LivingEntity) || !InkOverlayCapability.hasCapability((LivingEntity) entity))
        {
            return;
        }
        InkOverlayCapability.get((LivingEntity) entity).readNBT(nbt);
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeInt(entityId);
        buffer.writeNbt(nbt);
    }
}
