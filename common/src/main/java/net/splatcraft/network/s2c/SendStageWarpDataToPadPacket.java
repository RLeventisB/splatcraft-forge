package net.splatcraft.network.s2c;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.splatcraft.client.gui.stagepad.StageSelectionScreen;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.Stage;
import net.splatcraft.util.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class SendStageWarpDataToPadPacket extends PlayS2CPacket
{
    private static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(SendStageWarpDataToPadPacket.class);
    final List<String> validStages;
    final List<String> outOfReachStages;
    final List<String> needsUpdate;

    public SendStageWarpDataToPadPacket(List<String> validStages, List<String> outOfReachStages, List<String> needsUpdate)
    {
        this.validStages = validStages;
        this.outOfReachStages = outOfReachStages;
        this.needsUpdate = needsUpdate;
    }

    public static SendStageWarpDataToPadPacket compile(PlayerEntity player)
    {
        ArrayList<Stage> stages = Stage.getAllStages(player.getWorld());

        ArrayList<Stage> needsUpdate = new ArrayList<>();
        for (Stage stage : stages)
            if (stage.needSpawnPadUpdate())
                needsUpdate.add(stage);

        stages.removeIf(stage -> !stage.hasSpawnPads());

        ArrayList<Stage> outOfRange = new ArrayList<>();
        for (Stage stage : stages)
        {
            ArrayList<BlockPos> validPads = new ArrayList<>(stage.getSpawnPadPositions());

            validPads.removeIf(pos ->
                !(SuperJumpCommand.canSuperJumpTo(player, new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ()))));

            if (validPads.isEmpty())
                outOfRange.add(stage);
        }

        stages.removeIf(outOfRange::contains);
        stages.removeIf(needsUpdate::contains);

        return new SendStageWarpDataToPadPacket(stages.stream().map(
            stage -> stage.id).toList(),
            outOfRange.stream().map(stage -> stage.id).toList(),
            needsUpdate.stream().map(stage -> stage.id).toList());
    }

    public static SendStageWarpDataToPadPacket decode(RegistryByteBuf buffer)
    {
        int validStageCount = buffer.readInt();
        ArrayList<String> validStages = new ArrayList<>();
        for (int i = 0; i < validStageCount; i++)
            validStages.add(buffer.readString());

        int outOfReachStageCount = buffer.readInt();
        ArrayList<String> outOfReachStages = new ArrayList<>();
        for (int i = 0; i < outOfReachStageCount; i++)
            outOfReachStages.add(buffer.readString());

        int needsUpdateCount = buffer.readInt();
        ArrayList<String> needsUpdateStages = new ArrayList<>();
        for (int i = 0; i < needsUpdateCount; i++)
            needsUpdateStages.add(buffer.readString());

        return new SendStageWarpDataToPadPacket(validStages, outOfReachStages, needsUpdateStages);
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeInt(validStages.size());
        for (String stageId : validStages)
        {
            buffer.writeString(stageId);
        }
        buffer.writeInt(outOfReachStages.size());
        for (String stageId : outOfReachStages)
        {
            buffer.writeString(stageId);
        }
        buffer.writeInt(needsUpdate.size());
        for (String stageId : needsUpdate)
        {
            buffer.writeString(stageId);
        }
    }

    @Override
    public void execute()
    {
        StageSelectionScreen.updateValidSuperJumpsList(validStages, outOfReachStages, needsUpdate);
    }
}