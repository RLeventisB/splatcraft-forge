package net.splatcraft.network.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.world.GameRules;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.util.CommonUtils;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class UpdateBooleanGamerulesPacket extends PlayS2CPacket
{
    public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(UpdateBooleanGamerulesPacket.class);
    public TreeMap<Integer, Boolean> booleanRules;

    public UpdateBooleanGamerulesPacket(TreeMap<Integer, Boolean> booleanRules)
    {
        this.booleanRules = booleanRules;
    }

    public UpdateBooleanGamerulesPacket(GameRules.Key<GameRules.BooleanRule> rule, boolean value)
    {
        booleanRules = new TreeMap<>();
        booleanRules.put(SplatcraftGameRules.getRuleIndex(rule), value);
    }

    public static UpdateBooleanGamerulesPacket decode(RegistryByteBuf buffer)
    {
        TreeMap<Integer, Boolean> booleanRules = new TreeMap<>();
        int entrySize = buffer.readInt();

        for (int i = 0; i < entrySize; i++)
        {
            booleanRules.put(buffer.readInt(), buffer.readBoolean());
        }

        return new UpdateBooleanGamerulesPacket(booleanRules);
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        Set<Map.Entry<Integer, Boolean>> entrySet = booleanRules.entrySet();

        buffer.writeInt(entrySet.size());

        for (Map.Entry<Integer, Boolean> rule : entrySet)
        {
            buffer.writeInt(rule.getKey());
            buffer.writeBoolean(rule.getValue());
        }
    }

    @Override
    public void execute()
    {
        SplatcraftGameRules.booleanRules.putAll(booleanRules);
    }
}
