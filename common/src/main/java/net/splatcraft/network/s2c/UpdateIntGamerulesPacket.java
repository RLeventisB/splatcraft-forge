package net.splatcraft.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.GameRules;
import net.splatcraft.registries.SplatcraftGameRules;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class UpdateIntGamerulesPacket extends PlayS2CPacket
{
    public TreeMap<Integer, Integer> intRules;

    public UpdateIntGamerulesPacket(TreeMap<Integer, Integer> intRules)
    {
        this.intRules = intRules;
    }

    public UpdateIntGamerulesPacket(GameRules.Key<GameRules.IntegerRule> rule, int value)
    {
        this.intRules = new TreeMap<>();
        intRules.put(SplatcraftGameRules.getRuleIndex(rule), value);
    }

    public static UpdateIntGamerulesPacket decode(FriendlyByteBuf buffer)
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
    public void encode(FriendlyByteBuf buffer)
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
