package net.splatcraft.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.splatcraft.client.gui.stagepad.AbstractStagePadScreen;
import net.splatcraft.data.Stage;
import net.splatcraft.util.ClientUtils;

import java.util.HashMap;
import java.util.Map;

public class UpdateStageListPacket extends PlayS2CPacket
{
    NbtCompound nbt;

    public UpdateStageListPacket(NbtCompound nbt)
    {
        this.nbt = nbt;
    }

    public UpdateStageListPacket(HashMap<String, Stage> stages)
    {
        NbtCompound stageNbt = new NbtCompound();

        for (Map.Entry<String, Stage> e : stages.entrySet())
            stageNbt.put(e.getKey(), e.getValue().writeData());

        nbt = stageNbt;
    }

    public static UpdateStageListPacket decode(FriendlyByteBuf buffer)
    {
        return new UpdateStageListPacket(buffer.readNbt());
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeNbt(nbt);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void execute()
    {
        ClientUtils.clientStages.clear();
        for (String key : nbt.getAllKeys())
            ClientUtils.clientStages.put(key, new Stage(nbt.getCompound(key), key));

        if (Minecraft.getInstance().screen instanceof AbstractStagePadScreen stagePadScreen)
            stagePadScreen.onStagesUpdate();
    }
}