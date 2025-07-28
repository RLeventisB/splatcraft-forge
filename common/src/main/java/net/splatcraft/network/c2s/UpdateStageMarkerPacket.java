package net.splatcraft.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.SendStageMarkerUpdatePacket;
import net.splatcraft.platform.Services;
import net.splatcraft.tileentities.StageMarkerTileEntity;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.CommonUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

public class UpdateStageMarkerPacket extends PlayC2SPacket
{
	public static final StreamCodec<ByteBuf, UpdateStageMarkerPacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.INT.apply(CodecUtils.arrayOf(Integer[]::new)), v -> v.intDatas,
		ByteBufCodecs.FLOAT.apply(CodecUtils.arrayOf(Float[]::new)), v -> v.floatDatas,
		StageMarkerTileEntity.MarkerType.STREAM_CODEC, v -> v.markerType,
		BlockPos.STREAM_CODEC, v -> v.offset,
		GlobalPos.STREAM_CODEC, v -> v.pos,
		ByteBufCodecs.BOOL, v -> v.active,
		UpdateStageMarkerPacket::new
	);
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(UpdateStageMarkerPacket.class);
	final Integer[] intDatas;
	final Float[] floatDatas;
	final StageMarkerTileEntity.MarkerType markerType;
	final BlockPos offset;
	final GlobalPos pos;
	final boolean active;
	public UpdateStageMarkerPacket(StageMarkerTileEntity marker)
	{
		intDatas = ArrayUtils.toObject(marker.intDatas);
		floatDatas = ArrayUtils.toObject(marker.floatDatas);
		markerType = marker.getMarkerType();
		offset = marker.getOffset();
		pos = GlobalPos.of(marker.getLevel().dimension(), marker.getBlockPos());
		active = marker.isActive();
	}
	public UpdateStageMarkerPacket(Integer[] intDatas, Float[] floatDatas, StageMarkerTileEntity.MarkerType markerType, BlockPos offset, GlobalPos pos, boolean active)
	{
		this.intDatas = intDatas;
		this.floatDatas = floatDatas;
		this.markerType = markerType;
		this.offset = offset;
		this.pos = pos;
		this.active = active;
	}
	public static UpdateStageMarkerPacket decode(RegistryFriendlyByteBuf buffer)
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
	public void execute(ServerPlayer player)
	{
		if (player.canUseGameMasterBlocks())
		{
			ServerLevel level = Services.PLATFORM.getServerInstance().getLevel(pos.dimension());
			if (level != null && level.getBlockEntity(pos.pos()) instanceof StageMarkerTileEntity marker)
			{
				marker.intDatas = ArrayUtils.toPrimitive(intDatas);
				marker.floatDatas = ArrayUtils.toPrimitive(floatDatas);
				marker.setMarkerType(markerType);
				marker.setOffset(offset);
				marker.setActive(active);
				marker.notifyChange();
				
				SplatcraftPacketHandler.sendToTrackers(new SendStageMarkerUpdatePacket(intDatas,
					floatDatas,
					markerType,
					offset,
					pos,
					active), level, new ChunkPos(pos.pos()));
			}
		}
	}
}
