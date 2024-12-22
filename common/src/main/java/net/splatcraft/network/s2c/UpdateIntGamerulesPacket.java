package net.splatcraft.network.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.world.GameRules;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.util.CommonUtils;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class UpdateIntGamerulesPacket extends PlayS2CPacket
{
    public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(UpdateIntGamerulesPacket.class);
    public TreeMap<Integer, Integer> intRules;

    public UpdateIntGamerulesPacket(TreeMap<Integer, Integer> intRules)
    {
        this.intRules = intRules;
    }

    public UpdateIntGamerulesPacket(GameRules.Key<GameRules.IntRule> rule, int value)
    {
        intRules = new TreeMap<>();
        intRules.put(SplatcraftGameRules.getRuleIndex(rule), value);
    }

    public static UpdateIntGamerulesPacket decode(RegistryByteBuf buffer)
    {
        TreeMap<Integer, Integer> intRules = new TreeMap<>();
        int entrySize = buffer.readInt();

        for (int i = 0; i < entrySize; i++)
        {
            intRules.put(buffer.readInt(), buffer.readInt());
        }

        return new UpdateIntGamerulesPacket(intRules);
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }

    @Override
    public void encode(RegistryByteBuf buffer)
    {
        Set<Map.Entry<Integer, Integer>> entrySet = intRules.entrySet();

        buffer.writeInt(entrySet.size());

        for (Map.Entry<Integer, Integer> rule : entrySet)
        {
            buffer.writeInt(rule.getKey());
            buffer.writeInt(rule.getValue());
        }
    }

    @Override
    public void execute()
    {
        SplatcraftGameRules.intRules.putAll(intRules);
    }
}
