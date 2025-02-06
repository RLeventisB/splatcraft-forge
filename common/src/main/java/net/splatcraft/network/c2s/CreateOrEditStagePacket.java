package net.splatcraft.network.c2s;

import dev.architectury.utils.GameInstance;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.NotifyStageCreatePacket;
import net.splatcraft.util.CommonUtils;

public class CreateOrEditStagePacket extends PlayC2SPacket
{
	public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(CreateOrEditStagePacket.class);
	private static final PacketCodec<ByteBuf, RegistryKey<World>> WORLD_KEY_CODEC = RegistryKey.createPacketCodec(RegistryKeys.WORLD);
	final String stageId;
	final Text stageName;
	final BlockPos corner1;
	final BlockPos corner2;
	final RegistryKey<World> worldKey;
	public CreateOrEditStagePacket(String stageId, Text stageName, BlockPos corner1, BlockPos corner2, RegistryKey<World> worldKey)
	{
		this.stageId = stageId;
		this.stageName = stageName;
		this.corner1 = corner1;
		this.corner2 = corner2;
		this.worldKey = worldKey;
	}
	public static CreateOrEditStagePacket decode(RegistryByteBuf buf)
	{
		return new CreateOrEditStagePacket(buf.readString(), TextCodecs.PACKET_CODEC.decode(buf), buf.readBlockPos(), buf.readBlockPos(), WORLD_KEY_CODEC.decode(buf));
	}
	@Override
	public Id<? extends CustomPayload> getId()
	{
		return ID;
	}
	@Override
	public void encode(RegistryByteBuf buffer)
	{
		buffer.writeString(stageId);
		TextCodecs.PACKET_CODEC.encode(buffer, stageName);
		buffer.writeBlockPos(corner1);
		buffer.writeBlockPos(corner2);
		WORLD_KEY_CODEC.encode(buffer, worldKey);
	}
	@Override
	public void execute(PlayerEntity player)
	{
		SaveInfoCapability.get().createOrEditStage(GameInstance.getServer(), worldKey, stageId, corner1, corner2, stageName);
		SplatcraftPacketHandler.sendToPlayer(new NotifyStageCreatePacket(stageId), (ServerPlayerEntity) player);
	}
}
