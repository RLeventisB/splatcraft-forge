package net.splatcraft.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.NotifyStageCreatePacket;
import net.splatcraft.platform.Services;
import net.splatcraft.util.CommonUtils;

public class CreateOrEditStagePacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(CreateOrEditStagePacket.class);
	private static final StreamCodec<ByteBuf, ResourceKey<Level>> WORLD_KEY_CODEC = ResourceKey.streamCodec(Registries.DIMENSION);
	final String stageId;
	final Component stageName;
	final BlockPos corner1;
	final BlockPos corner2;
	final ResourceKey<Level> worldKey;
	public CreateOrEditStagePacket(String stageId, Component stageName, BlockPos corner1, BlockPos corner2, ResourceKey<Level> worldKey)
	{
		this.stageId = stageId;
		this.stageName = stageName;
		this.corner1 = corner1;
		this.corner2 = corner2;
		this.worldKey = worldKey;
	}
	public static CreateOrEditStagePacket decode(RegistryFriendlyByteBuf buf)
	{
		return new CreateOrEditStagePacket(buf.readUtf(), ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(buf), buf.readBlockPos(), buf.readBlockPos(), WORLD_KEY_CODEC.decode(buf));
	}
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeUtf(stageId);
		ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(buffer, stageName);
		buffer.writeBlockPos(corner1);
		buffer.writeBlockPos(corner2);
		WORLD_KEY_CODEC.encode(buffer, worldKey);
	}
	@Override
	public void execute(Player player)
	{
		SaveInfoCapability.get().createOrEditStage(Services.PLATFORM.getServerInstance(), worldKey, stageId, corner1, corner2, stageName);
		SplatcraftPacketHandler.sendToPlayer(new NotifyStageCreatePacket(stageId), (ServerPlayer) player);
	}
}
