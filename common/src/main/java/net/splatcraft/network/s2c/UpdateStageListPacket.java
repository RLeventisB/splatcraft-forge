package net.splatcraft.network.s2c;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.client.gui.stagepad.AbstractStagePadScreen;
import net.splatcraft.data.Stage;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;

import java.util.HashMap;
import java.util.Map;

public class UpdateStageListPacket extends PlayS2CPacket
{
    private static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(UpdateStageListPacket.class);
    private static final PacketCodec<RegistryByteBuf, Map<String, Stage>> STAGE_INFO_PACKET_CODEC = PacketCodecs.map(HashMap::new, PacketCodecs.STRING, Stage.PACKET_CODEC);
    Map<String, Stage> stages;

    public UpdateStageListPacket(Map<String, Stage> stages)
    {
        this.stages = stages;
    }

    public static UpdateStageListPacket decode(RegistryByteBuf buffer)
    {
        return new UpdateStageListPacket(STAGE_INFO_PACKET_CODEC.decode(buffer));
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        PacketCodecs.map(HashMap::new, PacketCodecs.STRING, Stage.PACKET_CODEC).encode(buffer, (HashMap<String, Stage>) stages);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void execute()
    {
        ClientUtils.clientStages.clear();
        ClientUtils.clientStages.putAll(stages);

        if (MinecraftClient.getInstance().currentScreen instanceof AbstractStagePadScreen stagePadScreen)
            stagePadScreen.onStagesUpdate();
    }
}