package net.splatcraft.network.c2s;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.splatcraft.Splatcraft;
import net.splatcraft.tileentities.InkVatTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

public class UpdateBlockColorPacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = new Type<>(Splatcraft.identifierOf("update_block_color_packet"));
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
	public static UpdateBlockColorPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new UpdateBlockColorPacket(new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt()), InkColor.STREAM_CODEC.decode(buffer), buffer.readInt());
	}
	@Override
	public void execute(Player player)
	{
		BlockEntity te = player.level().getBlockEntity(pos);
		
		if (te instanceof InkVatTileEntity te1)
		{
			te1.pointer = inkVatPointer;
		}
		
		ColorUtils.withInkColor(te, color);
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeInt(pos.getX());
		buffer.writeInt(pos.getY());
		buffer.writeInt(pos.getZ());
		InkColor.STREAM_CODEC.encode(buffer, color);
		buffer.writeInt(inkVatPointer);
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}