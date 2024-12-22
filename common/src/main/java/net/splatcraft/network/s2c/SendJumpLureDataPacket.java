package net.splatcraft.network.s2c;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.handlers.JumpLureHudHandler;
import net.splatcraft.util.InkColor;

import java.util.ArrayList;
import java.util.UUID;

public class SendJumpLureDataPacket extends PlayS2CPacket
{
    public static final Id<? extends CustomPayload> ID = new Id<>(Splatcraft.identifierOf("send_jump_lure_data_packet"));
    final InkColor color;
    final boolean canJumpToSpawn;
    final BlockPos spawnPosition;
    final ArrayList<UUID> uuids;
    public SendJumpLureDataPacket(InkColor color, boolean canJumpToSpawn, ArrayList<UUID> uuids, BlockPos spawnPosition)
    {
        this.color = color;
        this.canJumpToSpawn = canJumpToSpawn;
        this.uuids = uuids;
        this.spawnPosition = spawnPosition;
    }

    public static SendJumpLureDataPacket decode(RegistryByteBuf buffer)
    {
        InkColor color = InkColor.constructOrReuse(buffer.readInt());
        boolean canJump = buffer.readBoolean();
        BlockPos spawnPosition = buffer.readBlockPos();
        int uuidCount = buffer.readInt();
        ArrayList<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < uuidCount; i++)
            uuids.add(buffer.readUuid());

        return new SendJumpLureDataPacket(color, canJump, uuids, spawnPosition);
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeInt(color.getColor());
        buffer.writeBoolean(canJumpToSpawn);
        buffer.writeBlockPos(spawnPosition);
        buffer.writeInt(uuids.size());
        for (UUID uuid : uuids)
            buffer.writeUuid(uuid);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void execute()
    {
        JumpLureHudHandler.updateTargetData(new JumpLureHudHandler.SuperJumpTargets(uuids, canJumpToSpawn, color, spawnPosition));
    }
}
