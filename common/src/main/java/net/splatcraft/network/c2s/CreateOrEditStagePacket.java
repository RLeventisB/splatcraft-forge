package net.splatcraft.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.NotifyStageCreatePacket;
import net.splatcraft.util.CommonUtils;

import java.util.Objects;

public class CreateOrEditStagePacket extends PlayC2SPacket
{
    private static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(CreateOrEditStagePacket.class);
    final String stageId;
    final Text stageName;
    final BlockPos corner1;
    final BlockPos corner2;
    final Identifier dimension;

    public CreateOrEditStagePacket(String stageId, Text stageName, BlockPos corner1, BlockPos corner2, World dimension)
    {
        this(stageId, stageName, corner1, corner2, dimension.getDimension().effects());
    }

    public CreateOrEditStagePacket(String stageId, Text stageName, BlockPos corner1, BlockPos corner2, Identifier dimension)
    {
        this.stageId = stageId;
        this.stageName = stageName;
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.dimension = dimension;
    }

    public static CreateOrEditStagePacket decode(RegistryByteBuf buf)
    {
        return new CreateOrEditStagePacket(buf.readString(), TextCodecs.PACKET_CODEC.decode(buf), buf.readBlockPos(), buf.readBlockPos(), buf.readIdentifier());
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
        buffer.writeIdentifier(dimension);
    }

    @Override
    public void execute(PlayerEntity player)
    {
        SaveInfoCapability.get().createOrEditStage(Objects.requireNonNull(player.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, dimension))), stageId, corner1, corner2, stageName);
        SplatcraftPacketHandler.sendToPlayer(new NotifyStageCreatePacket(stageId), (ServerPlayerEntity) player);
    }
}
