package net.splatcraft.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.UpdateStageListPacket;
import net.splatcraft.util.CommonUtils;

import java.util.Map;

public class RequestSetStageRulePacket extends PlayC2SPacket
{
    private static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(RequestSetStageRulePacket.class);
    final String stageId;
    final String ruleId;
    final Boolean value;

    public RequestSetStageRulePacket(String stageId, String ruleId, Boolean value)
    {
        this.stageId = stageId;
        this.ruleId = ruleId;
        this.value = value;
    }

    public static RequestSetStageRulePacket decode(RegistryByteBuf buffer)
    {
        int valueIndex = buffer.readInt();
        return new RequestSetStageRulePacket(buffer.readString(), buffer.readString(), valueIndex == 0 ? null : valueIndex == 1);
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        buffer.writeInt(value == null ? 0 : value ? 1 : 2);
        buffer.writeString(stageId);
        buffer.writeString(ruleId);
    }

    @Override
    public void execute(PlayerEntity player)
    {
        Map<String, Stage> stages = SaveInfoCapability.get().getStages();

        Stage stage = stages.get(stageId);
        stage.applySetting(ruleId.replace(Splatcraft.MODID + ".", ""), value);

        SplatcraftPacketHandler.sendToAll(new UpdateStageListPacket(stages));
    }
}
