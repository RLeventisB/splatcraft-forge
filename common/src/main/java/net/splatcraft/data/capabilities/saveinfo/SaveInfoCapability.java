package net.splatcraft.data.capabilities.saveinfo;

import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.util.InkColor;

import java.util.ArrayList;
import java.util.HashMap;

public class SaveInfoCapability
{
    private static SaveInfo saveInfo = new SaveInfo(new HashMap<>(), new HashMap<>(), new ArrayList<>());

    public static SaveInfo get() throws NullPointerException
    {
        return saveInfo;
    }

    public static void save(NbtCompound nbt)
    {
        SaveInfo.CODEC.encode(saveInfo, NbtOps.INSTANCE, nbt);
    }

    public static boolean load(Dynamic<?> dataGetter)
    {
        return dataGetter.decode(SaveInfo.CODEC).ifSuccess(v -> {
            saveInfo = v.getFirst();
            for (InkColor color : saveInfo.colorScores())
            {
                ScoreboardHandler.createColorCriterion(color);
            }
        }).isSuccess();
    }
}
