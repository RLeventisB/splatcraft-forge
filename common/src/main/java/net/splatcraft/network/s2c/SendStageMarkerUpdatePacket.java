package net.splatcraft.network.s2c;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.splatcraft.tileentities.StageMarkerTileEntity;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.CommonUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

public class SendStageMarkerUpdatePacket extends PlayS2CPacket
{
	public static final StreamCodec<ByteBuf, SendStageMarkerUpdatePacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.INT.apply(CodecUtils.arrayOf(Integer[]::new)), v -> v.intDatas,
		ByteBufCodecs.FLOAT.apply(CodecUtils.arrayOf(Float[]::new)), v -> v.floatDatas,
		StageMarkerTileEntity.MarkerType.STREAM_CODEC, v -> v.markerType,
		BlockPos.STREAM_CODEC, v -> v.offset,
		GlobalPos.STREAM_CODEC, v -> v.pos,
		ByteBufCodecs.BOOL, v -> v.active,
		SendStageMarkerUpdatePacket::new
	);
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SendStageMarkerUpdatePacket.class);
	final Integer[] intDatas;
	final Float[] floatDatas;
	final StageMarkerTileEntity.MarkerType markerType;
	final BlockPos offset;
	final GlobalPos pos;
	final boolean active;
	public SendStageMarkerUpdatePacket(StageMarkerTileEntity marker)
	{
		intDatas = ArrayUtils.toObject(marker.intDatas);
		floatDatas = ArrayUtils.toObject(marker.floatDatas);
		markerType = marker.getMarkerType();
		offset = marker.getOffset();
		pos = GlobalPos.of(marker.getLevel().dimension(), marker.getBlockPos());
		active = marker.isActive();
	}
	public SendStageMarkerUpdatePacket(Integer[] intDatas, Float[] floatDatas, StageMarkerTileEntity.MarkerType markerType, BlockPos offset, GlobalPos pos, boolean active)
	{
		this.intDatas = intDatas;
		this.floatDatas = floatDatas;
		this.markerType = markerType;
		this.offset = offset;
		this.pos = pos;
		this.active = active;
	}
	public static SendStageMarkerUpdatePacket decode(RegistryFriendlyByteBuf buffer)
	{
		return STREAM_CODEC.decode(buffer);
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		STREAM_CODEC.encode(buffer, this);
	}
	@Override
	public void execute()
	{
		if (ClientUtils.getClient().level.dimension() != pos.dimension())
			return;
		
		ClientLevel level = ClientUtils.getClient().level;
		if (level != null && level.getBlockEntity(pos.pos()) instanceof StageMarkerTileEntity marker)
		{
			marker.intDatas = ArrayUtils.toPrimitive(intDatas);
			marker.floatDatas = ArrayUtils.toPrimitive(floatDatas);
			marker.setMarkerType(markerType);
			marker.setOffset(offset);
			marker.setActive(active);
			marker.notifyChange();
		}
	}
}
