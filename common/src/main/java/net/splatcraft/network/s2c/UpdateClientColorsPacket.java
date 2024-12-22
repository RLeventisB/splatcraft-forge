package net.splatcraft.network.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkColor;

import java.util.TreeMap;
import java.util.UUID;

public class UpdateClientColorsPacket extends PlayS2CPacket
{
    public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(UpdateClientColorsPacket.class);
    final TreeMap<UUID, InkColor> colors;
    final boolean reset;

    protected UpdateClientColorsPacket(TreeMap<UUID, InkColor> colors, boolean reset)
    {
        this.colors = colors;
        this.reset = reset;
    }

    public UpdateClientColorsPacket(TreeMap<UUID, InkColor> colors)
    {
        this(colors, true);
    }

    public UpdateClientColorsPacket(UUID player, InkColor color)
    {
        colors = new TreeMap<>();
        colors.put(player, color);
        reset = false;
    }

    public static UpdateClientColorsPacket decode(RegistryByteBuf buffer)
    {
        TreeMap<UUID, InkColor> colors = new TreeMap<>();

        boolean reset = buffer.readBoolean();
        int size = buffer.readVarInt();
        for (int i = 0; i < size; i++)
        {
            colors.put(buffer.readUuid(), InkColor.constructOrReuse(buffer.readInt()));
        }
        return new UpdateClientColorsPacket(colors, reset);
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void execute()
    {
        if (reset)
        {
            ClientUtils.resetClientColors();
        }
        ClientUtils.putClientColors(colors);
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeBoolean(reset);
        buffer.writeVarInt(colors.entrySet().size());
        colors.forEach((key, value) ->
        {
            buffer.writeUuid(key);
            buffer.writeInt(value.getColor());
        });
    }
}