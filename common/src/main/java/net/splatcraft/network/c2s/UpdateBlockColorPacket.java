package net.splatcraft.network.c2s;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import net.splatcraft.Splatcraft;
import net.splatcraft.tileentities.InkVatTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;

public class UpdateBlockColorPacket extends PlayC2SPacket
{
    private static final Id<? extends CustomPayload> ID = new Id<>(Splatcraft.identifierOf("update_block_color_packet"));
    BlockPos pos;
    InkColor color;
    int inkVatPointer = -1;

    public UpdateBlockColorPacket(BlockPos pos, InkColor color)
    {
        this.color = color;
        this.pos = pos;
    }

    public UpdateBlockColorPacket(BlockPos pos, InkColor color, int pointer)
    {
        this(pos, color);
        inkVatPointer = pointer;
    }

    public static UpdateBlockColorPacket decode(RegistryByteBuf buffer)
    {
        return new UpdateBlockColorPacket(new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt()), InkColor.PACKET_CODEC.decode(buffer), buffer.readInt());
    }

    @Override
    public void execute(PlayerEntity player)
    {
        BlockEntity te = player.getWorld().getBlockEntity(pos);

        if (te instanceof InkVatTileEntity te1)
        {
            te1.pointer = inkVatPointer;
        }

        ColorUtils.setInkColor(te, color);
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeInt(pos.getX());
        buffer.writeInt(pos.getY());
        buffer.writeInt(pos.getZ());
        InkColor.PACKET_CODEC.encode(buffer, color);
        buffer.writeInt(inkVatPointer);
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }
}